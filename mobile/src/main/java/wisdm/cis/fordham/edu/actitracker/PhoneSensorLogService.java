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
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PhoneSensorLogService extends Service implements SensorEventListener {
    private static final String TAG = "PhoneSensorService";

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private List<ThreeTupleRecord> mPhoneAccelerometerRecords = new ArrayList<ThreeTupleRecord>();
    private List<ThreeTupleRecord> mPhoneGyroscopeRecords = new ArrayList<ThreeTupleRecord>();
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private int SAMPLE_RATE;
    private String username;
    private String activityName;
    private int minutes;
    private ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
    private boolean timedMode;

    public PhoneSensorLogService() {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                mPhoneAccelerometerRecords.add(
                        new ThreeTupleRecord(
                                event.timestamp, event.values[0], event.values[1], event.values[2]));
                Log.d(TAG, "Accel: " + event.timestamp);
                break;
            case Sensor.TYPE_GYROSCOPE:
                mPhoneGyroscopeRecords.add(
                        new ThreeTupleRecord(
                                event.timestamp, event.values[0], event.values[1], event.values[2]));
                Log.d(TAG, "Gyro: " + event.timestamp);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        username = intent.getStringExtra("USERNAME");
        activityName = intent.getStringExtra("ACTIVITY_NAME");
        minutes = intent.getIntExtra("MINUTES", 0);
        timedMode = intent.getBooleanExtra("TIMEDMODE", true);
        SAMPLE_RATE = getSamplingRate();

        Log.d(TAG, "serviceSTARTED. username: " + username + ", activity: " + activityName +
                ", sampling rate: " + SAMPLE_RATE + ", minutes: " + minutes);
        registerSensorListeners();

        return START_NOT_STICKY;
    }

    private int getSamplingRate() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return Integer.parseInt(sharedPreferences.getString("pref_samplingRate", "0"));
    }
    /**
     * This method acquires and registers the sensors and the wake lock for sensor sampling.
     * Listeners are registered after a delay to allow user to set up phone/watch position if needed.
     * Listeners are unregistered after user specified time if in timed mode. Otherwise, waits for
     * user to stop service manually via stop button.
     */
    private void registerSensorListeners(){

        // Get the accelerometer and gyroscope sensors if they exist
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
            mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        // Acquire the wake lock to sample with the screen off
        mPowerManager = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);

        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.acquire();

        // Register sensor listener after delay to compensate for user putting phone in pocket

        long delay = 5;

        Log.d(TAG, "Before start: " + System.currentTimeMillis());

        exec.schedule(new Runnable() {
            @Override
            public void run() {
                mSensorManager.registerListener(PhoneSensorLogService.this, mAccelerometer, SAMPLE_RATE);
                mSensorManager.registerListener(PhoneSensorLogService.this, mGyroscope, SAMPLE_RATE);
                Log.d(TAG, "Start: " + System.currentTimeMillis());
                if (timedMode) {
                    scheduleLogStop();
                }
            }
        }, delay, TimeUnit.SECONDS);
    }

    private void scheduleLogStop() {
        // Unregister sensor listener after specified minutes
        exec.schedule(new Runnable() {
            @Override
            public void run() {
                mSensorManager.unregisterListener(PhoneSensorLogService.this, mAccelerometer);
                mSensorManager.unregisterListener(PhoneSensorLogService.this, mGyroscope);
                Log.d(TAG, "End: " + System.currentTimeMillis());
                writeFiles();
                Log.d(TAG, "About to stop service...");
                stopSelf();
            }
        }, minutes, TimeUnit.MINUTES);
    }

    private void writeFiles() {
        Log.d(TAG, "Writing files. Size of Accel: " + mPhoneAccelerometerRecords.size() +
            "Size of Gyro: " + mPhoneGyroscopeRecords.size());

        File accelFile = new File(getFilesDir(), "accel.txt");
        File gyroFile = new File(getFilesDir(), "gyro.txt");

        try {
            BufferedWriter accelBufferedWriter = new BufferedWriter(new FileWriter(accelFile));
            BufferedWriter gyroBufferedWriter = new BufferedWriter(new FileWriter(gyroFile));

            for (ThreeTupleRecord record : mPhoneAccelerometerRecords) {
                accelBufferedWriter.write(record.toString());
                accelBufferedWriter.newLine();
            }

            for (ThreeTupleRecord record : mPhoneGyroscopeRecords) {
                gyroBufferedWriter.write(record.toString());
                gyroBufferedWriter.newLine();
            }

            accelBufferedWriter.close();
            gyroBufferedWriter.close();
        }

        catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error writing files!");
        }
    }
    @Override
    public void onCreate() {
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void onDestroy() {
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
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
