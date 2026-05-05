package com.example.supportapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.supportapplication.models.ChecklistItem;
import com.example.supportapplication.models.SupportTask;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private Spinner taskSpinner;
    private List<SupportTask> completedTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        taskSpinner = findViewById(R.id.taskSpinner);
        databaseReference = FirebaseDatabase.getInstance().getReference("tasks");
        completedTasks = new ArrayList<>();

        loadCompletedTasks();

        Button saveReportButton = findViewById(R.id.saveReportButton);
        saveReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveReportForSelectedTask();
            }
        });
    }

    private void loadCompletedTasks() {
        databaseReference.orderByChild("status").equalTo("Completed").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                completedTasks.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        SupportTask task = snapshot.getValue(SupportTask.class);
                        if (task != null && "Completed".equals(task.getStatus())) {
                            completedTasks.add(task);
                        }
                    }
                    updateTaskSpinner();
                } else {
                    Toast.makeText(ReportActivity.this, "Выполненных задач не найдено", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ReportActivity.this, "Database error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTaskSpinner() {
        List<String> taskTitles = new ArrayList<>();
        for (SupportTask task : completedTasks) {
            taskTitles.add(task.getTitle());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, taskTitles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        taskSpinner.setAdapter(adapter);
    }

    private void saveReportForSelectedTask() {
        int selectedPosition = taskSpinner.getSelectedItemPosition();
        if (selectedPosition != -1) {
            SupportTask selectedTask = completedTasks.get(selectedPosition);
            if ("Completed".equals(selectedTask.getStatus())) {
                try {
                    createPdf("report_" + selectedTask.getId() + ".pdf", selectedTask);
                    Toast.makeText(ReportActivity.this, "Отчет успешно сохранен.", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(ReportActivity.this, "Ошибка сохранения отчета", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Only completed tasks can be reported", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(ReportActivity.this, "Задача не выбрана", Toast.LENGTH_SHORT).show();
        }
    }

    private void createPdf(String dest, SupportTask task) throws IOException {
        File file = new File(getExternalFilesDir(null), dest);

        PdfWriter writer = new PdfWriter(file);

        PdfDocument pdf = new PdfDocument(writer);

        Document document = new Document(pdf);

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(new Date(task.getTimestamp()));

        document.add(new Paragraph("ID заявки: " + task.getId()));
        document.add(new Paragraph("Название: " + task.getTitle()));
        document.add(new Paragraph("Описание: " + task.getDescription()));
        document.add(new Paragraph("Номер аудитории: " + task.getRoomNumber()));
        document.add(new Paragraph("Статус: " + task.getStatus()));
        document.add(new Paragraph("Дата создания: " + formattedDate));

        document.close();
    }
}
