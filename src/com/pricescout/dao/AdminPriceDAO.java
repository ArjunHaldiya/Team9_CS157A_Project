package com.pricescout.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.pricescout.model.AdminListing;

/**
 * Admin price management (FR10). Every change that touches more than one table runs in a single
 * transaction, so a failure part-way through never leaves prices, history and alerts out of step:
 *
 *   change a price  -> UPDATE price_listing, INSERT price_history (the price being replaced),
 *                      UPDATE price_alert (flag alerts whose target is now met)
 *   add a listing   -> INSERT price_listing, UPDATE price_alert
 *   delete a listing-> DELETE cart_item, DELETE price_history, DELETE price_listing
 *
 * price_history holds the prices a listing used to have (each stamped with when it took effect);
 * the listing's current price lives in price_listing. That matches the seed data and history.jsp.
 */
public class AdminPriceDAO {

    /** Result of a price change. */
    public static class PriceChange {
        public final String productName;
        public final String storeName;
        public final BigDecimal oldPrice;
        public final BigDecimal newPrice;
        public final int alertsTriggered;

        PriceChange(String productName, String storeName, BigDecimal oldPrice, BigDecimal newPrice, int alertsTriggered) {
            this.productName = productName;
            this.storeName = storeName;
            this.oldPrice = oldPrice;
            this.newPrice = newPrice;
            this.alertsTriggered = alertsTriggered;
        }

        public boolean isChanged() { return oldPrice.compareTo(newPrice) != 0; }
    }

    /** Result of removing a listing. */
    public static class Removal {
        public final String productName;
        public final String storeName;
        public final int cartItemsRemoved;
        public final int historyRowsRemoved;

        Removal(String productName, String storeName, int cartItemsRemoved, int historyRowsRemoved) {
            this.productName = productName;
            this.storeName = storeName;
            this.cartItemsRemoved = cartItemsRemoved;
            this.historyRowsRemoved = historyRowsRemoved;
        }
    }

