package com.example.supportapplication.models;

import java.util.List;

public class SupportTask {
    private String id;
    private String title;
    private String description;
    private String roomNumber;
    private String status;
    private String priority;
    private String category;
    private String assignedTo;
    private long timestamp;
    private long dueDate;
    private String fileUrl;
    private List<ChecklistItem> checklist;

    public SupportTask() {}

    public SupportTask(String id, String title, String description, String roomNumber,
                       String status, String priority, long timestamp, String fileUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.roomNumber = roomNumber;
        this.status = status;
        this.priority = priority;
        this.timestamp = timestamp;
        this.fileUrl = fileUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority != null ? priority : "Средний"; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getCategory() { return category != null ? category : ""; }
    public void setCategory(String category) { this.category = category; }

    public String getAssignedTo() { return assignedTo != null ? assignedTo : ""; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public List<ChecklistItem> getChecklist() { return checklist; }
    public void setChecklist(List<ChecklistItem> checklist) { this.checklist = checklist; }
}
