package com.pricescout.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** One recorded price for a store listing at a point in time. */
public class PricePoint {
    private final Timestamp recordedAt;
    private final BigDecimal price;

    public PricePoint(Timestamp recordedAt, BigDecimal price) {
        this.recordedAt = recordedAt;
        this.price = price;
    }

    public Timestamp getRecordedAt() { return recordedAt; }
    public BigDecimal getPrice() { return price; }
}
