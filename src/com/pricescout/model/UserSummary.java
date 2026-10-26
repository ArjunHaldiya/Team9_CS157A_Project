package com.pricescout.model;

import java.sql.Timestamp;

/** A user as admins see it: account details plus how much they use the site. */
public class UserSummary {
    private final int id;
    private final String name;
    private final String email;
    private final String role;
    private final Timestamp createdAt;
    private final int cartItems;
    private final int alerts;

    public UserSummary(int id, String name, String email, String role, Timestamp createdAt, int cartItems, int alerts) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
        this.cartItems = cartItems;
        this.alerts = alerts;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Timestamp getCreatedAt() { return createdAt; }
    public int getCartItems() { return cartItems; }
    public int getAlerts() { return alerts; }
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
