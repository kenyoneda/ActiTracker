package wisdm.cis.fordham.edu.actitracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
    private static final String PREF_SENSOR_LIST_PHONE = "pref_sensorListPhone";

    private SensorManager mSensorManager;
    private ArrayList<Integer> mSensorCodes = new ArrayList<Integer>();
    private ArrayList<Sensor> mSensors = new ArrayList<Sensor>();
    private ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
    private ArrayList<ArrayList<SensorRecord>> mRecords = new ArrayList<ArrayList<SensorRecord>>();
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

        getSensorList();

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
            for (Sensor sensor : mSensors) {
                mSensorManager.unregisterListener(PhoneSensorLogService.this, sensor);
            }
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
                for (Sensor sensor : mSensors) {
                    mSensorManager.registerListener(PhoneSensorLogService.this, sensor, samplingRate);
                }
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
                for (Sensor sensor : mSensors) {
                    mSensorManager.unregisterListener(PhoneSensorLogService.this, sensor);
                }
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
     * Get selected sensor codes from preference page
     */
    private void getSensorList() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Default sensors (accel/gyro) if none are selected. TODO: Handle more gracefully.
        Set<String> defaultSensors = new HashSet<String>(Arrays.asList("1", "4"));

        List<String> stringList = new ArrayList<String>(sharedPreferences.getStringSet(PREF_SENSOR_LIST_PHONE, defaultSensors));

        // Weird bug where default sensors are not referenced
        if (stringList.isEmpty()) {
            stringList = new ArrayList<String>(defaultSensors);
        }

        for (String s : stringList) {
            if (StringUtils.isNumeric(s)) {
                mSensorCodes.add(Integer.valueOf(s));
                Log.d(TAG, "code added: " + s);
            }
        }
    }

    /**
     * Get sensors and create an equal number of ArrayList of SensorRecords
     */
    private void getSensors() {
        for (Integer i : mSensorCodes) {
            mSensors.add(mSensorManager.getDefaultSensor(i));
            mRecords.add(new ArrayList<SensorRecord>());
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
        for (int i = 0; i < mRecords.size(); i++) {
            File file = SensorFileSaver.createFile(this, directory, username, activityName,
                    mSensors.get(i).getName().trim().toLowerCase().replace(" ", "_"));
            SensorFileSaver.writeFile(file, mRecords.get(i));
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int index = mSensorCodes.indexOf(event.sensor.getType());
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
