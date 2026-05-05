package com.example.supportapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.supportapplication.adapters.TaskAdapter;
import com.example.supportapplication.models.SupportTask;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ViewTasksActivity extends AppCompatActivity {

    private ListView tasksListView;
    private TextView emptyTextView;
    private TaskAdapter taskAdapter;
    private List<SupportTask> taskList;
    private List<SupportTask> filteredTaskList;
    private DatabaseReference tasksReference;
    private Spinner statusSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_tasks);

        tasksListView = findViewById(R.id.tasksListView);
        emptyTextView = findViewById(R.id.emptyTextView);
        statusSpinner = findViewById(R.id.statusSpinner);

        taskList = new ArrayList<>();
        filteredTaskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(this, filteredTaskList);
        tasksListView.setAdapter(taskAdapter);

        tasksReference = FirebaseDatabase.getInstance().getReference("tasks");

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(spinnerAdapter);

        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterTasksByStatus(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        tasksListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SupportTask selectedTask = filteredTaskList.get(position);
                Intent intent = new Intent(ViewTasksActivity.this, TaskDetailActivity.class);
                intent.putExtra("taskId", selectedTask.getId());
                startActivity(intent);
            }
        });

        tasksListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showDeleteConfirmationDialog(position);
                return true;
            }
        });

        loadTasks();
    }

    private void loadTasks() {
        tasksReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                taskList.clear();
                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    SupportTask task = taskSnapshot.getValue(SupportTask.class);
                    taskList.add(task);
                }
                filterTasksByStatus(statusSpinner.getSelectedItem().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ViewTasksActivity.this, "Ошибка загрузки заявок", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterTasksByStatus(String status) {
        filteredTaskList.clear();
        if (status.equals("All")) {
            filteredTaskList.addAll(taskList);
        } else {
            for (SupportTask task : taskList) {
                if (task.getStatus().equals(status)) {
                    filteredTaskList.add(task);
                }
            }
        }
        taskAdapter.notifyDataSetChanged();
        boolean isEmpty = filteredTaskList.isEmpty();
        tasksListView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void showDeleteConfirmationDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Вы действительно хотите удалить эту заявку ?");
        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteTask(position);
            }
        });
        builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void deleteTask(int position) {
        SupportTask taskToDelete = filteredTaskList.get(position);
        tasksReference.child(taskToDelete.getId()).removeValue();
        taskList.remove(taskToDelete);
        filterTasksByStatus(statusSpinner.getSelectedItem().toString());
    }
}
