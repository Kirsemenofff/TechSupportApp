package com.example.supportapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.supportapplication.adapters.TaskRecyclerAdapter;
import com.example.supportapplication.models.SupportTask;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewTasksActivity extends AppCompatActivity {

    private static final int SORT_DATE_DESC = 0;
    private static final int SORT_DATE_ASC  = 1;
    private static final int SORT_PRIORITY  = 2;
    private static final int SORT_TITLE     = 3;

    private RecyclerView tasksRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyStateLayout;
    private TextView emptyTextView;
    private EditText searchEditText;
    private Spinner statusSpinner, categoryFilterSpinner, sortSpinner;

    private TaskRecyclerAdapter taskAdapter;
    private final List<SupportTask> taskList = new ArrayList<>();
    private final List<SupportTask> filteredList = new ArrayList<>();

    private DatabaseReference tasksReference;
    private ValueEventListener tasksListener;

    private String currentSearch = "";
    private int currentSort = SORT_DATE_DESC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_tasks);

        tasksRecyclerView     = findViewById(R.id.tasksRecyclerView);
        swipeRefreshLayout    = findViewById(R.id.swipeRefreshLayout);
        emptyStateLayout      = findViewById(R.id.emptyStateLayout);
        emptyTextView         = findViewById(R.id.emptyTextView);
        searchEditText        = findViewById(R.id.searchEditText);
        statusSpinner         = findViewById(R.id.statusSpinner);
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner);
        sortSpinner           = findViewById(R.id.sortSpinner);

        taskAdapter = new TaskRecyclerAdapter(filteredList, new TaskRecyclerAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(SupportTask task) {
                Intent intent = new Intent(ViewTasksActivity.this, TaskDetailActivity.class);
                intent.putExtra("taskId", task.getId());
                startActivity(intent);
            }
            @Override
            public void onTaskLongClick(SupportTask task) {
                showDeleteDialog(task);
            }
        });
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskAdapter);

        // Status filter
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(
                this, R.array.status_array, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);

        // Category filter — prepend "Все категории"
        String[] rawCategories = getResources().getStringArray(R.array.category_array);
        String[] categoryOptions = new String[rawCategories.length + 1];
        categoryOptions[0] = "Все категории";
        System.arraycopy(rawCategories, 0, categoryOptions, 1, rawCategories.length);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categoryOptions);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(categoryAdapter);

        // Sort
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(
                this, R.array.sort_array, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { applyFilters(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        categoryFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { applyFilters(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                currentSort = pos;
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                currentSearch = s.toString().trim().toLowerCase();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(() -> swipeRefreshLayout.setRefreshing(false));

        tasksReference = FirebaseDatabase.getInstance().getReference("tasks");
        loadTasks();
    }

    private void loadTasks() {
        tasksListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                taskList.clear();
                for (DataSnapshot snap : dataSnapshot.getChildren()) {
                    SupportTask task = snap.getValue(SupportTask.class);
                    if (task != null) taskList.add(task);
                }
                applyFilters();
                swipeRefreshLayout.setRefreshing(false);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewTasksActivity.this, "Ошибка загрузки заявок", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        };
        tasksReference.addValueEventListener(tasksListener);
    }

    private void applyFilters() {
        String status = statusSpinner.getSelectedItem() != null
                ? statusSpinner.getSelectedItem().toString() : "All";
        String category = categoryFilterSpinner.getSelectedItem() != null
                ? categoryFilterSpinner.getSelectedItem().toString() : "Все категории";

        filteredList.clear();
        for (SupportTask task : taskList) {
            boolean matchStatus   = status.equals("All") || task.getStatus().equals(status);
            boolean matchCategory = category.equals("Все категории") || task.getCategory().equals(category);
            boolean matchSearch   = currentSearch.isEmpty()
                    || task.getTitle().toLowerCase().contains(currentSearch)
                    || task.getDescription().toLowerCase().contains(currentSearch)
                    || task.getRoomNumber().toLowerCase().contains(currentSearch);
            if (matchStatus && matchCategory && matchSearch) filteredList.add(task);
        }

        switch (currentSort) {
            case SORT_PRIORITY:
                Collections.sort(filteredList, (a, b) ->
                        priorityWeight(b.getPriority()) - priorityWeight(a.getPriority()));
                break;
            case SORT_DATE_ASC:
                Collections.sort(filteredList, (a, b) ->
                        Long.compare(a.getTimestamp(), b.getTimestamp()));
                break;
            case SORT_TITLE:
                Collections.sort(filteredList, (a, b) ->
                        a.getTitle().compareToIgnoreCase(b.getTitle()));
                break;
            default:
                Collections.sort(filteredList, (a, b) ->
                        Long.compare(b.getTimestamp(), a.getTimestamp()));
                break;
        }

        taskAdapter.notifyDataSetChanged();
        boolean empty = filteredList.isEmpty();
        tasksRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyStateLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
        boolean isFiltered = !currentSearch.isEmpty()
                || !status.equals("All")
                || !category.equals("Все категории");
        emptyTextView.setText(isFiltered
                ? "Ничего не найдено по вашему запросу"
                : "Создайте первую заявку через главный экран");
    }

    private int priorityWeight(String p) {
        switch (p) {
            case "Критический": return 4;
            case "Высокий":     return 3;
            case "Средний":     return 2;
            default:            return 1;
        }
    }

    private void showDeleteDialog(SupportTask task) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить заявку")
                .setMessage("Удалить заявку «" + task.getTitle() + "»?")
                .setPositiveButton("Удалить", (d, w) -> {
                    tasksReference.child(task.getId()).removeValue();
                    Toast.makeText(this, "Заявка удалена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tasksListener != null) tasksReference.removeEventListener(tasksListener);
    }
}
