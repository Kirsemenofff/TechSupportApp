package com.example.supportapplication.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.supportapplication.R;
import com.example.supportapplication.models.SupportTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskRecyclerAdapter extends RecyclerView.Adapter<TaskRecyclerAdapter.ViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(SupportTask task);
        void onTaskLongClick(SupportTask task);
    }

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DUE_FORMAT =
            new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    private final List<SupportTask> tasks;
    private final OnTaskClickListener listener;

    public TaskRecyclerAdapter(List<SupportTask> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SupportTask task = tasks.get(position);

        holder.title.setText(task.getTitle());
        holder.description.setText(task.getDescription());
        holder.status.setText(task.getStatus());
        holder.priority.setText(task.getPriority());
        holder.date.setText(DATE_FORMAT.format(new Date(task.getTimestamp())));

        // Category chip
        String category = task.getCategory();
        if (category != null && !category.isEmpty()) {
            holder.category.setVisibility(View.VISIBLE);
            holder.category.setText(category);
        } else {
            holder.category.setVisibility(View.GONE);
        }

        // Deadline
        if (task.getDueDate() > 0) {
            holder.deadline.setVisibility(View.VISIBLE);
            holder.deadline.setText("До: " + DUE_FORMAT.format(new Date(task.getDueDate())));
            boolean overdue = task.getDueDate() < System.currentTimeMillis()
                    && !task.getStatus().equals("Completed");
            holder.deadline.setTextColor(Color.parseColor(overdue ? "#EF4444" : "#94A3B8"));
        } else {
            holder.deadline.setVisibility(View.GONE);
        }

        // Status color
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

        // Priority color
        switch (task.getPriority()) {
            case "Критический":
                holder.priority.setTextColor(Color.parseColor("#EF4444"));
                break;
            case "Высокий":
                holder.priority.setTextColor(Color.parseColor("#F59E0B"));
                break;
            case "Средний":
                holder.priority.setTextColor(Color.parseColor("#3B82F6"));
                break;
            default:
                holder.priority.setTextColor(Color.parseColor("#10B981"));
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(task);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onTaskLongClick(task);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, status, description, priority, date, category, deadline;

        ViewHolder(View view) {
            super(view);
            title       = view.findViewById(R.id.taskTitle);
            status      = view.findViewById(R.id.taskStatus);
            description = view.findViewById(R.id.taskDescription);
            priority    = view.findViewById(R.id.taskPriority);
            date        = view.findViewById(R.id.taskDate);
            category    = view.findViewById(R.id.taskCategory);
            deadline    = view.findViewById(R.id.taskDeadline);
        }
    }
}
