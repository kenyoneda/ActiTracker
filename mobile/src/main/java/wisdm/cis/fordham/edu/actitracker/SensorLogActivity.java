package wisdm.cis.fordham.edu.actitracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

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

    private Button mLogStartButton;
    private Button mLogStopButton;
    private EditText mLogTime;
    private String username;
    private String activityName;
    private boolean timedMode;      // True if timed mode. False if manual mode.
    private int minutes;            // Data log time in minutes.
    private int defaultMinutes = 1; // Default minutes if no time entered.
    private int samplingRate;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_log);
        initializeGoogleApiClient();

        mLogStartButton = (Button)findViewById(R.id.log_start_button);
        mLogStopButton = (Button)findViewById(R.id.log_stop_button);
        mLogTime = (EditText)findViewById(R.id.log_time_minutes);

        Intent i = getIntent();
        username = i.getStringExtra("USERNAME");
        activityName = i.getStringExtra("ACTIVITY_NAME");
        samplingRate = getSamplingRate();

        setOnClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

                        Intent i = new Intent(SensorLogActivity.this, PhoneSensorLogService.class);
                        if (timedMode) {
                            // prompt user if no time entered
                            if (mLogTime.getText().toString().isEmpty()) {
                                Toast.makeText(getApplicationContext(), "Please enter a valid time",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                minutes = Integer.parseInt(mLogTime.getText().toString());

                                mLogStartButton.setEnabled(false);
                                i.putExtra("USERNAME", username);
                                i.putExtra("ACTIVITY_NAME", activityName);
                                i.putExtra("TIMED_MODE", timedMode);
                                i.putExtra("SAMPLING_RATE", samplingRate);
                                i.putExtra("MINUTES", minutes);

                                // Send settings and start logging on watch
                                sendSettingsandStart();

                                // Start service on phone
                                startService(i);
                            }
                        }
                    }
                });

        mLogStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogStopButton.setEnabled(false);
                sendMessage("/stop");

                Intent i = new Intent(SensorLogActivity.this, PhoneSensorLogService.class);
                stopService(i);
            }
        });
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
    private void sendSettingsandStart() {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/settings");
        putDataMapRequest.getDataMap().putInt("MINUTES", minutes);
        putDataMapRequest.getDataMap().putInt("SAMPLING_RATE", getSamplingRate());
        putDataMapRequest.getDataMap().putBoolean("TIMED_MODE", timedMode);
        putDataMapRequest.getDataMap().putString("USERNAME", username);
        putDataMapRequest.getDataMap().putString("ACTIVITY_NAME", activityName);
        putDataMapRequest.getDataMap().putLong("TIMESTAMP", System.currentTimeMillis());

        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest);
    }

    private int getSamplingRate() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return Integer.parseInt(sharedPreferences.getString("pref_samplingRate", "0"));
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