package com.pricescout.model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents one product row on the homepage, with its price at
 * each participating store (Best Buy, Target, Walmart) attached.
 */
public class Product {

    private int productId;
    private String name;
    private String category;
    private String imageUrl;

    // store name -> price
    private Map<String, BigDecimal> storePrices = new LinkedHashMap<>();

    public Product(int productId, String name, String category, String imageUrl) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.imageUrl = imageUrl;
    }

    public void addStorePrice(String storeName, BigDecimal price) {
        storePrices.put(storeName, price);
    }

    public Map<String, BigDecimal> getStorePrices() {
        return storePrices;
    }

    /** Returns the store name with the lowest price, or null if no prices. */
    public String getLowestPriceStore() {
        String bestStore = null;
        BigDecimal bestPrice = null;
        for (Map.Entry<String, BigDecimal> entry : storePrices.entrySet()) {
            if (bestPrice == null || entry.getValue().compareTo(bestPrice) < 0) {
                bestPrice = entry.getValue();
                bestStore = entry.getKey();
            }
        }
        return bestStore;
    }

    public BigDecimal getLowestPrice() {
        String store = getLowestPriceStore();
        return store == null ? null : storePrices.get(store);
    }

    public int getProductId() { return productId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
}
