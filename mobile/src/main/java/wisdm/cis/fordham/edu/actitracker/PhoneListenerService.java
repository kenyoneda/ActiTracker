package wisdm.cis.fordham.edu.actitracker;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.apache.commons.lang3.SerializationUtils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Listens for sensor logging data from watch (asset byte stream) and unpacks into
 * ArrayList so it can be written to a file.
 */
public class PhoneListenerService extends WearableListenerService {

    private static final String TAG = "PhoneListenerService";
    private static final String USERNAME = "USERNAME";
    private static final String ACTIVITY_NAME = "ACTIVITY_NAME";
    private static final String DATA = "/data";
    private static final String WATCH_SENSORS = "/watch_sensors";
    private static final String SENSOR_LIST_STRING = "SENSOR_LIST_STRING";
    private static final String SENSOR_CODES = "SENSOR_CODES";
    private static final String WATCH_SENSOR_NAMES = "WATCH_SENSOR_NAMES";

    public PhoneListenerService() {
    }

    /**
     * Gets sensor data from watch and unpacks from asset.
     * Then writes watch files to disk.
     * @param dataEvents
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged called");

        for (DataEvent event : dataEvents) {
            /**
             * Sensor data from watch
             */
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals(DATA)) {

                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                // Names & assets of watch sensors that were sampled with
                ArrayList<String> watchSensorNames =
                        dataMapItem.getDataMap().getStringArrayList(WATCH_SENSOR_NAMES);
                ArrayList<Asset> assets = new ArrayList<Asset>(watchSensorNames.size());
                for (String sensor : watchSensorNames) {
                    assets.add(dataMapItem.getDataMap().getAsset(sensor));
                }

                String username = dataMapItem.getDataMap().getString(USERNAME);
                String activityName = dataMapItem.getDataMap().getString(ACTIVITY_NAME);

                // Unpack data from each asset and write to file
                for (int i = 0; i < assets.size(); i++) {
                    ArrayList<SensorRecord> record = loadDataFromAsset(assets.get(i));
                    writeFile(record, username, activityName, watchSensorNames.get(i));
                }
            }

            /**
             * Sensor list from watch. Send local broadcast to settings page.
             */
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals(WATCH_SENSORS)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                DataMap dataMap = dataMapItem.getDataMap();

                ArrayList<String> sensorListString = dataMap.getStringArrayList(SENSOR_LIST_STRING);
                ArrayList<Integer> sensorCodes = dataMap.getIntegerArrayList(SENSOR_CODES);

                Intent i = new Intent(WATCH_SENSORS);
                i.putExtra(SENSOR_LIST_STRING, sensorListString);
                i.putExtra(SENSOR_CODES, sensorCodes);
                LocalBroadcastManager localBroadcastManager =
                        LocalBroadcastManager.getInstance(PhoneListenerService.this);
                localBroadcastManager.sendBroadcast(i);
            }
        }
    }

    private void writeFile(ArrayList<SensorRecord> record, String username, String activityName,
                           String sensorName) {
        File directory = SensorFileSaver.getDirectory(this, username, activityName);
        File file = SensorFileSaver.createFile(this, directory, username, activityName, sensorName);
        SensorFileSaver.writeFile(file, record);
    }

    /**
     * Helper function to unpack ArrayList of records from byte stream.
     * @param asset
     * @return
     */
    private ArrayList<SensorRecord> loadDataFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult result = googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!result.isSuccess()) {
            return null;
        }

        InputStream assetInputStream = Wearable.DataApi
                .getFdForAsset(googleApiClient, asset).await().getInputStream();
        googleApiClient.disconnect();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
        }

        return (ArrayList<SensorRecord>) SerializationUtils.deserialize(assetInputStream);
    }
}