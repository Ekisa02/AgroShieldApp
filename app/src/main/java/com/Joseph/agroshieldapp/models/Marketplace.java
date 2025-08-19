package com.Joseph.agroshieldapp.models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Marketplace {
    private String id;
    private String name;
    private String price;
    private String description;
    private String healthInfo;
    private String location;
    private String phone;
    private String whatsapp;
    private String imageBase64;
    private String farmerId;
    private @ServerTimestamp Date timestamp;

    // Empty constructor needed for Firestore
    public Marketplace() {}

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getHealthInfo() { return healthInfo; }
    public void setHealthInfo(String healthInfo) { this.healthInfo = healthInfo; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    // Standard WhatsApp getter
    public String getWhatsapp() {
        return whatsapp;
    }

    // WhatsApp setter
    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    // Fluent interface method for WhatsApp-related operations
    public Marketplace withWhatsapp() {
        return this;
    }

    // Helper to get WhatsApp number
    public String getWhatsappNumber() {
        return whatsapp;
    }

    // Helper method to check if WhatsApp is available
    public boolean hasWhatsapp() {
        return whatsapp != null && !whatsapp.isEmpty();
    }

    // Image helper methods
    public Bitmap getImageBitmap() {
        try {
            if (imageBase64 == null || imageBase64.isEmpty()) {
                return null;
            }

            // Handle potential data URI prefix
            String base64Data = imageBase64;
            if (base64Data.contains(",")) {
                base64Data = base64Data.split(",")[1];
            }

            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    // Formatted price display
    public String getFormattedPrice() {
        try {
            double amount = Double.parseDouble(price);
            return String.format("KSh %,.2f", amount);
        } catch (NumberFormatException e) {
            return "KSh " + price;
        }
    }

    // Formatted location display
    public String getFormattedLocation() {
        return location != null && !location.isEmpty() ? location : "Location not specified";
    }

    // Formatted health info display
    public String getFormattedHealthInfo() {
        return healthInfo != null && !healthInfo.isEmpty() ? healthInfo : "No health info provided";
    }
}