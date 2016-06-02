package wisdm.cis.fordham.edu.actitracker;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ListViewAdapter extends ArrayAdapter<String> {
    private SparseBooleanArray mSelectedItemsIds;
    List<String> list;
    LayoutInflater inflater;

    public ListViewAdapter(Context context, int resource, List<String> list) {
        super(context, resource, list);
        mSelectedItemsIds = new  SparseBooleanArray();
        this.list = list;

        inflater = LayoutInflater.from(getContext());
    }

    public void toggleSelection(int i) {
        selectView(i, !mSelectedItemsIds.get(i));
    }

    private void selectView(int i, boolean b) {
        if (b)
            mSelectedItemsIds.put(i,  b);
        else
            mSelectedItemsIds.delete(i);

        notifyDataSetChanged();
    }

    public SparseBooleanArray getSelectedIds() {
        return mSelectedItemsIds;
    }

    private class ViewHolder {
        TextView textView;
    }

    public View getView(int position, View view, ViewGroup parent) {
        final ViewHolder holder;

        if (view == null) {
            holder = new ViewHolder();
            view = inflater.inflate(R.layout.list_item, parent, false);

            holder.textView = (TextView)  view.findViewById(R.id.activity);
            view.setTag(holder);

        } else {
            holder = (ViewHolder)  view.getTag();
        }

        holder.textView.setText(list.get(position));

        return view;
    }
}