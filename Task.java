import java.io.Serializable;

public class Task implements Serializable {
    private String name;
    private String priority;
    private String status;
    private String deadline;

    public Task(String name, String priority, String status, String deadline) {
        this.name = name;
        this.priority = priority;
        this.status = status;
        this.deadline = deadline;
    }

    public String getName()     { return name; }
    public String getPriority() { return priority; }
    public String getStatus()   { return status; }
    public String getDeadline() { return deadline; }

    public void setName(String name)         { this.name = name; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setStatus(String status)     { this.status = status; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
}