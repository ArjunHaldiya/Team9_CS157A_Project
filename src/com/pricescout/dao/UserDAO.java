package com.pricescout.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.pricescout.model.User;
import com.pricescout.util.PasswordUtil;

/** All user queries use PreparedStatement parameters, so input can't inject SQL. */
public class UserDAO {

    /** Returns the user if the email/password pair is valid, otherwise null. */
    public User authenticate(String email, String password) throws SQLException {
        String sql = "SELECT user_id, name, email, password_hash, role FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    PasswordUtil.hash(password); // same work either way, so timing doesn't reveal which emails exist
                    return null;
                }
                if (!PasswordUtil.verify(password, rs.getString("password_hash"))) {
                    return null;
                }
                return new User(rs.getInt("user_id"), rs.getString("name"),
                                rs.getString("email"), rs.getString("role"));
            }
        }
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Creates a CUSTOMER and their cart (Cart is 1:1 with Users) in one transaction. */
    public User register(String name, String email, String password) throws SQLException {
        String hash = PasswordUtil.hash(password);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int userId;
                String insertUser = "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, 'CUSTOMER')";
                try (PreparedStatement stmt = conn.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, name);
                    stmt.setString(2, email);
                    stmt.setString(3, hash);
                    stmt.executeUpdate();
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        keys.next();
                        userId = keys.getInt(1);
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO cart (user_id) VALUES (?)")) {
                    stmt.setInt(1, userId);
                    stmt.executeUpdate();
                }
                conn.commit();
                return new User(userId, name, email, "CUSTOMER");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Fresh copy of a user from the database (role may have changed), or null if deleted. */
    public User findById(int userId) throws SQLException {
        String sql = "SELECT user_id, name, email, role FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? new User(rs.getInt("user_id"), rs.getString("name"),
                                            rs.getString("email"), rs.getString("role")) : null;
            }
        }
    }
}
