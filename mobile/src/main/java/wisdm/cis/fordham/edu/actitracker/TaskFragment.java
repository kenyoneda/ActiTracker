package wisdm.cis.fordham.edu.actitracker;

import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Comment...
 */
public class TaskFragment extends ListFragment {

    private static final String TAG = "TaskFragment";
    private static final String USERNAME = "USERNAME";
    private static final String ACTIVITY_NAME = "ACTIVITY_NAME";

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ArrayList<String> defaultTasks = new ArrayList<String>(
                Arrays.asList(getResources().getStringArray(R.array.default_tasks)));

        // Populate list with default activities
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, defaultTasks);
        setListAdapter(adapter);
    }

    /**
     * On task click, move to SensorLogActivity to prepare user for sensor logging.
     *
     * @param l
     * @param v
     * @param position
     * @param id
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String activity = getListAdapter().getItem(position).toString();
        Toast.makeText(getActivity().getApplicationContext(), "Task Chosen: " + activity, Toast.LENGTH_SHORT).show();
        Intent i = new Intent(getActivity(), SensorLogActivity.class);
        i.putExtra(ACTIVITY_NAME, ((TaskSelectionActivity)getActivity()).getUsername());
        i.putExtra(ACTIVITY_NAME, activity);
        startActivity(i);
    }
}