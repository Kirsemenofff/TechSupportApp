package com.example.supportapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.supportapplication.adapters.ChecklistAdapter;
import com.example.supportapplication.adapters.StatusHistoryAdapter;
import com.example.supportapplication.models.ChecklistItem;
import com.example.supportapplication.models.StatusChange;
import com.example.supportapplication.models.SupportTask;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TaskDetailActivity extends AppCompatActivity {

    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView roomNumberTextView;
    private Spinner statusSpinner;
    private Button changeStatusButton;
    private Button viewFileButton;
    private Button chatButton;
    private Button viewHistoryButton;
    private Button editTaskButton;
    private ListView checklistListView;
    private Button saveChecklistButton;

    private DatabaseReference taskReference;
    private DatabaseReference statusHistoryReference;
    private ValueEventListener taskListener;
    private ValueEventListener statusHistoryListener;
    private String taskId;
    private SupportTask currentTask;
    private ChecklistAdapter checklistAdapter;
    private List<ChecklistItem> checklist;
    private List<StatusChange> statusChangeList;
    private StatusHistoryAdapter statusHistoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        titleTextView = findViewById(R.id.titleTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        roomNumberTextView = findViewById(R.id.roomNumberTextView);
        statusSpinner = findViewById(R.id.statusSpinner);
        changeStatusButton = findViewById(R.id.changeStatusButton);
        viewFileButton = findViewById(R.id.viewFileButton);
        chatButton = findViewById(R.id.chatButton);
        viewHistoryButton = findViewById(R.id.viewHistoryButton);
        editTaskButton = findViewById(R.id.editTaskButton);
        checklistListView = findViewById(R.id.checklistListView);
        saveChecklistButton = findViewById(R.id.saveChecklistButton);

        taskId = getIntent().getStringExtra("taskId");
        if (taskId == null || taskId.isEmpty()) {
            Toast.makeText(this, "Неверный ID заявки", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        taskReference = FirebaseDatabase.getInstance().getReference("tasks").child(taskId);
        statusHistoryReference = FirebaseDatabase.getInstance().getReference("statusHistory").child(taskId);

        checklist = new ArrayList<>();
        checklistAdapter = new ChecklistAdapter(this, checklist);
        checklistListView.setAdapter(checklistAdapter);

        statusChangeList = new ArrayList<>();
        statusHistoryAdapter = new StatusHistoryAdapter(this, statusChangeList);

        loadTaskDetails();
        loadStatusHistory();

        List<String> statusOptions = Arrays.asList("Pending", "Completed", "In Progress");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        changeStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeTaskStatus();
            }
        });

        viewFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewFile();
            }
        });

        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TaskDetailActivity.this, ChatActivity.class);
                intent.putExtra("taskId", taskId);
                startActivity(intent);
            }
        });


        viewHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TaskDetailActivity.this, StatusHistoryActivity.class);
                intent.putExtra("taskId", taskId);
                startActivity(intent);
            }
        });

        editTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TaskDetailActivity.this, EditTaskActivity.class);
                intent.putExtra("taskId", taskId);
                startActivity(intent);
            }
        });

        saveChecklistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChecklist();
            }
        });
    }

    private void loadTaskDetails() {
        taskListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentTask = dataSnapshot.getValue(SupportTask.class);
                if (currentTask != null) {
                    titleTextView.setText(currentTask.getTitle());
                    descriptionTextView.setText(currentTask.getDescription());
                    roomNumberTextView.setText(currentTask.getRoomNumber());
                    statusSpinner.setSelection(((ArrayAdapter<String>) statusSpinner.getAdapter()).getPosition(currentTask.getStatus()));

                    checklist.clear();
                    if (currentTask.getChecklist() != null) {
                        checklist.addAll(currentTask.getChecklist());
                    } else {
                        checklist.add(new ChecklistItem("Внешний вид компьютера в норме", false));
                        checklist.add(new ChecklistItem("Все кабели подключены", false));
                        checklist.add(new ChecklistItem("Программное обеспечение обновлено", false));
                        checklist.add(new ChecklistItem("Выполнено резервное копирование", false));
                        checklist.add(new ChecklistItem("Антивирус работает", false));
                        checklist.add(new ChecklistItem("Сетевое соединение стабильно", false));
                        currentTask.setChecklist(checklist);
                        taskReference.setValue(currentTask);
                    }
                    checklistAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(TaskDetailActivity.this, "Заявка не найдена", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TaskDetailActivity.this, "Ошибка загрузки заявки", Toast.LENGTH_SHORT).show();
            }
        };
        taskReference.addValueEventListener(taskListener);
    }

    private void loadStatusHistory() {
        statusHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                statusChangeList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    StatusChange statusChange = snapshot.getValue(StatusChange.class);
                    statusChangeList.add(statusChange);
                }
                statusHistoryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TaskDetailActivity.this, "Ошибка загрузки истории статусов", Toast.LENGTH_SHORT).show();
            }
        };
        statusHistoryReference.addValueEventListener(statusHistoryListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskListener != null) taskReference.removeEventListener(taskListener);
        if (statusHistoryListener != null) statusHistoryReference.removeEventListener(statusHistoryListener);
    }

    private void changeTaskStatus() {
        final String newStatus = statusSpinner.getSelectedItem().toString();
        currentTask.setStatus(newStatus);
        taskReference.setValue(currentTask).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser() != null
                            ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                            : "unknown";
                    StatusChange statusChange = new StatusChange(newStatus, currentUserEmail, System.currentTimeMillis());
                    statusHistoryReference.push().setValue(statusChange);
                    Toast.makeText(TaskDetailActivity.this, "Статус изменён на " + newStatus, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(TaskDetailActivity.this, "Ошибка изменения статуса", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveChecklist() {
        currentTask.setChecklist(checklist);
        taskReference.setValue(currentTask).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(TaskDetailActivity.this, "Чек-лист сохранён", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(TaskDetailActivity.this, "Ошибка сохранения чек-листа", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void viewFile() {
        String fileUrl = currentTask.getFileUrl();
        if (fileUrl != null && !fileUrl.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl));
            startActivity(intent);
        } else {
            Toast.makeText(this, "К заявке не прикреплён файл", Toast.LENGTH_SHORT).show();
        }
    }
}
