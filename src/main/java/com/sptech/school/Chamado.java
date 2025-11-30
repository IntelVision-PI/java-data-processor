package com.sptech.school;

public class Chamado {
    private String id;
    private String key;
    private String created;
    private String status;
    private String labels;
    private String description;

    public Chamado(String id, String key, String created, String status, String labels, String description) {
        this.id = id;
        this.key = key;
        this.created = created;
        this.status = status;
        this.labels = labels;
        this.description = description;
    }

    public String getId() { return id; }
    public String getKey() { return key; }
    public String getCreated() { return created; }
    public String getStatus() { return status; }
    public String getLabels() { return labels; }
    public String getDescription() { return description; }
}
