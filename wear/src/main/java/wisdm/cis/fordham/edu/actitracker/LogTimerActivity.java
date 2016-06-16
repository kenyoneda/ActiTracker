package wisdm.cis.fordham.edu.actitracker;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

/**
 * This class displays a countdown timer when app is logging in timed mode.
 * Closes itself when done.
 */
public class LogTimerActivity extends Activity {

    private static final String TAG = "LogTimerActivity";
    private TextView mSeconds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_log_timer);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mSeconds = (TextView) stub.findViewById(R.id.seconds);
                Bundle extras = getIntent().getExtras();
                if (extras == null) {
                    mSeconds.setText(getResources().getString(R.string.wear_error));
                } else {
                    long milliseconds = extras.getInt("MINUTES", 0) * 60 * 1000;
                    long ms = 1000L;

                    new CountDownTimer(milliseconds, ms) {
                        public void onTick(long millisUntilFinished) {
                            String time = String.format("%02d : %02d",
                                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60);
                            mSeconds.setText(time);
                        }

                        // Close activity when done
                        public void onFinish() {
                            finish();
                        }
                    }.start();
                }
            }
        });
    }
}