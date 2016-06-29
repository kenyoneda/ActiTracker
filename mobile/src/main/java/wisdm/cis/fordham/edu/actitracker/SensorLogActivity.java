package wisdm.cis.fordham.edu.actitracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Activity to prepare user for data collection.
 * Two modes:
 * 1. Timed mode - user enters time in minutes to log data.
 * 2. Manual mode - user can manually start/stop data logging.
 *
 * If watch (Android Wear) is connected, above info is sent to simultaneously log data.
 */
public class SensorLogActivity extends AppCompatActivity {

    private static final String TAG = "SensorLogActivity";
    private static final String USERNAME = "USERNAME";
    private static final String ACTIVITY_NAME = "ACTIVITY_NAME";
    private static final String TIMED_MODE = "TIMED_MODE";
    private static final String SAMPLING_RATE = "SAMPLING_RATE";
    private static final String MINUTES = "MINUTES";
    private static final String TIMESTAMP = "TIMESTAMP";
    private static final String WATCH_SENSOR_CODES = "WATCH_SENSOR_CODES";
    private static final String STOP = "/stop";
    private static final String SETTINGS = "/settings";
    private static final String PREF_SAMPLING_RATE = "pref_samplingRate";
    private static final String PREF_SENSOR_LIST_WEAR = "pref_sensorListWear";
    
