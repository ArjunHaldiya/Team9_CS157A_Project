package com.pricescout.model;

import java.math.BigDecimal;

/** A customer's target price on a product, with where the price stands right now. */
public class PriceAlert {
    private final int alertId;
    private final int productId;
    private final String productName;
    private final BigDecimal targetPrice;
    private final boolean triggered;
    private final BigDecimal lowestPrice; // null if no store lists the product
    private final String lowestStore;

    public PriceAlert(int alertId, int productId, String productName, BigDecimal targetPrice,
                      boolean triggered, BigDecimal lowestPrice, String lowestStore) {
        this.alertId = alertId;
        this.productId = productId;
        this.productName = productName;
        this.targetPrice = targetPrice;
        this.triggered = triggered;
        this.lowestPrice = lowestPrice;
        this.lowestStore = lowestStore;
    }

    public int getAlertId() { return alertId; }
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getTargetPrice() { return targetPrice; }
    /** The stored flag, flipped when a price update (or setting the alert) meets the target. */
    public boolean isTriggered() { return triggered; }
    public BigDecimal getLowestPrice() { return lowestPrice; }
    public String getLowestStore() { return lowestStore; }

    /** True once the alert has fired, or if the cheapest store is at/below the target right now. */
    public boolean isReached() {
        return triggered || (lowestPrice != null && lowestPrice.compareTo(targetPrice) <= 0);
    }
}
