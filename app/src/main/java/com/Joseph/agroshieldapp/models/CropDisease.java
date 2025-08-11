package com.Joseph.agroshieldapp.models;

public class CropDisease {
    private String name;
    private String description;
    private String imageUrl;
    private String cropType;

    public CropDisease() {
        // Required empty constructor for Firebase
    }

    public CropDisease(String name, String description, String imageUrl, String cropType) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.cropType = cropType;
    }

    // Getters and setters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getCropType() { return cropType; }
}