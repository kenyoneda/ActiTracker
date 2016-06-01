package wisdm.cis.fordham.edu.actitracker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

public class SensorLogActivity extends AppCompatActivity {

    private static final String TAG = "SensorLogActivity";

    private Button mLogStartButton;
    private Button mLogStopButton;
    private EditText mLogTime;
    private String username;
    private String activityName;
    private boolean timedMode;
    private int minutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_log);

        mLogStartButton = (Button)findViewById(R.id.log_start_button);
        mLogStopButton = (Button)findViewById(R.id.log_stop_button);
        mLogTime = (EditText)findViewById(R.id.log_time_minutes);

        Intent i = getIntent();
        username = i.getStringExtra("USERNAME");
        activityName = i.getStringExtra("ACTIVITY_NAME");

        mLogStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SensorLogActivity.this, PhoneSensorLogService.class);
                i.putExtra("USERNAME", username);
                i.putExtra("ACTIVITY_NAME", activityName);
                if (timedMode) {
                    minutes = Integer.parseInt(mLogTime.getText().toString());
                    i.putExtra("MINUTES", minutes);
                }
                i.putExtra("TIMEDMODE", timedMode);
                startService(i);
            }
        });

        mLogStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SensorLogActivity.this, PhoneSensorLogService.class);
                stopService(i);
            }
        });
    }

    /**
     * Set UI to corresponding timer mode.
     * Timer: EditText to set time and start button.
     * Manual: Start/stop button.
     *
     * @param view
     */
    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

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
