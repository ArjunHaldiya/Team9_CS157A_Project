package com.pricescout.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything the product detail page shows: the product, its type-specific facts and its store listings. */
public class ProductDetail {
    private final int productId;
    private final String name;
    private final String description;
    private final String imageUrl;
    private final String category;
    private final Map<String, String> attributes = new LinkedHashMap<>(); // label -> value
    private final List<StoreListing> listings = new ArrayList<>();         // cheapest first

    public ProductDetail(int productId, String name, String description, String imageUrl, String category) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public int getProductId() { return productId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getCategory() { return category; }
    public Map<String, String> getAttributes() { return attributes; }
    public List<StoreListing> getListings() { return listings; }

    /** The cheapest listing, or null if no store carries the product. */
    public StoreListing getLowestListing() {
        return listings.isEmpty() ? null : listings.get(0);
    }
}
