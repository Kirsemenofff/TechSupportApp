package com.example.supportapplication.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.supportapplication.R;
import com.example.supportapplication.models.SupportTask;

import java.util.List;

public class TaskAdapter extends ArrayAdapter<SupportTask> {

    public TaskAdapter(Context context, List<SupportTask> tasks) {
        super(context, 0, tasks);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_task, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SupportTask task = getItem(position);
        holder.title.setText(task.getTitle());
        holder.status.setText(task.getStatus());
        holder.description.setText(task.getDescription());

        switch (task.getStatus()) {
            case "Completed":
                holder.status.setTextColor(Color.parseColor("#10B981"));
                break;
            case "In Progress":
                holder.status.setTextColor(Color.parseColor("#3B82F6"));
                break;
            default:
                holder.status.setTextColor(Color.parseColor("#F59E0B"));
                break;
        }

        return convertView;
    }

    static class ViewHolder {
        final TextView title;
        final TextView status;
        final TextView description;

        ViewHolder(View view) {
            title = view.findViewById(R.id.taskTitle);
            status = view.findViewById(R.id.taskStatus);
            description = view.findViewById(R.id.taskDescription);
        }
    }
}