    /** Every listing (optionally only products whose name contains the text), grouped by product name. */
    public List<AdminListing> listListings(String nameFilter) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT pl.listing_id, p.product_id, p.name, s.store_name, pl.price, pl.last_updated, pl.product_url, " +
            "  CASE WHEN EXISTS (SELECT 1 FROM electronics e WHERE e.product_id = p.product_id) THEN 'Electronics' " +
            "       WHEN EXISTS (SELECT 1 FROM household h WHERE h.product_id = p.product_id) THEN 'Household' " +
            "       WHEN EXISTS (SELECT 1 FROM books b WHERE b.product_id = p.product_id) THEN 'Books' " +
            "       ELSE 'Other' END AS category " +
            "FROM price_listing pl " +
            "JOIN products p ON p.product_id = pl.product_id " +
            "JOIN stores s ON s.store_id = pl.store_id ");
        boolean filter = nameFilter != null && !nameFilter.trim().isEmpty();
        if (filter) sql.append("WHERE p.name LIKE ? ESCAPE '!' ");
        sql.append("ORDER BY p.name, s.store_name");

        List<AdminListing> listings = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            if (filter) {
                String escaped = nameFilter.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
                stmt.setString(1, "%" + escaped + "%");
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    listings.add(new AdminListing(
                        rs.getInt("listing_id"), rs.getInt("product_id"), rs.getString("name"),
                        rs.getString("category"), rs.getString("store_name"), rs.getBigDecimal("price"),
                        rs.getTimestamp("last_updated"), rs.getString("product_url")));
                }
            }
        }
        return listings;
    }

    /** product_id -> name, for the "add a listing" dropdown. */
    public Map<Integer, String> productOptions() throws SQLException {
        return options("SELECT product_id, name FROM products ORDER BY name");
    }

    /** store_id -> name, for the "add a listing" dropdown. */
    public Map<Integer, String> storeOptions() throws SQLException {
        return options("SELECT store_id, store_name FROM stores ORDER BY store_name");
    }

    private Map<Integer, String> options(String sql) throws SQLException {
        Map<Integer, String> map = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) map.put(rs.getInt(1), rs.getString(2));
        }
        return map;
    }

    /**
     * Changes a listing's price in one transaction. The row is locked (FOR UPDATE) while we read
     * the old price, so two admins editing at once can't both record the same "old" price.
     * Returns null if the listing doesn't exist. If the price is unchanged nothing is written.
     */
    public PriceChange updatePrice(int listingId, BigDecimal newPrice) throws SQLException {
        String lock =
            "SELECT pl.product_id, pl.price, pl.last_updated, p.name, s.store_name " +
            "FROM price_listing pl JOIN products p ON p.product_id = pl.product_id " +
            "JOIN stores s ON s.store_id = pl.store_id WHERE pl.listing_id = ? FOR UPDATE";
        String insertHistory = "INSERT INTO price_history (listing_id, price, recorded_at) VALUES (?, ?, ?)";
        String updatePrice = "UPDATE price_listing SET price = ? WHERE listing_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int productId;
                BigDecimal oldPrice;
                Timestamp oldSince;
                String productName, storeName;
                try (PreparedStatement stmt = conn.prepareStatement(lock)) {
                    stmt.setInt(1, listingId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return null;
                        }
                        productId = rs.getInt("product_id");
                        oldPrice = rs.getBigDecimal("price");
                        oldSince = rs.getTimestamp("last_updated");
                        productName = rs.getString("name");
                        storeName = rs.getString("store_name");
                    }
                }
                if (oldPrice.compareTo(newPrice) == 0) {
                    conn.rollback();
                    return new PriceChange(productName, storeName, oldPrice, newPrice, 0);
                }
                try (PreparedStatement stmt = conn.prepareStatement(insertHistory)) {
                    stmt.setInt(1, listingId);
                    stmt.setBigDecimal(2, oldPrice);
                    stmt.setTimestamp(3, oldSince != null ? oldSince : new Timestamp(System.currentTimeMillis()));
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement(updatePrice)) {
                    stmt.setBigDecimal(1, newPrice);
                    stmt.setInt(2, listingId);
                    stmt.executeUpdate();
                }
                int triggered = triggerAlerts(conn, productId);
                conn.commit();
                return new PriceChange(productName, storeName, oldPrice, newPrice, triggered);
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Adds a store's listing of a product. Throws SQLException with error code 1062 if that store
     * already lists the product (UNIQUE product_id + store_id), or 1452 if the product/store is unknown.
     * Returns how many customer alerts the new (possibly cheapest) price triggered.
     */
    public int addListing(int productId, int storeId, BigDecimal price, String productUrl) throws SQLException {
        String insert = "INSERT INTO price_listing (product_id, store_id, price, product_url) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(insert)) {
                    stmt.setInt(1, productId);
                    stmt.setInt(2, storeId);
                    stmt.setBigDecimal(3, price);
                    stmt.setString(4, productUrl);
                    stmt.executeUpdate();
                }
                int triggered = triggerAlerts(conn, productId);
                conn.commit();
                return triggered;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Deletes a listing. Rows that point at it go first (cart items, then history) because of the
     * foreign keys; doing it all in one transaction means it is either fully removed or not at all.
     * Returns null if the listing doesn't exist.
     */
    public Removal deleteListing(int listingId) throws SQLException {
        String info =
            "SELECT p.name, s.store_name FROM price_listing pl " +
            "JOIN products p ON p.product_id = pl.product_id JOIN stores s ON s.store_id = pl.store_id " +
            "WHERE pl.listing_id = ? FOR UPDATE";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String productName, storeName;
                try (PreparedStatement stmt = conn.prepareStatement(info)) {
                    stmt.setInt(1, listingId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return null;
                        }
                        productName = rs.getString(1);
                        storeName = rs.getString(2);
                    }
                }
                int carts = deleteWhere(conn, "DELETE FROM cart_item WHERE listing_id = ?", listingId);
                int history = deleteWhere(conn, "DELETE FROM price_history WHERE listing_id = ?", listingId);
                deleteWhere(conn, "DELETE FROM price_listing WHERE listing_id = ?", listingId);
                conn.commit();
                return new Removal(productName, storeName, carts, history);
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Flags every not-yet-triggered alert on the product whose target is at or above the cheapest
     * current price. Runs inside the caller's transaction, so it sees the price change just made.
     */
    private static int triggerAlerts(Connection conn, int productId) throws SQLException {
        String sql =
            "UPDATE price_alert SET triggered = TRUE " +
            "WHERE product_id = ? AND triggered = FALSE " +
            "AND target_price >= (SELECT MIN(price) FROM price_listing WHERE product_id = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setInt(2, productId);
            return stmt.executeUpdate();
        }
    }

    private static int deleteWhere(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate();
        }
    }
}
