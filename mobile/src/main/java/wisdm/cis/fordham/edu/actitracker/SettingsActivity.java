package wisdm.cis.fordham.edu.actitracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages logging settings.
 * 1. Sensor List - Dynamically updated for device. Name of sensor is the entry and their constant
 * int code (Sensor.getType()) is the value.
 * 2. Sampling Rate - Through xml/preferences.xml
 */
public class SettingsActivity extends PreferenceActivity {

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {

        private static final String PREF_SENSOR_LIST_PHONE = "pref_sensorListPhone";
        private static final String PREF_SENSOR_LIST_WEAR = "pref_sensorListWear";
        private static final String GET_SENSORS = "/get_sensors";
        private static final String WATCH_SENSORS = "/watch_sensors";
        private static final String SENSOR_LIST_STRING = "SENSOR_LIST_STRING";
        private static final String SENSOR_CODES = "SENSOR_CODES";

        private GoogleApiClient mGoogleApiClient;
        private LocalBroadcastManager mLocalBroadcastManager;
        private BroadcastReceiver mBroadcastReceiver;
        private ArrayList<String> mWearSensorListString;
        private ArrayList<Integer> mWearSensorCodes;
        private PreferenceScreen mPreferenceScreen;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            initializeGoogleApiClient();
            sendMessage(GET_SENSORS);

            /**
             * Receiver for list of sensors from watch (via PhoneListenerService).
             * Once this is received, update settings page.
             */
            mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(WATCH_SENSORS)) {
                        Log.d(TAG, "Broadcast receives.");
                        mWearSensorListString = intent.getStringArrayListExtra(SENSOR_LIST_STRING);
                        mWearSensorCodes = intent.getIntegerArrayListExtra(SENSOR_CODES);
                        MultiSelectListPreference preferenceListWear = new MultiSelectListPreference(getActivity());
                        preferenceListWear.setKey(PREF_SENSOR_LIST_WEAR);
                        preferenceListWear.setTitle(getResources().getString(R.string.sensor_list_wear));
                        preferenceListWear.setEntries(
                                mWearSensorListString.toArray(new CharSequence[mWearSensorListString.size()]));
                        CharSequence[] wearSensorCodes = new CharSequence[mWearSensorCodes.size()];
                        for (int i = 0; i < mWearSensorCodes.size(); i++) {
                            wearSensorCodes[i] = mWearSensorCodes.get(i).toString();
                        }
                        preferenceListWear.setEntryValues(wearSensorCodes);
                        mPreferenceScreen.addPreference(preferenceListWear);
                        setPreferenceScreen(mPreferenceScreen);
                    }
                }
            };
            mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
            IntentFilter mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(WATCH_SENSORS);
            mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);

            // Create preference for choosing phone sensors to log with
            mPreferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
            MultiSelectListPreference preferenceListPhone = new MultiSelectListPreference(getActivity());
            preferenceListPhone.setKey(PREF_SENSOR_LIST_PHONE);
            preferenceListPhone.setTitle(getResources().getString(R.string.sensor_list_phone));
            preferenceListPhone.setEntries(getSensorList());
            preferenceListPhone.setEntryValues(getSensorCodes());
            mPreferenceScreen.addPreference(preferenceListPhone);

            setPreferenceScreen(mPreferenceScreen);
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
            mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        }

        /**
         * Get phone sensor list by name
         * @return
         */
        private CharSequence[] getSensorList() {
            SensorManager sensorManager = (SensorManager)getActivity().getSystemService(SENSOR_SERVICE);
            List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
            List<String> sensorListString = new ArrayList<String>();
            for (Sensor sensor : sensorList) {
                sensorListString.add(sensor.getName());
            }

            return sensorListString.toArray(new CharSequence[sensorListString.size()]);
        }

        /**
         * Get phone sensor list by constant id (int)
         * @return
         */
        private CharSequence[] getSensorCodes() {
            SensorManager sensorManager = (SensorManager)getActivity().getSystemService(SENSOR_SERVICE);
            List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
            List<String> sensorCodes = new ArrayList<String>();
            for (Sensor sensor : sensorList) {
                sensorCodes.add(String.valueOf(sensor.getType()));
            }

            return sensorCodes.toArray(new CharSequence[sensorCodes.size()]);
        }

        /**
         * Needed to connect to watch (Android Wear).
         */
        private void initializeGoogleApiClient() {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
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
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mGoogleApiClient.blockingConnect();
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
    }
}