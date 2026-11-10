package com.pricescout.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** One price_listing row joined with its product and store, for the admin price manager. */
public class AdminListing {
    private final int listingId;
    private final int productId;
    private final String productName;
    private final String category;
    private final String storeName;
    private final BigDecimal price;
    private final Timestamp lastUpdated;
    private final String productUrl;

    public AdminListing(int listingId, int productId, String productName, String category,
                        String storeName, BigDecimal price, Timestamp lastUpdated, String productUrl) {
        this.listingId = listingId;
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.storeName = storeName;
        this.price = price;
        this.lastUpdated = lastUpdated;
        this.productUrl = productUrl;
    }

    public int getListingId() { return listingId; }
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getCategory() { return category; }
    public String getStoreName() { return storeName; }
    public BigDecimal getPrice() { return price; }
    public Timestamp getLastUpdated() { return lastUpdated; }
    public String getProductUrl() { return productUrl; }
}
