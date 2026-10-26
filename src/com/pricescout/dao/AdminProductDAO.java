package com.pricescout.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.pricescout.model.Listing;
import com.pricescout.model.Product;
import com.pricescout.model.ProductForm;
import com.pricescout.model.Store;

/**
 * Admin-only product and price management. Multi-table changes run in transactions.
 * Price history is written by the trg_price_listing_history trigger (see database/schema.sql),
 * so any UPDATE to price_listing.price is logged automatically.
 */
public class AdminProductDAO {

    private static final String CATEGORY_CASE =
        "CASE WHEN EXISTS (SELECT 1 FROM electronics e WHERE e.product_id = p.product_id) THEN 'Electronics' " +
        "     WHEN EXISTS (SELECT 1 FROM household h WHERE h.product_id = p.product_id) THEN 'Household' " +
        "     WHEN EXISTS (SELECT 1 FROM books b WHERE b.product_id = p.product_id) THEN 'Books' " +
        "     ELSE 'Other' END";

    public List<Store> listStores() throws SQLException {
        List<Store> stores = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT store_id, store_name FROM stores ORDER BY store_name");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) stores.add(new Store(rs.getInt(1), rs.getString(2)));
        }
        return stores;
    }

    /** Every product, including ones no store carries yet (the storefront hides those). */
    public List<Product> listProducts() throws SQLException {
        String sql =
            "SELECT p.product_id, p.name, p.image_url, " + CATEGORY_CASE + " AS category, s.store_name, pl.price " +
            "FROM products p " +
            "LEFT JOIN price_listing pl ON pl.product_id = p.product_id " +
            "LEFT JOIN stores s ON s.store_id = pl.store_id " +
            "ORDER BY p.name, s.store_name";
        Map<Integer, Product> byId = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("product_id");
                Product p = byId.get(id);
                if (p == null) {
                    p = new Product(id, rs.getString("name"), rs.getString("category"), rs.getString("image_url"));
                    byId.put(id, p);
                }
                if (rs.getString("store_name") != null) p.addStorePrice(rs.getString("store_name"), rs.getBigDecimal("price"));
            }
        }
        return new ArrayList<>(byId.values());
    }

    /** Loads a product with its subclass fields, or null if it doesn't exist. */
    public ProductForm getProduct(int productId) throws SQLException {
        String sql =
            "SELECT p.product_id, p.name, p.description, p.image_url, " + CATEGORY_CASE + " AS category, " +
            "       e.brand AS e_brand, e.model_number, e.warranty_months, " +
            "       h.brand AS h_brand, h.material, h.dimensions, b.author, b.isbn, b.publisher " +
            "FROM products p " +
            "LEFT JOIN electronics e ON e.product_id = p.product_id " +
            "LEFT JOIN household h ON h.product_id = p.product_id " +
            "LEFT JOIN books b ON b.product_id = p.product_id " +
            "WHERE p.product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                ProductForm f = new ProductForm();
                f.setProductId(rs.getInt("product_id"));
                f.setName(nz(rs.getString("name")));
                f.setDescription(nz(rs.getString("description")));
                f.setImageUrl(nz(rs.getString("image_url")));
                f.setCategory(rs.getString("category"));
                f.setBrand(nz("Household".equals(f.getCategory()) ? rs.getString("h_brand") : rs.getString("e_brand")));
                f.setModelNumber(nz(rs.getString("model_number")));
                int w = rs.getInt("warranty_months");
                f.setWarrantyMonths(rs.wasNull() ? null : w);
                f.setMaterial(nz(rs.getString("material")));
                f.setDimensions(nz(rs.getString("dimensions")));
                f.setAuthor(nz(rs.getString("author")));
                f.setIsbn(nz(rs.getString("isbn")));
                f.setPublisher(nz(rs.getString("publisher")));
                return f;
            }
        }
    }

    /** Current listings for a product, keyed by store_id. */
    public Map<Integer, Listing> getListings(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return getListings(conn, productId);
        }
    }

    private Map<Integer, Listing> getListings(Connection conn, int productId) throws SQLException {
        Map<Integer, Listing> map = new HashMap<>();
        String sql = "SELECT listing_id, store_id, price, product_url FROM price_listing WHERE product_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt("store_id"), new Listing(rs.getInt("listing_id"), rs.getInt("store_id"),
                                                              rs.getBigDecimal("price"), rs.getString("product_url")));
                }
            }
        }
        return map;
    }

    /**
     * Creates (productId == 0) or updates a product and its ISA subclass row in one transaction.
     * Returns the product id, or -1 if the product to update no longer exists.
     */
    public int saveProduct(ProductForm f) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int id = f.getProductId();
                if (f.isNew()) {
                    String sql = "INSERT INTO products (name, description, image_url) VALUES (?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        stmt.setString(1, f.getName());
                        stmt.setString(2, blankToNull(f.getDescription()));
                        stmt.setString(3, blankToNull(f.getImageUrl()));
                        stmt.executeUpdate();
                        try (ResultSet keys = stmt.getGeneratedKeys()) {
                            keys.next();
                            id = keys.getInt(1);
                        }
                    }
                } else {
                    String sql = "UPDATE products SET name = ?, description = ?, image_url = ? WHERE product_id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, f.getName());
                        stmt.setString(2, blankToNull(f.getDescription()));
                        stmt.setString(3, blankToNull(f.getImageUrl()));
                        stmt.setInt(4, id);
                        if (stmt.executeUpdate() == 0) {
                            conn.rollback();
                            return -1;
                        }
                    }
                }
                // A product belongs to at most one subclass: clear all, then insert the chosen one.
                for (String table : new String[] {"electronics", "household", "books"}) {
                    try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + table + " WHERE product_id = ?")) {
                        stmt.setInt(1, id);
                        stmt.executeUpdate();
                    }
                }
                switch (f.getCategory()) {
                    case "Electronics":
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "INSERT INTO electronics (product_id, brand, model_number, warranty_months) VALUES (?, ?, ?, ?)")) {
                            stmt.setInt(1, id);
                            stmt.setString(2, blankToNull(f.getBrand()));
                            stmt.setString(3, blankToNull(f.getModelNumber()));
                            if (f.getWarrantyMonths() == null) stmt.setNull(4, Types.INTEGER);
                            else stmt.setInt(4, f.getWarrantyMonths());
                            stmt.executeUpdate();
                        }
                        break;
                    case "Household":
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "INSERT INTO household (product_id, brand, material, dimensions) VALUES (?, ?, ?, ?)")) {
                            stmt.setInt(1, id);
                            stmt.setString(2, blankToNull(f.getBrand()));
                            stmt.setString(3, blankToNull(f.getMaterial()));
                            stmt.setString(4, blankToNull(f.getDimensions()));
                            stmt.executeUpdate();
                        }
                        break;
                    case "Books":
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "INSERT INTO books (product_id, author, isbn, publisher) VALUES (?, ?, ?, ?)")) {
                            stmt.setInt(1, id);
                            stmt.setString(2, blankToNull(f.getAuthor()));
                            stmt.setString(3, blankToNull(f.getIsbn()));
                            stmt.setString(4, blankToNull(f.getPublisher()));
                            stmt.executeUpdate();
                        }
                        break;
                    default:
                        break; // "Other": no subclass row
                }
                conn.commit();
                return id;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Applies an admin's store prices for one product. For each store:
     * price null = store doesn't carry it (listing removed), new price = listing added,
     * changed price = listing updated (the trigger logs the old price to price_history).
     * Returns {added, updated, removed}.
     */
    public int[] savePrices(int productId, List<Listing> wanted) throws SQLException {
        int added = 0, updated = 0, removed = 0;
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Map<Integer, Listing> existing = getListings(conn, productId);
                for (Listing w : wanted) {
                    Listing ex = existing.get(w.getStoreId());
                    if (w.getPrice() == null) {
                        if (ex != null) {
                            deleteListing(conn, ex.getListingId());
                            removed++;
                        }
                    } else if (ex == null) {
                        String sql = "INSERT INTO price_listing (product_id, store_id, price, product_url) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setInt(1, productId);
                            stmt.setInt(2, w.getStoreId());
                            stmt.setBigDecimal(3, w.getPrice());
                            stmt.setString(4, blankToNull(w.getProductUrl()));
                            stmt.executeUpdate();
                        }
                        added++;
                    } else if (w.getPrice().compareTo(ex.getPrice()) != 0) {
                        String sql = "UPDATE price_listing SET price = ?, product_url = ? WHERE listing_id = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setBigDecimal(1, w.getPrice());
                            stmt.setString(2, blankToNull(w.getProductUrl()));
                            stmt.setInt(3, ex.getListingId());
                            stmt.executeUpdate();
                        }
                        updated++;
                    } else if (!Objects.equals(blankToNull(w.getProductUrl()), ex.getProductUrl())) {
                        // URL-only change: keep last_updated so it still marks when the price was set.
                        String sql = "UPDATE price_listing SET product_url = ?, last_updated = last_updated WHERE listing_id = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setString(1, blankToNull(w.getProductUrl()));
                            stmt.setInt(2, ex.getListingId());
                            stmt.executeUpdate();
                        }
                        updated++;
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
        return new int[] {added, updated, removed};
    }

    /** Deletes a product and everything that references it. Returns its name, or null if not found. */
    public String deleteProduct(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String name;
                try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM products WHERE product_id = ?")) {
                    stmt.setInt(1, productId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return null;
                        }
                        name = rs.getString(1);
                    }
                }
                for (Listing l : getListings(conn, productId).values()) deleteListing(conn, l.getListingId());
                for (String sql : new String[] {
                        "DELETE FROM price_alert WHERE product_id = ?",
                        "DELETE FROM electronics WHERE product_id = ?",
                        "DELETE FROM household WHERE product_id = ?",
                        "DELETE FROM books WHERE product_id = ?",
                        "DELETE FROM products WHERE product_id = ?"}) {
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, productId);
                        stmt.executeUpdate();
                    }
                }
                conn.commit();
                return name;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Removes a listing plus the cart items and history rows that point at it. */
    private void deleteListing(Connection conn, int listingId) throws SQLException {
        for (String sql : new String[] {
                "DELETE FROM cart_item WHERE listing_id = ?",
                "DELETE FROM price_history WHERE listing_id = ?",
                "DELETE FROM price_listing WHERE listing_id = ?"}) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, listingId);
                stmt.executeUpdate();
            }
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static String blankToNull(String s) { return s == null || s.trim().isEmpty() ? null : s.trim(); }

}
