package com.pricescout.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.pricescout.model.PriceAlert;

/**
 * Price drop alerts (FR7), stored in price_alert. A customer has at most one alert per product
 * (UNIQUE user_id + product_id), so setting a target again updates the existing row.
 * Every query is scoped by user_id so customers can only see and change their own alerts.
 */
public class AlertDAO {

    /** Outcome of setting an alert: did the current cheapest price already meet the target? */
    public static class SetResult {
        public final boolean triggered;
        public final BigDecimal lowestPrice; // null if no store lists the product
        public final String lowestStore;

        SetResult(boolean triggered, BigDecimal lowestPrice, String lowestStore) {
            this.triggered = triggered;
            this.lowestPrice = lowestPrice;
            this.lowestStore = lowestStore;
        }
    }

    private static final String SELECT_ALERTS =
        "SELECT pa.alert_id, pa.product_id, p.name, pa.target_price, pa.triggered, " +
        "  (SELECT MIN(pl.price) FROM price_listing pl WHERE pl.product_id = pa.product_id) AS lowest_price, " +
        "  (SELECT s.store_name FROM price_listing pl JOIN stores s ON s.store_id = pl.store_id " +
        "    WHERE pl.product_id = pa.product_id ORDER BY pl.price, s.store_name LIMIT 1) AS lowest_store " +
        "FROM price_alert pa JOIN products p ON p.product_id = pa.product_id " +
        "WHERE pa.user_id = ? ";

    /** All of a customer's alerts: fired ones first, then by product name. */
    public List<PriceAlert> findByUser(int userId) throws SQLException {
        List<PriceAlert> alerts = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALERTS + "ORDER BY pa.triggered DESC, p.name")) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) alerts.add(map(rs));
            }
        }
        return alerts;
    }

    /** The customer's alert on one product, or null if they have none. */
    public PriceAlert find(int userId, int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALERTS + "AND pa.product_id = ?")) {
            stmt.setInt(1, userId);
            stmt.setInt(2, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * INSERTs the alert, or UPDATEs the target if the customer already has one on this product.
     * If some store is already at or below the target, the alert is marked triggered right away
     * instead of waiting for the next admin price change. Returns null if the product doesn't exist.
     */
    public SetResult setAlert(int userId, int productId, BigDecimal target) throws SQLException {
        String exists = "SELECT 1 FROM products WHERE product_id = ?";
        String lowest =
            "SELECT pl.price, s.store_name FROM price_listing pl JOIN stores s ON s.store_id = pl.store_id " +
            "WHERE pl.product_id = ? ORDER BY pl.price, s.store_name LIMIT 1";
        String upsert =
            "INSERT INTO price_alert (user_id, product_id, target_price, triggered) VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE target_price = ?, triggered = ?";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(exists)) {
                stmt.setInt(1, productId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) return null;
                }
            }
            BigDecimal lowestPrice = null;
            String lowestStore = null;
            try (PreparedStatement stmt = conn.prepareStatement(lowest)) {
                stmt.setInt(1, productId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        lowestPrice = rs.getBigDecimal(1);
                        lowestStore = rs.getString(2);
                    }
                }
            }
            boolean triggered = lowestPrice != null && lowestPrice.compareTo(target) <= 0;
            try (PreparedStatement stmt = conn.prepareStatement(upsert)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, productId);
                stmt.setBigDecimal(3, target);
                stmt.setBoolean(4, triggered);
                stmt.setBigDecimal(5, target);
                stmt.setBoolean(6, triggered);
                stmt.executeUpdate();
            }
            return new SetResult(triggered, lowestPrice, lowestStore);
        }
    }

    /** Deletes one of the customer's own alerts. False if it doesn't exist or belongs to someone else. */
    public boolean delete(int userId, int alertId) throws SQLException {
        String sql = "DELETE FROM price_alert WHERE alert_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, alertId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    private static PriceAlert map(ResultSet rs) throws SQLException {
        return new PriceAlert(
            rs.getInt("alert_id"),
            rs.getInt("product_id"),
            rs.getString("name"),
            rs.getBigDecimal("target_price"),
            rs.getBoolean("triggered"),
            rs.getBigDecimal("lowest_price"),
            rs.getString("lowest_store"));
    }
}
