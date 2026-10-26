package com.pricescout.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** A store's current price for a product plus its past prices (from price_history). */
public class StoreHistory {
    private final String storeName;
    private final BigDecimal currentPrice;
    private final Timestamp lastUpdated;
    private final List<PricePoint> points = new ArrayList<>();

    public StoreHistory(String storeName, BigDecimal currentPrice, Timestamp lastUpdated) {
        this.storeName = storeName;
        this.currentPrice = currentPrice;
        this.lastUpdated = lastUpdated;
    }

    public void addPoint(PricePoint point) { points.add(point); }

    public String getStoreName() { return storeName; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public Timestamp getLastUpdated() { return lastUpdated; }
    public List<PricePoint> getPoints() { return points; }

    /** Lowest price ever seen, including the current one. */
    public BigDecimal getLowestPrice() {
        BigDecimal low = currentPrice;
        for (PricePoint p : points) {
            if (p.getPrice().compareTo(low) < 0) low = p.getPrice();
        }
        return low;
    }

    /** Percent change from the oldest recorded price to now, or null with no history. */
    public BigDecimal getChangePercent() {
        if (points.isEmpty()) return null;
        BigDecimal first = points.get(0).getPrice();
        if (first.signum() == 0) return null;
        return currentPrice.subtract(first).multiply(BigDecimal.valueOf(100)).divide(first, 1, RoundingMode.HALF_UP);
    }
}
