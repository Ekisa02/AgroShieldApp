package com.Joseph.agroshieldapp.models;

public class Crop {
    private String name;
    private String disease;
    private int severity;
    private String imageUrl;

    public Crop(String name, String disease, int severity, String imageUrl) {
        this.name = name;
        this.disease = disease;
        this.severity = severity;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public String getDisease() { return disease; }
    public int getSeverity() { return severity; }
    public String getImageUrl() { return imageUrl; }
}
