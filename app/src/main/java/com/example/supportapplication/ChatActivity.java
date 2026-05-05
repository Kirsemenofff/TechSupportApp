package com.example.supportapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.supportapplication.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private DatabaseReference messagesReference;
    private ListView messagesListView;
    private EditText messageEditText;
    private Button sendButton;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private String taskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        taskId = getIntent().getStringExtra("taskId");
        if (taskId == null || taskId.isEmpty()) {
            Toast.makeText(this, "Invalid task ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        messagesListView = findViewById(R.id.messagesListView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList);
        messagesListView.setAdapter(messageAdapter);

        messagesReference = FirebaseDatabase.getInstance().getReference("tasks").child(taskId).child("messages");

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        loadMessages();
    }

    private void loadMessages() {
        messagesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                messageList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    messageList.add(message);
                }
                messageAdapter.notifyDataSetChanged();
                messagesListView.setSelection(messageList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();
        if (!messageText.isEmpty()) {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(this, "Необходимо войти в систему", Toast.LENGTH_SHORT).show();
                return;
            }
            String messageId = messagesReference.push().getKey();
            String sender = FirebaseAuth.getInstance().getCurrentUser().getUid();
            long timestamp = System.currentTimeMillis();

            Message message = new Message(messageId, messageText, sender, timestamp);
            messagesReference.child(messageId).setValue(message);

            messageEditText.setText("");
            messagesListView.setSelection(messageAdapter.getCount() - 1);
        }
    }
}
