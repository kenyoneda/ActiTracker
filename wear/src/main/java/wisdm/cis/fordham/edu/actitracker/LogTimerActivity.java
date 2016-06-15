package wisdm.cis.fordham.edu.actitracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class LogTimerActivity extends Activity {

    private TextView mTextView;
    private TextView mSeconds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mSeconds = (TextView) stub.findViewById(R.id.seconds);
                Bundle extras = getIntent().getExtras();
                if (extras == null) {
                    mSeconds.setText(getResources().getString(R.string.wear_error));
                }
                else {
                    int seconds = extras.getInt("MINUTES", 0) * 60;
                    new CountDownTimer(Long.valueOf(seconds) * 1000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            String time = String.format("%02d : %02d",
                                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60);
                            mSeconds.setText(time);
                        }

                        public void onFinish() {
                            mSeconds.setText("Done!");
                        }
                    }.start();
                }
            }
        });
    }
}