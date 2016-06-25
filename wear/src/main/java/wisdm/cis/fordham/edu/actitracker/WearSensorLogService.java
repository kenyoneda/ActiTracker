package wisdm.cis.fordham.edu.actitracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.apache.commons.lang3.SerializationUtils;

import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Service that logs data for user determined time.
 * Sends data back to phone for writing.
 */
public class WearSensorLogService extends Service implements SensorEventListener {

    private static final String TAG = "WearSensorLogService";
    private static final String ACCEL_ASSET = "ACCEL_ASSET";
    private static final String GYRO_ASSET = "GYRO_ASSET";
    private static final String USERNAME = "USERNAME";
    private static final String ACTIVITY_NAME = "ACTIVITY_NAME";
    private static final String MINUTES = "MINUTES";
    private static final String SAMPLING_RATE = "SAMPLING_RATE";
    private static final String TIMED_MODE = "TIMED_MODE";
    private static final String DELAY = "DELAY";
    private static final String STOP_STOPWATCH = "stop_stopwatch";
    private static final String DATA = "/data";

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
    private ArrayList<Integer> mSensorList = new ArrayList<Integer>();
    private ArrayList<ArrayList<SensorRecord>> mRecords = new ArrayList<ArrayList<SensorRecord>>();
    private ArrayList<SensorRecord> mWatchAccelerometerRecords = new ArrayList<SensorRecord>();
    private ArrayList<SensorRecord> mWatchGyroscopeRecords = new ArrayList<SensorRecord>();
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private GoogleApiClient mGoogleApiClient;
    private String username;
    private String activityName;
    private boolean timedMode;
    private int minutes;
    private long logDelay = 5000L;
    private long phoneToWatchDelay;

    public WearSensorLogService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        minutes = intent.getIntExtra(MINUTES, 0);
        int samplingRate = intent.getIntExtra(SAMPLING_RATE, 0);
        phoneToWatchDelay = intent.getLongExtra(DELAY, 0);
        timedMode = intent.getBooleanExtra(TIMED_MODE, true);
        username = intent.getStringExtra(USERNAME);
        activityName = intent.getStringExtra(ACTIVITY_NAME);

        Log.d(TAG, "Service Started. Username: " + username + ", Activity: " + activityName +
                ", Sampling Rate: " + samplingRate + ", Minutes: " + minutes);
        registerListeners(minutes, samplingRate);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!timedMode) {
            mSensorManager.unregisterListener(WearSensorLogService.this, mAccelerometer);
            mSensorManager.unregisterListener(WearSensorLogService.this, mGyroscope);
            Log.d(TAG, "End: " + System.currentTimeMillis());
            stopStopwatch();
            sendData();
        }
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    /**
     * This method acquires and registers the sensors and the wake lock for sensor sampling.
     * Listeners are registered after a delay to allow user to set up phone/watch position if needed.
     * Listeners are unregistered after user specified time if in timed mode. Otherwise, waits for
     * user to stop service manually via stop button.
     */
    private void registerListeners(final int minutes, final int samplingRate) {
        getSensors();
        acquireWakeLock();

        // Register sensor listener after delay. Compensate for comm delay between phone and watch.
        Log.d(TAG, "Before start: " + System.currentTimeMillis());
        logDelay -= System.currentTimeMillis() - phoneToWatchDelay;
        if (logDelay < 0L) {
            logDelay = 0L;
        }

        exec.schedule(new Runnable() {
            @Override
            public void run() {
                displayTimer();
                mSensorManager.registerListener(WearSensorLogService.this, mAccelerometer, samplingRate);
                mSensorManager.registerListener(WearSensorLogService.this, mGyroscope, samplingRate);
                Log.d(TAG, "Start: " + System.currentTimeMillis());
                if (timedMode) {
                    scheduleLogStop(minutes);
                }
            }
        }, logDelay, TimeUnit.MILLISECONDS);
    }

    /**
     * Unregister sensor listener after specified minutes
     * @param minutes
     */
    private void scheduleLogStop(int minutes) {
        exec.schedule(new Runnable() {
            @Override
            public void run() {
                mSensorManager.unregisterListener(WearSensorLogService.this, mAccelerometer);
                mSensorManager.unregisterListener(WearSensorLogService.this, mGyroscope);
                Log.d(TAG, "End: " + System.currentTimeMillis());

                // Notify user that logging is done
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(1000);

                sendData();
                Log.d(TAG, "About to stop service...");
                stopSelf();
            }
        }, minutes, TimeUnit.MINUTES);
    }

    /**
     * Get the accelerometer and gyroscope if available on device
     */
    private void getSensors() {

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
            mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
    }

    /**
     * Acquire wake lock to sample with the screen off.
     */
    private void acquireWakeLock() {
        mPowerManager = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.setReferenceCounted(false);
        mWakeLock.acquire();
    }

    /**
     * comment
     */
    private void sendData() {
        Log.d(TAG, "Sending data from watch to phone");
        Asset accelAsset = Asset.createFromBytes(SerializationUtils.serialize(mWatchAccelerometerRecords));
        Asset gyroAsset = Asset.createFromBytes(SerializationUtils.serialize(mWatchGyroscopeRecords));

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        PutDataMapRequest dataMap = PutDataMapRequest.create(DATA);
        dataMap.getDataMap().putAsset(ACCEL_ASSET, accelAsset);
        dataMap.getDataMap().putAsset(GYRO_ASSET, gyroAsset);
        dataMap.getDataMap().putString(USERNAME, username);
        dataMap.getDataMap().putString(ACTIVITY_NAME, activityName);
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);
    }

    /**
     * Start UI on watch to display timer (if in timed mode) or stopwatch (if in manual mode)
     */
    private void displayTimer() {
        if (timedMode) {
            Intent i = new Intent(WearSensorLogService.this, LogTimerActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(MINUTES, minutes);
            startActivity(i);
        }
        else {
            Intent i = new Intent(WearSensorLogService.this, LogStopwatchActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(MINUTES, minutes);
            startActivity(i);
        }
    }

    private void stopStopwatch() {
        LocalBroadcastManager localBroadcastManager =
                LocalBroadcastManager.getInstance(WearSensorLogService.this);
        localBroadcastManager.sendBroadcast(new Intent(STOP_STOPWATCH));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int index = mSensorList.indexOf(event.sensor.getType());
        mRecords.get(index).add(new SensorRecord(event.timestamp, event.values));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}