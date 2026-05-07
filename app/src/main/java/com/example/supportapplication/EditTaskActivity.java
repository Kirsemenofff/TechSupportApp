package com.example.supportapplication;

import android.app.DatePickerDialog;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditTaskActivity extends AppCompatActivity {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    private EditText titleEditText, descriptionEditText, roomNumberEditText;
    private Spinner prioritySpinner, categorySpinner;
    private Button dueDateButton, saveButton;

    private DatabaseReference taskReference;
    private String taskId;
    private SupportTask currentTask;
    private long selectedDueDate = 0;

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
        categorySpinner     = findViewById(R.id.categorySpinner);
        dueDateButton       = findViewById(R.id.dueDateButton);
        saveButton          = findViewById(R.id.saveButton);

        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(
                this, R.array.priority_array, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                this, R.array.category_array, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        dueDateButton.setOnClickListener(v -> showDatePicker());

        taskReference = FirebaseDatabase.getInstance().getReference("tasks").child(taskId);
        taskReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentTask = dataSnapshot.getValue(SupportTask.class);
                if (currentTask != null) {
                    populateFields();
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

        saveButton.setOnClickListener(v -> saveChanges());
    }

    private void populateFields() {
        titleEditText.setText(currentTask.getTitle());
        descriptionEditText.setText(currentTask.getDescription());
        roomNumberEditText.setText(currentTask.getRoomNumber());

        List<String> priorities = Arrays.asList("Низкий", "Средний", "Высокий", "Критический");
        int pIdx = priorities.indexOf(currentTask.getPriority());
        if (pIdx >= 0) prioritySpinner.setSelection(pIdx);

        List<String> categories = Arrays.asList("Сеть", "Железо", "ПО", "Принтер", "Другое");
        int cIdx = categories.indexOf(currentTask.getCategory());
        categorySpinner.setSelection(cIdx >= 0 ? cIdx : 4);

        selectedDueDate = currentTask.getDueDate();
        if (selectedDueDate > 0) {
            dueDateButton.setText("Срок: " + DATE_FORMAT.format(new Date(selectedDueDate)));
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        if (selectedDueDate > 0) cal.setTimeInMillis(selectedDueDate);
        new DatePickerDialog(this, (view, year, month, day) -> {
            cal.set(year, month, day, 23, 59, 59);
            selectedDueDate = cal.getTimeInMillis();
            dueDateButton.setText("Срок: " + DATE_FORMAT.format(new Date(selectedDueDate)));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveChanges() {
        String title       = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String roomNumber  = roomNumberEditText.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(roomNumber)) {
            Toast.makeText(this, "Все поля обязательны для заполнения", Toast.LENGTH_SHORT).show();
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText("Сохранение…");

        currentTask.setTitle(title);
        currentTask.setDescription(description);
        currentTask.setRoomNumber(roomNumber);
        currentTask.setPriority(prioritySpinner.getSelectedItem().toString());
        currentTask.setCategory(categorySpinner.getSelectedItem().toString());
        currentTask.setDueDate(selectedDueDate);

        taskReference.setValue(currentTask).addOnCompleteListener(task -> {
            saveButton.setEnabled(true);
            saveButton.setText("Сохранить изменения");
            if (task.isSuccessful()) {
                Toast.makeText(this, "Заявка обновлена", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
