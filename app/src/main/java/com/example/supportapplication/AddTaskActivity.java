package com.example.supportapplication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.supportapplication.models.SupportTask;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddTaskActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 1;
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    private EditText titleEditText, descriptionEditText, roomNumberEditText;
    private Spinner prioritySpinner, categorySpinner;
    private Button attachFileButton, dueDateButton, saveTaskButton;
    private Uri fileUri;
    private long selectedDueDate = 0;

    private DatabaseReference tasksDatabase;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        titleEditText       = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        roomNumberEditText  = findViewById(R.id.roomNumberEditText);
        prioritySpinner     = findViewById(R.id.prioritySpinner);
        categorySpinner     = findViewById(R.id.categorySpinner);
        attachFileButton    = findViewById(R.id.attachFileButton);
        dueDateButton       = findViewById(R.id.dueDateButton);
        saveTaskButton      = findViewById(R.id.saveTaskButton);

        tasksDatabase    = FirebaseDatabase.getInstance().getReference("tasks");
        storageReference = FirebaseStorage.getInstance().getReference("uploads");

        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(
                this, R.array.priority_array, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
        prioritySpinner.setSelection(1); // "Средний" по умолчанию

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                this, R.array.category_array, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
        categorySpinner.setSelection(4); // "Другое" по умолчанию

        dueDateButton.setOnClickListener(v -> showDatePicker());
        attachFileButton.setOnClickListener(v -> openFileChooser());
        saveTaskButton.setOnClickListener(v -> saveTask());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            cal.set(year, month, day, 23, 59, 59);
            selectedDueDate = cal.getTimeInMillis();
            dueDateButton.setText("Срок: " + DATE_FORMAT.format(new Date(selectedDueDate)));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            fileUri = data.getData();
            attachFileButton.setText("Файл прикреплён");
        }
    }

    private void saveTask() {
        final String title       = titleEditText.getText().toString().trim();
        final String description = descriptionEditText.getText().toString().trim();
        final String roomNumber  = roomNumberEditText.getText().toString().trim();
        final String priority    = prioritySpinner.getSelectedItem().toString();
        final String category    = categorySpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(roomNumber)) {
            Toast.makeText(this, "Все поля обязательны для заполнения", Toast.LENGTH_SHORT).show();
            return;
        }

        saveTaskButton.setEnabled(false);
        saveTaskButton.setText("Сохранение…");

        final String taskId = tasksDatabase.push().getKey();
        final SupportTask newTask = new SupportTask(
                taskId, title, description, roomNumber,
                "Pending", priority, System.currentTimeMillis(), null);
        newTask.setCategory(category);
        newTask.setDueDate(selectedDueDate);

        if (fileUri != null) {
            final StorageReference fileRef =
                    storageReference.child(taskId + "_" + System.currentTimeMillis());
            fileRef.putFile(fileUri)
                    .addOnSuccessListener(snap ->
                            fileRef.getDownloadUrl().addOnCompleteListener(urlTask -> {
                                if (urlTask.isSuccessful()) {
                                    newTask.setFileUrl(urlTask.getResult().toString());
                                    saveToDatabase(taskId, newTask);
                                } else {
                                    resetSaveButton();
                                    Toast.makeText(this, "Ошибка получения URL файла", Toast.LENGTH_SHORT).show();
                                }
                            }))
                    .addOnFailureListener(e -> {
                        resetSaveButton();
                        Toast.makeText(this, "Ошибка загрузки файла", Toast.LENGTH_SHORT).show();
                    });
        } else {
            saveToDatabase(taskId, newTask);
        }
    }

    private void saveToDatabase(String taskId, SupportTask task) {
        tasksDatabase.child(taskId).setValue(task).addOnCompleteListener(t -> {
            resetSaveButton();
            if (t.isSuccessful()) {
                Toast.makeText(this, "Заявка создана", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Ошибка сохранения заявки", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetSaveButton() {
        saveTaskButton.setEnabled(true);
        saveTaskButton.setText("Создать заявку");
    }
}
