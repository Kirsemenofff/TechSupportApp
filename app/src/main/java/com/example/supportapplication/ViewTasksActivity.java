package com.example.supportapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ViewTasksActivity extends AppCompatActivity {

    private static final int PRIORITY_ORDER = 0;
    private static final int DATE_DESC_ORDER = 1;
    private static final int DATE_ASC_ORDER = 2;
    private static final int TITLE_ORDER = 3;

    private ListView tasksListView;
    private TextView emptyTextView;
    private EditText searchEditText;
    private Spinner statusSpinner;
    private Spinner sortSpinner;
    private TaskAdapter taskAdapter;
    private List<SupportTask> taskList;
    private List<SupportTask> filteredTaskList;
    private DatabaseReference tasksReference;
    private ValueEventListener tasksListener;
    private String currentSearch = "";
    private int currentSort = DATE_DESC_ORDER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_tasks);

        tasksListView = findViewById(R.id.tasksListView);
        emptyTextView = findViewById(R.id.emptyTextView);
        searchEditText = findViewById(R.id.searchEditText);
        statusSpinner = findViewById(R.id.statusSpinner);
        sortSpinner = findViewById(R.id.sortSpinner);

        taskList = new ArrayList<>();
        filteredTaskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(this, filteredTaskList);
        tasksListView.setAdapter(taskAdapter);

        tasksReference = FirebaseDatabase.getInstance().getReference("tasks");

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(spinnerAdapter);

        String[] sortOptions = {"По дате ↓", "По дате ↑", "По приоритету", "По названию"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFiltersAndSort();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSort = position;
                applyFiltersAndSort();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().trim().toLowerCase();
                applyFiltersAndSort();
            }
            @Override public void afterTextChanged(Editable s) {}
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
        tasksListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                taskList.clear();
                for (DataSnapshot taskSnapshot : dataSnapshot.getChildren()) {
                    SupportTask task = taskSnapshot.getValue(SupportTask.class);
                    if (task != null) taskList.add(task);
                }
                applyFiltersAndSort();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ViewTasksActivity.this, "Ошибка загрузки заявок", Toast.LENGTH_SHORT).show();
            }
        };
        tasksReference.addValueEventListener(tasksListener);
    }

    private void applyFiltersAndSort() {
        String status = statusSpinner.getSelectedItem() != null
                ? statusSpinner.getSelectedItem().toString() : "All";

        filteredTaskList.clear();
        for (SupportTask task : taskList) {
            boolean matchesStatus = status.equals("All") || task.getStatus().equals(status);
            boolean matchesSearch = currentSearch.isEmpty()
                    || task.getTitle().toLowerCase().contains(currentSearch)
                    || task.getDescription().toLowerCase().contains(currentSearch)
                    || task.getRoomNumber().toLowerCase().contains(currentSearch);
            if (matchesStatus && matchesSearch) {
                filteredTaskList.add(task);
            }
        }

        switch (currentSort) {
            case PRIORITY_ORDER:
                Collections.sort(filteredTaskList, new Comparator<SupportTask>() {
                    @Override
                    public int compare(SupportTask a, SupportTask b) {
                        return priorityWeight(b.getPriority()) - priorityWeight(a.getPriority());
                    }
                });
                break;
            case DATE_ASC_ORDER:
                Collections.sort(filteredTaskList, new Comparator<SupportTask>() {
                    @Override
                    public int compare(SupportTask a, SupportTask b) {
                        return Long.compare(a.getTimestamp(), b.getTimestamp());
                    }
                });
                break;
            case TITLE_ORDER:
                Collections.sort(filteredTaskList, new Comparator<SupportTask>() {
                    @Override
                    public int compare(SupportTask a, SupportTask b) {
                        return a.getTitle().compareToIgnoreCase(b.getTitle());
                    }
                });
                break;
            default: // DATE_DESC_ORDER
                Collections.sort(filteredTaskList, new Comparator<SupportTask>() {
                    @Override
                    public int compare(SupportTask a, SupportTask b) {
                        return Long.compare(b.getTimestamp(), a.getTimestamp());
                    }
                });
                break;
        }

        taskAdapter.notifyDataSetChanged();
        boolean isEmpty = filteredTaskList.isEmpty();
        tasksListView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private int priorityWeight(String priority) {
        switch (priority) {
            case "Критический": return 4;
            case "Высокий":     return 3;
            case "Средний":     return 2;
            default:            return 1;
        }
    }

    private void showDeleteConfirmationDialog(final int position) {
        new AlertDialog.Builder(this)
                .setMessage("Вы действительно хотите удалить эту заявку?")
                .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deleteTask(position);
                    }
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private void deleteTask(int position) {
        SupportTask taskToDelete = filteredTaskList.get(position);
        tasksReference.child(taskToDelete.getId()).removeValue();
        taskList.remove(taskToDelete);
        applyFiltersAndSort();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tasksListener != null) {
            tasksReference.removeEventListener(tasksListener);
        }
    }
}
