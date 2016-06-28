package wisdm.cis.fordham.edu.actitracker;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener for messages/data from phone.
 * If data - starts logging service (WearSensorLogService) and passes necessary parameters.
 * If message - stops logging service.
 */
public class WearListenerService extends WearableListenerService {

    private static final String TAG = "WearListenerService";
    private static final String STOP_COLLECTION = "/stop";
    private static final String GET_SENSORS = "/get_sensors";
    private static final String SENSOR_LIST_STRING = "SENSOR_LIST_STRING";
    private static final String SENSOR_CODES = "SENSOR_CODES";
    private static final String SETTINGS = "/settings";
    private static final String MINUTES = "MINUTES";
    private static final String SAMPLING_RATE = "SAMPLING_RATE";
    private static final String TIMED_MODE = "TIMED_MODE";
    private static final String TIMESTAMP = "TIMESTAMP";
    private static final String USERNAME = "USERNAME";
    private static final String ACTIVITY_NAME = "ACTIVITY_NAME";
    private static final String DELAY = "DELAY";
    private static final String WATCH_SENSOR_CODES = "WATCH_SENSOR_CODES";
    private static final String WATCH_SENSORS = "/watch_sensors";

    private GoogleApiClient mGoogleApiClient;
    private int minutes;
    private int samplingRate;
    private boolean timedMode;
    private long phoneToWatchDelay;
    private String username;
    private String activityName;
    private ArrayList<Integer> watchSensorCodes;

    public WearListenerService() {
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(STOP_COLLECTION)) {
            Intent i = new Intent(WearListenerService.this, WearSensorLogService.class);
            stopService(i);
        }

        if (messageEvent.getPath().equals(GET_SENSORS)) {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WATCH_SENSORS);
            SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
            ArrayList<String> sensorListString = new ArrayList<String>();
            ArrayList<Integer> sensorCodes = new ArrayList<Integer>();
            for (Sensor sensor : sensorList) {
                sensorListString.add(sensor.getName());
                sensorCodes.add(sensor.getType());
                Log.d(TAG, "String: " + sensor.getName() + " Code: " + sensor.getType());
            }

            putDataMapRequest.getDataMap().putLong(TIMESTAMP, System.currentTimeMillis());
            putDataMapRequest.getDataMap().putStringArrayList(SENSOR_LIST_STRING, sensorListString);
            putDataMapRequest.getDataMap().putIntegerArrayList(SENSOR_CODES, sensorCodes);
            PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();
            mGoogleApiClient.blockingConnect();
            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer events) {
        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals(SETTINGS)) {
                DataItem item = event.getDataItem();
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                minutes = dataMap.getInt(MINUTES);
                samplingRate = dataMap.getInt(SAMPLING_RATE);
                timedMode = dataMap.getBoolean(TIMED_MODE);
                phoneToWatchDelay = dataMap.getLong(TIMESTAMP);
                username = dataMap.getString(USERNAME);
                activityName = dataMap.getString(ACTIVITY_NAME);
                watchSensorCodes = dataMap.getIntegerArrayList(WATCH_SENSOR_CODES);
            }
        }

        Intent i = new Intent(WearListenerService.this, WearSensorLogService.class);

        i.putExtra(MINUTES, minutes);
        i.putExtra(SAMPLING_RATE, samplingRate);
        i.putExtra(TIMED_MODE, timedMode);
        i.putExtra(DELAY, phoneToWatchDelay);
        i.putExtra(USERNAME, username);
        i.putExtra(ACTIVITY_NAME, activityName);
        i.putExtra(WATCH_SENSOR_CODES, watchSensorCodes);
        startService(i);
    }
}