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
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Service that logs data for user determined time.
 * Writes data to application directory.
 */
public class PhoneSensorLogService extends Service implements SensorEventListener {

    private static final String TAG = "PhoneSensorLogService";
    private static final String MINUTES = "MINUTES";
    private static final String SAMPLING_RATE = "SAMPLING_RATE";
    private static final String TIMED_MODE = "TIMED_MODE";
    private static final String USERNAME = "USERNAME";
    private static final String ACTIVITY_NAME = "ACTIVITY_NAME";
    private static final String PHONE_ACCEL = "phone_accel";
    private static final String PHONE_GYRO = "phone_gyro";

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
    private ArrayList<ThreeTupleRecord> mPhoneAccelerometerRecords = new ArrayList<ThreeTupleRecord>();
    private ArrayList<ThreeTupleRecord> mPhoneGyroscopeRecords = new ArrayList<ThreeTupleRecord>();
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private String username;
    private String activityName;
    private boolean timedMode;
    private long logDelay = 5000;  // TODO: Incorporate into settings.

    public PhoneSensorLogService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        username = intent.getStringExtra(USERNAME);
        activityName = intent.getStringExtra(ACTIVITY_NAME);
        int minutes = intent.getIntExtra(MINUTES, 0);
        int samplingRate = intent.getIntExtra(SAMPLING_RATE, 0);
        timedMode = intent.getBooleanExtra(TIMED_MODE, true);

        Log.d(TAG, "Service Started. Username: " + username + ", Activity: " + activityName +
                ", Sampling Rate: " + samplingRate + ", Minutes: " + minutes);
        registerSensorListeners(minutes, samplingRate);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Write files to disk if manual mode.
        if (!timedMode) {
            mSensorManager.unregisterListener(PhoneSensorLogService.this, mAccelerometer);
            mSensorManager.unregisterListener(PhoneSensorLogService.this, mGyroscope);
            Log.d(TAG, "End: " + System.currentTimeMillis());
            writeFiles();
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
    private void registerSensorListeners(final int minutes, final int samplingRate){
        // Get the accelerometer and gyroscope if available on device
        getSensors();
        acquireWakeLock();

        // Register sensor listener after delay
        Log.d(TAG, "Before start: " + System.currentTimeMillis());
        exec.schedule(new Runnable() {
            @Override
            public void run() {
                mSensorManager.registerListener(PhoneSensorLogService.this, mAccelerometer, samplingRate);
                mSensorManager.registerListener(PhoneSensorLogService.this, mGyroscope, samplingRate);
                Log.d(TAG, "Start: " + System.currentTimeMillis());
                if (timedMode) {
                    scheduleLogStop(minutes);
                }
            }
        }, logDelay, TimeUnit.MILLISECONDS);
    }

    /**
     * Unregister sensor listener after specified minutes.
     * @param minutes
     */
    private void scheduleLogStop(int minutes) {
        exec.schedule(new Runnable() {
            @Override
            public void run() {
                mSensorManager.unregisterListener(PhoneSensorLogService.this, mAccelerometer);
                mSensorManager.unregisterListener(PhoneSensorLogService.this, mGyroscope);
                Log.d(TAG, "End: " + System.currentTimeMillis());

                // Notify user that logging is done
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(1000);

                writeFiles();
                Log.d(TAG, "Stopping service.");
                stopSelf();
            }
        }, minutes, TimeUnit.MINUTES);
    }

    /**
     * Get the accelerometer and gyroscope if available on device.
     */
    private void getSensors() {
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
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
     * Writes files to internal storage.
     * Format: /User/Activity/device_sensor_username_activityName_date_time.txt
     */
    private void writeFiles() {
        File directory = SensorFileSaver.getDirectory(this, username, activityName);
        File phoneAccelFile = SensorFileSaver.createFile(directory, username, activityName, PHONE_ACCEL);
        File phoneGyroFile = SensorFileSaver.createFile(directory, username, activityName, PHONE_GYRO);
        SensorFileSaver.writeFile(phoneAccelFile, mPhoneAccelerometerRecords);
        SensorFileSaver.writeFile(phoneGyroFile, mPhoneGyroscopeRecords);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                mPhoneAccelerometerRecords.add(
                        new ThreeTupleRecord(
                                event.timestamp, event.values[0], event.values[1], event.values[2]));
                break;
            case Sensor.TYPE_GYROSCOPE:
                mPhoneGyroscopeRecords.add(
                        new ThreeTupleRecord(
                                event.timestamp, event.values[0], event.values[1], event.values[2]));
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
