package wisdm.cis.fordham.edu.actitracker;

import android.content.Intent;
import android.provider.ContactsContract;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.apache.commons.lang3.BooleanUtils;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

/**
 * Listener for messages from phone.
 */
public class WearListenerService extends WearableListenerService {

    private static final String TAG = "WearListenerService";
    private static final String START_COLLECTION = "/start";
    private static final String STOP_COLLECTION = "/stop";
    private static final String SETTINGS = "/settings";
    private static final String TIMED_MODE = "/timedMode";

    private int samplingRate;
    private boolean timedMode;

    public WearListenerService() {
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(START_COLLECTION)) {
            int minutes = new BigInteger(messageEvent.getData()).intValue();
            Intent i = new Intent(WearListenerService.this, WearSensorLogService.class);
            i.putExtra("MINUTES", minutes);
            i.putExtra("SAMPLING_RATE", samplingRate);
            i.putExtra("TIMED_MODE", timedMode);
            startService(i);
        }

        if (messageEvent.getPath().equals(STOP_COLLECTION)) {
            Intent i = new Intent(WearListenerService.this, WearSensorLogService.class);
            stopService(i);
        }

        if (messageEvent.getPath().equals(SETTINGS)) {
            samplingRate = new BigInteger(messageEvent.getData()).intValue();
            Log.d(TAG, "settings: " + samplingRate);
        }

        if (messageEvent.getPath().equals(TIMED_MODE)) {
            timedMode = BooleanUtils.toBoolean(new BigInteger(messageEvent.getData()).intValue());
            Log.d(TAG, "timedmode: " + timedMode);
        }
    }
}