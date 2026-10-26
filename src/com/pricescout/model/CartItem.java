package com.pricescout.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** One row of a user's cart: a specific store's listing of a product, plus quantity. */
public class CartItem {
    private final int itemId;
    private final int productId;
    private final String productName;
    private final String category;
    private final String storeName;
    private final BigDecimal price;
    private final int quantity;
    private final Timestamp addedAt;
    private final BigDecimal bestPrice;
    private final String bestStore;

    public CartItem(int itemId, int productId, String productName, String category, String storeName,
                    BigDecimal price, int quantity, Timestamp addedAt, BigDecimal bestPrice, String bestStore) {
        this.itemId = itemId;
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.storeName = storeName;
        this.price = price;
        this.quantity = quantity;
        this.addedAt = addedAt;
        this.bestPrice = bestPrice;
        this.bestStore = bestStore;
    }

    public int getItemId() { return itemId; }
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getCategory() { return category; }
    public String getStoreName() { return storeName; }
    public BigDecimal getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public Timestamp getAddedAt() { return addedAt; }
    public BigDecimal getBestPrice() { return bestPrice; }
    public String getBestStore() { return bestStore; }

    public BigDecimal getLineTotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /** True when another store currently lists this product for less. */
    public boolean isCheaperElsewhere() {
        return bestPrice != null && bestPrice.compareTo(price) < 0;
    }
}
