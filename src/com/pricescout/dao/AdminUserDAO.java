package com.pricescout.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.pricescout.model.UserSummary;
import com.pricescout.util.PasswordUtil;

/** Admin-only user management. */
public class AdminUserDAO {

    private static final String SELECT =
        "SELECT u.user_id, u.name, u.email, u.role, u.created_at, " +
        "  (SELECT COALESCE(SUM(ci.quantity), 0) FROM cart c JOIN cart_item ci ON ci.cart_id = c.cart_id " +
        "    WHERE c.user_id = u.user_id) AS cart_items, " +
        "  (SELECT COUNT(*) FROM price_alert pa WHERE pa.user_id = u.user_id) AS alerts " +
        "FROM users u ";

    public List<UserSummary> listUsers() throws SQLException {
        List<UserSummary> users = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT + "ORDER BY u.role = 'ADMIN' DESC, u.name");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) users.add(map(rs));
        }
        return users;
    }

    public UserSummary getUser(int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT + "WHERE u.user_id = ?")) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int countAdmins() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'");
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public boolean emailTakenByOther(String email, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM users WHERE email = ? AND user_id <> ?")) {
            stmt.setString(1, email);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Updates name, email and role; also resets the password when newPassword is not null. */
    public boolean updateUser(int userId, String name, String email, String role, String newPassword) throws SQLException {
        String sql = newPassword == null
            ? "UPDATE users SET name = ?, email = ?, role = ? WHERE user_id = ?"
            : "UPDATE users SET name = ?, email = ?, role = ?, password_hash = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int i = 1;
            stmt.setString(i++, name);
            stmt.setString(i++, email);
            stmt.setString(i++, role);
            if (newPassword != null) stmt.setString(i++, PasswordUtil.hash(newPassword));
            stmt.setInt(i, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    /** Deletes a user with their cart, cart items and price alerts. Returns their name, or null if not found. */
    public String deleteUser(int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String name;
                try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM users WHERE user_id = ?")) {
                    stmt.setInt(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return null;
                        }
                        name = rs.getString(1);
                    }
                }
                for (String sql : new String[] {
                        "DELETE ci FROM cart_item ci JOIN cart c ON c.cart_id = ci.cart_id WHERE c.user_id = ?",
                        "DELETE FROM cart WHERE user_id = ?",
                        "DELETE FROM price_alert WHERE user_id = ?",
                        "DELETE FROM users WHERE user_id = ?"}) {
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, userId);
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

    private static UserSummary map(ResultSet rs) throws SQLException {
        return new UserSummary(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"), rs.getString("role"),
                               rs.getTimestamp("created_at"), rs.getInt("cart_items"), rs.getInt("alerts"));
    }
}
