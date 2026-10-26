package com.pricescout.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.pricescout.model.PricePoint;
import com.pricescout.model.StoreHistory;

public class PriceHistoryDAO {

    /** Returns {name, category} for a product, or null if it doesn't exist. */
    public String[] getProductInfo(int productId) throws SQLException {
        String sql =
            "SELECT p.name, " +
            "  CASE WHEN EXISTS (SELECT 1 FROM electronics e WHERE e.product_id = p.product_id) THEN 'Electronics' " +
            "       WHEN EXISTS (SELECT 1 FROM household h WHERE h.product_id = p.product_id) THEN 'Household' " +
            "       WHEN EXISTS (SELECT 1 FROM books b WHERE b.product_id = p.product_id) THEN 'Books' " +
            "       ELSE 'Other' END AS category " +
            "FROM products p WHERE p.product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? new String[] { rs.getString("name"), rs.getString("category") } : null;
            }
        }
    }

    /** One entry per store that lists the product, each with its price history (oldest first). */
    public List<StoreHistory> getHistory(int productId) throws SQLException {
        String sql =
            "SELECT s.store_name, pl.price AS current_price, pl.last_updated, ph.price AS old_price, ph.recorded_at " +
            "FROM price_listing pl " +
            "JOIN stores s ON s.store_id = pl.store_id " +
            "LEFT JOIN price_history ph ON ph.listing_id = pl.listing_id " +
            "WHERE pl.product_id = ? " +
            "ORDER BY s.store_name, ph.recorded_at";
        Map<String, StoreHistory> byStore = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String store = rs.getString("store_name");
                    StoreHistory h = byStore.get(store);
                    if (h == null) {
                        h = new StoreHistory(store, rs.getBigDecimal("current_price"), rs.getTimestamp("last_updated"));
                        byStore.put(store, h);
                    }
                    if (rs.getBigDecimal("old_price") != null) {
                        h.addPoint(new PricePoint(rs.getTimestamp("recorded_at"), rs.getBigDecimal("old_price")));
                    }
                }
            }
        }
        return new ArrayList<>(byStore.values());
    }
}
