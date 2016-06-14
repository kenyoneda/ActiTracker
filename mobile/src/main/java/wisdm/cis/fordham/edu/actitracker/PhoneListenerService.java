package wisdm.cis.fordham.edu.actitracker;

import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.apache.commons.lang3.SerializationUtils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class PhoneListenerService extends WearableListenerService {

    private static final String TAG = "PhoneListenerService";

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
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals("/data")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset watchAccelAsset = dataMapItem.getDataMap().getAsset("ACCEL_ASSET");
                Asset watchGyroAsset = dataMapItem.getDataMap().getAsset("GYRO_ASSET");
                String username = dataMapItem.getDataMap().getString("USERNAME");
                String activityName = dataMapItem.getDataMap().getString("ACTIVITY_NAME");

                ArrayList<ThreeTupleRecord> watchAccelData = loadDataFromAsset(watchAccelAsset);
                ArrayList<ThreeTupleRecord> watchGyroData = loadDataFromAsset(watchGyroAsset);

                writeFiles(watchAccelData, watchGyroData, username, activityName);
            }
        }
    }

    private void writeFiles(ArrayList<ThreeTupleRecord> watchAccelRecords,
                            ArrayList<ThreeTupleRecord> watchGyroRecords,
                            String username, String activityName) {
        File directory = SensorFileSaver.getDirectory(this, username, activityName);
        File watchAccelFile = SensorFileSaver.createFile(directory, username, activityName, "watch_accel");
        File watchGyroFile = SensorFileSaver.createFile(directory, username, activityName, "watch_gyro");
        SensorFileSaver.writeFile(watchAccelFile, watchAccelRecords);
        SensorFileSaver.writeFile(watchGyroFile, watchGyroRecords);
    }

    /**
     * Helper function to unpack ArrayList of records from byte stream.
     * @param asset
     * @return
     */
    private ArrayList<ThreeTupleRecord> loadDataFromAsset(Asset asset) {
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

        return (ArrayList<ThreeTupleRecord>) SerializationUtils.deserialize(assetInputStream);
    }
}