    private Button mLogStartButton;
    private Button mLogStopButton;
    private Button mFileListButton;
    private TextView mTimer;
    private Chronometer mChronometer;
    private EditText mLogTime;
    private ListView mFileList;
    private String username;
    private String activityName;
    private boolean timedMode;      // True if timed mode. False if manual mode.
    private int minutes;            // Data log time in minutes.
    private int samplingRate;
    private long logDelay = 5000L;
    private ArrayList<Integer> watchSensorCodes;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_log);
        initializeGoogleApiClient();

        mLogStartButton = (Button)findViewById(R.id.log_start_button);
        mLogStopButton = (Button)findViewById(R.id.log_stop_button);
        mFileListButton = (Button)findViewById(R.id.file_list_button);
        mTimer = (TextView)findViewById(R.id.log_timer);
        mChronometer = (Chronometer)findViewById(R.id.log_stopwatch);
        mLogTime = (EditText)findViewById(R.id.log_time_minutes);
        mFileList = (ListView)findViewById(R.id.file_list);

        Intent i = getIntent();
        username = i.getStringExtra(USERNAME);
        activityName = i.getStringExtra(ACTIVITY_NAME);
        samplingRate = getSamplingRate();
        watchSensorCodes = getWatchSensorCodes();

        setOnClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    /**
     * Start button sends settings and user/activity info to phone logging service/watch and becomes
     * disabled once logging starts.
     * If in manual mode, stop button sends message to watch to stop logging and stops phone
     * logging service.
     */
    private void setOnClickListeners() {
        mLogStartButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // prompt user if no time entered
                        if (timedMode && mLogTime.getText().toString().isEmpty()) {
                            Toast.makeText(getApplicationContext(), R.string.invalid_time,
                                    Toast.LENGTH_SHORT).show();
                        }

                        else {
                            Intent i = new Intent(SensorLogActivity.this, PhoneSensorLogService.class);
                            mLogStartButton.setEnabled(false);
                            i.putExtra(USERNAME, username);
                            i.putExtra(ACTIVITY_NAME, activityName);
                            i.putExtra(TIMED_MODE, timedMode);
                            i.putExtra(SAMPLING_RATE, samplingRate);

                            // Send minutes if timed mode
                            if (timedMode) {
                                minutes = Integer.parseInt(mLogTime.getText().toString());
                                i.putExtra(MINUTES, minutes);
                            }

                            // Send settings and start logging on watch
                            sendSettingsAndStart();

                            // Display timer/stopwatch and start service on phone
                            displayTimer();
                            startService(i);
                        }
                    }
                });

        mLogStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogStopButton.setEnabled(false);
                mChronometer.stop();
                sendMessage(STOP);

                Intent i = new Intent(SensorLogActivity.this, PhoneSensorLogService.class);
                stopService(i);
            }
        });

        mFileListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileList();
            }
        });
    }

    /**
     * Show files on disk given username and activity (if they exist).
     * Useful for multi-day logging.
     */
    private void showFileList() {
        File directory = SensorFileSaver.getDirectory(this, username, activityName);
        File[] files = directory.listFiles();
        ArrayList<String> fileList = new ArrayList<String>();
        for (File file : files) {
            fileList.add(file.getName());
            String size = android.text.format.Formatter.formatShortFileSize(this, file.length());
            fileList.add(getResources().getString(R.string.file_size) + size);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.file_list, fileList);
        mFileList.setAdapter(adapter);
    }

    /**
     * Needed to connect to watch (Android Wear).
     */
    private void initializeGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "onConnected method called");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "Connection to wearable suspended. Code: " + i);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d(TAG, "onConnection failed: " + connectionResult);
                    }
                })
                .addApi(Wearable.API)
                .build();
    }

    /**
     *  Connect to watch and send a message with a given message and setting.
     */
    private void sendMessage(final String message) {
        if (mGoogleApiClient.isConnected()){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    NodeApi.GetConnectedNodesResult nodes =
                            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

                    for (Node node : nodes.getNodes()) {

                        MessageApi.SendMessageResult result =
                                Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(),
                                        message, message.getBytes()).await();

                        Log.d(TAG, "Sent to node: " + node.getId() +
                                " with display name: " + node.getDisplayName());

                        if (!result.getStatus().isSuccess()) {
                            Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                        }
                        else {
                            Log.d(TAG, "Message Successfully sent.");
                        }
                    }
                }
            }).start();
        }
        else {
            Log.e(TAG, "Wearable not connected");
        }
    }

    /**
     * Sends all necessary parameters to watch. Once they are sent, logging service is started
     * by WearListenerService.
     */
    private void sendSettingsAndStart() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(SETTINGS);
                putDataMapRequest.getDataMap().putInt(MINUTES, minutes);
                putDataMapRequest.getDataMap().putInt(SAMPLING_RATE, getSamplingRate());
                putDataMapRequest.getDataMap().putBoolean(TIMED_MODE, timedMode);
                putDataMapRequest.getDataMap().putString(USERNAME, username);
                putDataMapRequest.getDataMap().putString(ACTIVITY_NAME, activityName);
                putDataMapRequest.getDataMap().putIntegerArrayList(WATCH_SENSOR_CODES, watchSensorCodes);
                putDataMapRequest.getDataMap().putLong(TIMESTAMP, System.currentTimeMillis());

                PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
                PendingResult<DataApi.DataItemResult> pendingResult =
                        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest);
            }
        }).start();
    }

    // Get sampling rate from preferences (settings page).
    private int getSamplingRate() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return Integer.parseInt(sharedPreferences.getString(PREF_SAMPLING_RATE, "0"));
    }

    // Get watch sensors from preferences (settings page).
    private ArrayList<Integer> getWatchSensorCodes() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> defaultSensors = new HashSet<String>(Arrays.asList("1", "4"));
        List<String> stringList = new ArrayList<String>(sharedPreferences.getStringSet(PREF_SENSOR_LIST_WEAR, defaultSensors));
        ArrayList<Integer> watchSensorCodes = new ArrayList<Integer>();
        for (String s : stringList) {
            watchSensorCodes.add(Integer.valueOf(s));
        }
        return watchSensorCodes;
    }

    private void displayTimer() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (timedMode) {
                    mChronometer.setVisibility(View.GONE);
                    mTimer.setVisibility(View.VISIBLE);
                    final long milliseconds = minutes * 60 * 1000;
                    long ms = 1000L;
                    new CountDownTimer(milliseconds, ms) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            String time = String.format("%02d:%02d",
                                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60);
                            mTimer.setText(time);
                        }

                        @Override
                        public void onFinish() {
                            mTimer.setText(getResources().getString(R.string.log_timer_end));
                        }
                    }.start();
                }
                else {
                    mTimer.setVisibility(View.GONE);
                    mChronometer.setVisibility(View.VISIBLE);
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                }
            }
        }, logDelay);

    }
    /**
     * Set UI to corresponding timer mode.
     * Timer: EditText to set time and start button.
     * Manual: Start/stop button.
     *
     * @param view
     */
    public void onRadioButtonClicked(View view) {
        switch (view.getId()) {
            case R.id.radio_timed:
                timedMode = true;
                mLogStartButton.setVisibility(View.VISIBLE);
                mLogStopButton.setVisibility(View.INVISIBLE);
                mLogTime.setVisibility(View.VISIBLE);
                break;

            case R.id.radio_manual:
                timedMode = false;
                mLogStartButton.setVisibility(View.VISIBLE);
                mLogStopButton.setVisibility(View.VISIBLE);
                mLogTime.setVisibility(View.INVISIBLE);
                break;
        }
    }
}