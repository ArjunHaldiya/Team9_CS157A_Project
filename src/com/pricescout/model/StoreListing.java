package com.pricescout.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** One store's current listing of a product (one row of price_listing, plus the store name). */
public class StoreListing {
    private final int listingId;
    private final String storeName;
    private final BigDecimal price;
    private final Timestamp lastUpdated;
    private final String productUrl; // null unless it is a valid http(s) link
    private boolean lowest;

    public StoreListing(int listingId, String storeName, BigDecimal price, Timestamp lastUpdated, String productUrl) {
        this.listingId = listingId;
        this.storeName = storeName;
        this.price = price;
        this.lastUpdated = lastUpdated;
        this.productUrl = productUrl;
    }

    public int getListingId() { return listingId; }
    public String getStoreName() { return storeName; }
    public BigDecimal getPrice() { return price; }
    public Timestamp getLastUpdated() { return lastUpdated; }
    public String getProductUrl() { return productUrl; }
    public boolean isLowest() { return lowest; }
    public void setLowest(boolean lowest) { this.lowest = lowest; }
}
