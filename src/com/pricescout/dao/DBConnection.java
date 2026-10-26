package com.pricescout.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central place to open a JDBC connection to the MySQL "pricescout" database.
 * Update DB_USER / DB_PASSWORD to match your local MySQL install.
 */
public class DBConnection {

    private static final String DB_URL =
        "jdbc:mysql://localhost:3306/pricescout?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Archi@2006";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "MySQL JDBC driver not found. Add mysql-connector-j-x.x.x.jar to WEB-INF/lib.", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
