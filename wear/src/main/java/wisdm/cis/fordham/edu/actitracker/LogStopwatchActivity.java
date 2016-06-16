package wisdm.cis.fordham.edu.actitracker;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.widget.Chronometer;
import android.widget.TextView;

public class LogStopwatchActivity extends Activity {

    private static final String TAG = "LogStopwatchActivity";
    private static final String STOP_STOPWATCH = "stop_stopwatch";

    private Chronometer mChronometer;
    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_stopwatch);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mChronometer = (Chronometer) stub.findViewById(R.id.stopwatch);
                mChronometer.start();
            }
        });

        // Listen for when to stop stopwatch
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(STOP_STOPWATCH)) {
                    mChronometer.stop();
                    finish();
                }
            }
        };
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(STOP_STOPWATCH);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }
}
