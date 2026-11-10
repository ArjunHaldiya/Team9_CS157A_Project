package com.pricescout.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.pricescout.model.ProductDetail;
import com.pricescout.model.StoreListing;
import com.pricescout.util.InputUtil;

/**
 * Reads one product for the detail page (FR3): the products row, the facts from whichever ISA
 * subtype table it belongs to (electronics / household / books), and every store's price_listing.
 * Read-only: viewing a product never writes to the database.
 */
public class ProductDetailDAO {

    /** Returns the product, or null if there is no product with that id. */
    public ProductDetail find(int productId) throws SQLException {
        String sql =
            "SELECT p.name, p.description, p.image_url, " +
            "       e.product_id AS e_id, e.brand AS e_brand, e.model_number, e.warranty_months, " +
            "       h.product_id AS h_id, h.brand AS h_brand, h.material, h.dimensions, " +
            "       b.product_id AS b_id, b.author, b.isbn, b.publisher " +
            "FROM products p " +
            "LEFT JOIN electronics e ON e.product_id = p.product_id " +
            "LEFT JOIN household h ON h.product_id = p.product_id " +
            "LEFT JOIN books b ON b.product_id = p.product_id " +
            "WHERE p.product_id = ?";
        String listingSql =
            "SELECT pl.listing_id, s.store_name, pl.price, pl.last_updated, pl.product_url " +
            "FROM price_listing pl JOIN stores s ON s.store_id = pl.store_id " +
            "WHERE pl.product_id = ? ORDER BY pl.price, s.store_name";

        try (Connection conn = DBConnection.getConnection()) {
            ProductDetail detail;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, productId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) return null;
                    String category;
                    if (rs.getObject("e_id") != null) category = "Electronics";
                    else if (rs.getObject("h_id") != null) category = "Household";
                    else if (rs.getObject("b_id") != null) category = "Books";
                    else category = "Other";

                    detail = new ProductDetail(productId, rs.getString("name"), rs.getString("description"),
                                               rs.getString("image_url"), category);
                    Map<String, String> a = detail.getAttributes();
                    switch (category) {
                        case "Electronics":
                            put(a, "Brand", rs.getString("e_brand"));
                            put(a, "Model number", rs.getString("model_number"));
                            Object months = rs.getObject("warranty_months");
                            if (months != null) put(a, "Warranty", months + " months");
                            break;
                        case "Household":
                            put(a, "Brand", rs.getString("h_brand"));
                            put(a, "Material", rs.getString("material"));
                            put(a, "Dimensions", rs.getString("dimensions"));
                            break;
                        case "Books":
                            put(a, "Author", rs.getString("author"));
                            put(a, "ISBN", rs.getString("isbn"));
                            put(a, "Publisher", rs.getString("publisher"));
                            break;
                        default:
                            break;
                    }
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(listingSql)) {
                stmt.setInt(1, productId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        detail.getListings().add(new StoreListing(
                            rs.getInt("listing_id"),
                            rs.getString("store_name"),
                            rs.getBigDecimal("price"),
                            rs.getTimestamp("last_updated"),
                            InputUtil.webUrlOrNull(rs.getString("product_url"))));
                    }
                }
            }
            // Listings are sorted cheapest first; flag every listing tied for the lowest price.
            if (!detail.getListings().isEmpty()) {
                java.math.BigDecimal low = detail.getListings().get(0).getPrice();
                for (StoreListing l : detail.getListings()) {
                    l.setLowest(l.getPrice().compareTo(low) == 0);
                }
            }
            return detail;
        }
    }

    private static void put(Map<String, String> map, String label, String value) {
        if (value != null && !value.trim().isEmpty()) map.put(label, value.trim());
    }
}
