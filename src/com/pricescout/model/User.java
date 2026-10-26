package com.pricescout.model;

import java.io.Serializable;

/** Logged-in user kept in the HTTP session. Never holds the password hash. */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String name;
    private final String email;
    private final String role;

    public User(int id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
