package com.Joseph.agroshieldapp.models;

import java.util.List;

public class FAOResponse {
    private List<Disease> data;

    public List<Disease> getData() {
        return data;
    }

    public static class Disease {
        private String diseaseName;
        private String description;
        private String imageUrl;
        private String affectedCrop;

        // Getters and setters
        public String getDiseaseName() { return diseaseName; }
        public String getDescription() { return description; }
        public String getImageUrl() { return imageUrl; }
        public String getAffectedCrop() { return affectedCrop; }
    }
}
