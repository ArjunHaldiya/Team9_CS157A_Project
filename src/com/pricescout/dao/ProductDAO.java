package com.pricescout.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.pricescout.model.Product;

//DAO - Data Access Object
public class ProductDAO {

    private static final String BASE_SELECT =
        "SELECT p.product_id, p.name AS product_name, p.image_url, " +
        "       c.name AS category_name, s.name AS store_name, pl.price " +
        "FROM Products p " +
        "JOIN Categories c ON p.category_id = c.category_id " +
        "JOIN PriceListing pl ON pl.product_id = p.product_id " +
        "JOIN Stores s ON pl.store_id = s.store_id ";

    
    public List<Product> search(String keyword, String category) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT + "WHERE 1=1 ");
        List<String> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND p.name LIKE ? ");
            params.add("%" + keyword.trim() + "%");
        }
        if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("All")) {
            sql.append("AND c.name = ? ");
            params.add(category.trim());
        }
        sql.append("ORDER BY p.name, s.name");

        Map<Integer, Product> byId = new LinkedHashMap<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int productId = rs.getInt("product_id");
                    Product product = byId.get(productId);
                    if (product == null) {
                        product = new Product(
                            productId,
                            rs.getString("product_name"),
                            rs.getString("category_name"),
                            rs.getString("image_url")
                        );
                        byId.put(productId, product);
                    }
                    BigDecimal price = rs.getBigDecimal("price");
                    product.addStorePrice(rs.getString("store_name"), price);
                }
            }
        }
        return new ArrayList<>(byId.values());
    }

    /** Distinct category names, for the browse-by-category nav. */
    public List<String> getCategories() throws SQLException {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT name FROM Categories ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString("name"));
            }
        }
        return categories;
    }
}
