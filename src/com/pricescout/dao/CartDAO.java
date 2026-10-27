package com.pricescout.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.pricescout.model.CartItem;

/**
 * Cart data lives in the database (cart + cart_item), not the browser.
 * Every update/delete joins through cart.user_id so users can only touch their own items.
 */
public class CartDAO {

    public static final int MAX_QUANTITY = 99;

    private static final String CATEGORY_CASE =
        "CASE WHEN EXISTS (SELECT 1 FROM electronics e WHERE e.product_id = p.product_id) THEN 'Electronics' " +
        "     WHEN EXISTS (SELECT 1 FROM household h WHERE h.product_id = p.product_id) THEN 'Household' " +
        "     WHEN EXISTS (SELECT 1 FROM books b WHERE b.product_id = p.product_id) THEN 'Books' " +
        "     ELSE 'Other' END";

    /** Cart is 1:1 with users; create it on demand if a user somehow has none. */
    int getOrCreateCartId(Connection conn, int userId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT cart_id FROM cart WHERE user_id = ?")) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO cart (user_id) VALUES (?)",
                                                            PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /**
     * Adds a product from one store (e.g. "iPhone 15 from Best Buy") to the cart.
     * Adding the same listing again increases its quantity.
     * Returns a label like "iPhone 15 from Best Buy", or null if that listing doesn't exist.
     */
    public String addItem(int userId, int productId, String storeName, int quantity) throws SQLException {
        String findListing =
            "SELECT pl.listing_id, p.name, s.store_name FROM price_listing pl " +
            "JOIN products p ON p.product_id = pl.product_id " +
            "JOIN stores s ON s.store_id = pl.store_id " +
            "WHERE pl.product_id = ? AND s.store_name = ?";
        String upsert =
            "INSERT INTO cart_item (cart_id, listing_id, quantity) VALUES (?, ?, ?) AS new " +
            "ON DUPLICATE KEY UPDATE quantity = LEAST(cart_item.quantity + new.quantity, " + MAX_QUANTITY + ")";

        try (Connection conn = DBConnection.getConnection()) {
            int listingId;
            String label;
            try (PreparedStatement stmt = conn.prepareStatement(findListing)) {
                stmt.setInt(1, productId);
                stmt.setString(2, storeName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) return null;
                    listingId = rs.getInt(1);
                    label = rs.getString(2) + " from " + rs.getString(3);
                }
            }
            int cartId = getOrCreateCartId(conn, userId);
            try (PreparedStatement stmt = conn.prepareStatement(upsert)) {
                stmt.setInt(1, cartId);
                stmt.setInt(2, listingId);
                stmt.setInt(3, quantity);
                stmt.executeUpdate();
            }
            return label;
        }
    }

    public boolean updateQuantity(int userId, int itemId, int quantity) throws SQLException {
        String sql = "UPDATE cart_item ci JOIN cart c ON c.cart_id = ci.cart_id " +
                     "SET ci.quantity = ? WHERE ci.cart_item_id = ? AND c.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, itemId);
            stmt.setInt(3, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean removeItem(int userId, int itemId) throws SQLException {
        String sql = "DELETE ci FROM cart_item ci JOIN cart c ON c.cart_id = ci.cart_id " +
                     "WHERE ci.cart_item_id = ? AND c.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    /** Total quantity across all items, for the header cart badge. */
    public int countItems(int userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(ci.quantity), 0) FROM cart c " +
                     "JOIN cart_item ci ON ci.cart_id = c.cart_id WHERE c.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Cart contents, sorted alphabetically by product name, with the cheapest store for each product. */
    public List<CartItem> getItems(int userId) throws SQLException {
        String sql =
            "SELECT ci.cart_item_id, ci.quantity, pl.price, p.product_id, p.name, s.store_name, " +
            "       " + CATEGORY_CASE + " AS category, " +
            "       (SELECT MIN(pl2.price) FROM price_listing pl2 WHERE pl2.product_id = p.product_id) AS best_price, " +
            "       (SELECT s2.store_name FROM price_listing pl2 JOIN stores s2 ON s2.store_id = pl2.store_id " +
            "         WHERE pl2.product_id = p.product_id ORDER BY pl2.price, s2.store_name LIMIT 1) AS best_store " +
            "FROM cart c " +
            "JOIN cart_item ci ON ci.cart_id = c.cart_id " +
            "JOIN price_listing pl ON pl.listing_id = ci.listing_id " +
            "JOIN products p ON p.product_id = pl.product_id " +
            "JOIN stores s ON s.store_id = pl.store_id " +
            "WHERE c.user_id = ? " +
            "ORDER BY p.name ASC";

        List<CartItem> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new CartItem(
                        rs.getInt("cart_item_id"), 
                        rs.getInt("product_id"), 
                        rs.getString("name"),
                        rs.getString("category"), 
                        rs.getString("store_name"), 
                        rs.getBigDecimal("price"),
                        rs.getInt("quantity"), 
                        rs.getBigDecimal("best_price"), 
                        rs.getString("best_store")
                    ));
                }
            }
        }
        return items;
    }
}
