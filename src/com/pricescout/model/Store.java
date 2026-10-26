package com.pricescout.model;

public class Store {
    private final int id;
    private final String name;

    public Store(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
}
