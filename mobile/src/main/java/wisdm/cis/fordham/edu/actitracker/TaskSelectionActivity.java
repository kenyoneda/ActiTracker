package wisdm.cis.fordham.edu.actitracker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Activity where user can add/choose task to log sensor data for.
 * User can also navigate to settings.
 */
public class TaskSelectionActivity extends AppCompatActivity {
    private static final String TAG = "TaskSelectionActivity";

    private String username;
    ArrayList<String> taskList = new ArrayList<>();
    ListViewAdapter mAdapter;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_selection);

        Intent i = getIntent();
        username = i.getStringExtra("USERNAME");

        listView = (ListView) findViewById(R.id.list);
        mAdapter = new ListViewAdapter(this, R.layout.list_item, taskList);
        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String activity = listView.getItemAtPosition(i).toString();
                Intent intent = new Intent(getApplicationContext(), SensorLogActivity.class);
                intent.putExtra("USERNAME", (getUsername()));
                intent.putExtra("ACTIVITY_NAME", activity);
                startActivity(intent);
            }
        });
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {
                actionMode.setTitle(listView.getCheckedItemCount() + " Selected");
                mAdapter.toggleSelection(i);
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                actionMode.getMenuInflater().inflate(R.menu.context_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.delete) {
                    SparseBooleanArray selected = mAdapter.getSelectedIds();

                    for (int i =  (selected.size() - 1); i >= 0; i--) {
                        if (selected.valueAt(i)) {
                            String selectedItem = mAdapter.getItem(selected.keyAt(i));
                            mAdapter.remove(selectedItem);
                        }
                    }

                    actionMode.finish();
                    selected.clear();

                    return true;
                } else return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
            }
        });
    }

    public String getUsername() {
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

    public void addActivity(View view) {
        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setHint("Task name");
        input.setLayoutParams(lp);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New task");
        builder.setMessage("Enter new task name");
        builder.setView(input);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                taskList.add(input.getText().toString());
                mAdapter.notifyDataSetChanged();
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.create().show();

    }
}
