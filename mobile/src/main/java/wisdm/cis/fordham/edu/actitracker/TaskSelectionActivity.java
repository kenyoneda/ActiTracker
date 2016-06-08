package wisdm.cis.fordham.edu.actitracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Activity where user can add/choose task to log sensor data for. See TaskFragment to see
 * implementation of the task list.
 * User can also navigate to settings.
 */
public class TaskSelectionActivity extends AppCompatActivity {

    private static final String TAG = "TaskSelectionActivity";

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_selection);

        Intent i = getIntent();
        username = i.getStringExtra("USERNAME");
    }

    // For TaskFragment (list of tasks) to pass username to next activity (SensorLogActivity)
    protected String getUsername() {
        return username;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, 0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}