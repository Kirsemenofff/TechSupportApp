package com.example.supportapplication;

import android.app.ProgressDialog;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class AddTaskActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 1;

    private EditText titleEditText, descriptionEditText, roomNumberEditText;
    private Spinner prioritySpinner;
    private Button attachFileButton, saveTaskButton;
    private Uri fileUri;
    private ProgressDialog progressDialog;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference tasksDatabase;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        roomNumberEditText = findViewById(R.id.roomNumberEditText);
        prioritySpinner = findViewById(R.id.prioritySpinner);
        attachFileButton = findViewById(R.id.attachFileButton);
        saveTaskButton = findViewById(R.id.saveTaskButton);
        progressDialog = new ProgressDialog(this);

        firebaseAuth = FirebaseAuth.getInstance();
        tasksDatabase = FirebaseDatabase.getInstance().getReference("tasks");
        storageReference = FirebaseStorage.getInstance().getReference("uploads");

        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(
                this, R.array.priority_array, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
        prioritySpinner.setSelection(1); // "Средний" по умолчанию

        attachFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        saveTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTask();
            }
        });
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
        final String title = titleEditText.getText().toString().trim();
        final String description = descriptionEditText.getText().toString().trim();
        final String roomNumber = roomNumberEditText.getText().toString().trim();
        final String priority = prioritySpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(roomNumber)) {
            Toast.makeText(this, "Все поля обязательны для заполнения", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Сохранение заявки…");
        progressDialog.show();

        final String taskId = tasksDatabase.push().getKey();
        final SupportTask newTask = new SupportTask(
                taskId, title, description, roomNumber,
                "Pending", priority, System.currentTimeMillis(), null);

        if (fileUri != null) {
            final StorageReference fileReference =
                    storageReference.child(taskId + "_" + System.currentTimeMillis());
            fileReference.putFile(fileUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            fileReference.getDownloadUrl()
                                    .addOnCompleteListener(new OnCompleteListener<Uri>() {
                                        @Override
                                        public void onComplete(@NonNull com.google.android.gms.tasks.Task<Uri> task) {
                                            if (task.isSuccessful()) {
                                                newTask.setFileUrl(task.getResult().toString());
                                                saveTaskToDatabase(taskId, newTask);
                                            } else {
                                                progressDialog.dismiss();
                                                Toast.makeText(AddTaskActivity.this,
                                                        "Ошибка получения URL файла", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(AddTaskActivity.this,
                                    "Ошибка загрузки файла", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            saveTaskToDatabase(taskId, newTask);
        }
    }

    private void saveTaskToDatabase(String taskId, SupportTask task) {
        tasksDatabase.child(taskId).setValue(task)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull com.google.android.gms.tasks.Task<Void> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            Toast.makeText(AddTaskActivity.this, "Заявка создана", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddTaskActivity.this,
                                    "Ошибка сохранения заявки", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
