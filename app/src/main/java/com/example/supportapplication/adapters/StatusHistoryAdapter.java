package com.example.supportapplication.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.supportapplication.R;
import com.example.supportapplication.models.StatusChange;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatusHistoryAdapter extends ArrayAdapter<StatusChange> {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public StatusHistoryAdapter(Context context, List<StatusChange> statusChanges) {
        super(context, 0, statusChanges);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_status_change, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        StatusChange statusChange = getItem(position);
        holder.status.setText(statusChange.getStatus());
        holder.changedBy.setText(statusChange.getChangedBy());
        holder.timestamp.setText(DATE_FORMAT.format(new Date(statusChange.getTimestamp())));

        switch (statusChange.getStatus()) {
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
        final TextView status;
        final TextView changedBy;
        final TextView timestamp;

        ViewHolder(View view) {
            status = view.findViewById(R.id.statusTextView);
            changedBy = view.findViewById(R.id.changedByTextView);
            timestamp = view.findViewById(R.id.timestampTextView);
        }
    }
}
