package com.Joseph.agroshieldapp.models;

public class Product {
    private String name;
    private String price;
    private String seller;
    private String imageUrl;
    private String location;
    private String category;

    public Product(String name, String price, String seller,
                   String imageUrl, String location, String category) {
        this.name = name;
        this.price = price;
        this.seller = seller;
        this.imageUrl = imageUrl;
        this.location = location;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public String getSeller() {
        return seller;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLocation() {
        return location;
    }

    public String getCategory() {
        return category;
    }
}