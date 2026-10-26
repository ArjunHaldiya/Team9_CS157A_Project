package com.pricescout.model;

import java.math.BigDecimal;

/** A store's listing of a product (one price_listing row). Price is null when the store doesn't carry it. */
public class Listing {
    private final int listingId;
    private final int storeId;
    private final BigDecimal price;
    private final String productUrl;

    public Listing(int listingId, int storeId, BigDecimal price, String productUrl) {
        this.listingId = listingId;
        this.storeId = storeId;
        this.price = price;
        this.productUrl = productUrl;
    }

    public int getListingId() { return listingId; }
    public int getStoreId() { return storeId; }
    public BigDecimal getPrice() { return price; }
    public String getProductUrl() { return productUrl; }
}
