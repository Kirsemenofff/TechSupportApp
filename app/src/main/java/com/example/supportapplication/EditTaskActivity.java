package com.example.supportapplication;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.supportapplication.models.SupportTask;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;

public class EditTaskActivity extends AppCompatActivity {

    private EditText titleEditText, descriptionEditText, roomNumberEditText;
    private Spinner prioritySpinner;
    private Button saveButton;
    private ProgressDialog progressDialog;

    private DatabaseReference taskReference;
    private String taskId;
    private SupportTask currentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        taskId = getIntent().getStringExtra("taskId");
        if (taskId == null || taskId.isEmpty()) {
            Toast.makeText(this, "Неверный ID заявки", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        titleEditText       = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        roomNumberEditText  = findViewById(R.id.roomNumberEditText);
        prioritySpinner     = findViewById(R.id.prioritySpinner);
        saveButton          = findViewById(R.id.saveButton);
        progressDialog      = new ProgressDialog(this);

        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(
                this, R.array.priority_array, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        taskReference = FirebaseDatabase.getInstance().getReference("tasks").child(taskId);
        taskReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentTask = dataSnapshot.getValue(SupportTask.class);
                if (currentTask != null) {
                    titleEditText.setText(currentTask.getTitle());
                    descriptionEditText.setText(currentTask.getDescription());
                    roomNumberEditText.setText(currentTask.getRoomNumber());

                    List<String> priorities = Arrays.asList("Низкий", "Средний", "Высокий", "Критический");
                    int idx = priorities.indexOf(currentTask.getPriority());
                    if (idx >= 0) prioritySpinner.setSelection(idx);
                } else {
                    Toast.makeText(EditTaskActivity.this, "Заявка не найдена", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(EditTaskActivity.this, "Ошибка загрузки заявки", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChanges();
            }
        });
    }

    private void saveChanges() {
        String title       = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String roomNumber  = roomNumberEditText.getText().toString().trim();
        String priority    = prioritySpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(roomNumber)) {
            Toast.makeText(this, "Все поля обязательны для заполнения", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Сохранение…");
        progressDialog.show();

        currentTask.setTitle(title);
        currentTask.setDescription(description);
        currentTask.setRoomNumber(roomNumber);
        currentTask.setPriority(priority);

        taskReference.setValue(currentTask).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    Toast.makeText(EditTaskActivity.this, "Заявка обновлена", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditTaskActivity.this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
