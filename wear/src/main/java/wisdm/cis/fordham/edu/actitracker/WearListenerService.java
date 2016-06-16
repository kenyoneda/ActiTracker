package wisdm.cis.fordham.edu.actitracker;

import android.content.Intent;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Listener for messages/data from phone.
 * If data - starts logging service (WearSensorLogService) and passes necessary parameters.
 * If message - stops logging service.
 */
public class WearListenerService extends WearableListenerService {

    private static final String TAG = "WearListenerService";
    private static final String STOP_COLLECTION = "/stop";
    private static final String SETTINGS = "/settings";
    private static final String MINUTES = "MINUTES";
    private static final String SAMPLING_RATE = "SAMPLING_RATE";
    private static final String TIMED_MODE = "TIMED_MODE";
    private static final String TIMESTAMP = "TIMESTAMP";
    private static final String USERNAME = "USERNAME";
    private static final String ACTIVITY_NAME = "ACTIVITY_NAME";
    private static final String DELAY = "DELAY";

    private int minutes;
    private int samplingRate;
    private boolean timedMode;
    private long phoneToWatchDelay;
    private String username;
    private String activityName;

    public WearListenerService() {
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(STOP_COLLECTION)) {
            Intent i = new Intent(WearListenerService.this, WearSensorLogService.class);
            stopService(i);
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
            }
        }

        Intent i = new Intent(WearListenerService.this, WearSensorLogService.class);

        i.putExtra(MINUTES, minutes);
        i.putExtra(SAMPLING_RATE, samplingRate);
        i.putExtra(TIMED_MODE, timedMode);
        i.putExtra(DELAY, phoneToWatchDelay);
        i.putExtra(USERNAME, username);
        i.putExtra(ACTIVITY_NAME, activityName);
        startService(i);
    }
}