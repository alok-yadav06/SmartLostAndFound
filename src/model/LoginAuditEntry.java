package model;

public class LoginAuditEntry {

    private final int id;
    private final Integer userId;
    private final String username;
    private final String eventType;
    private final String eventTime;
    private final String notes;

    public LoginAuditEntry(int id, Integer userId, String username, String eventType, String eventTime, String notes) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventTime() {
        return eventTime;
    }

    public String getNotes() {
        return notes;
    }
}
