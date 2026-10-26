package com.pricescout.model;

/**
 * Everything an admin edits for one product: the shared products columns plus the
 * fields of its ISA subclass (electronics, household or books). Category "Other" has no subclass row.
 */
public class ProductForm {
    public static final String[] CATEGORIES = {"Electronics", "Household", "Books", "Other"};

    private int productId;
    private String name = "";
    private String description = "";
    private String imageUrl = "";
    private String category = "Electronics";
    private String brand = "";          // electronics.brand or household.brand
    private String modelNumber = "";
    private Integer warrantyMonths;
    private String material = "";
    private String dimensions = "";
    private String author = "";
    private String isbn = "";
    private String publisher = "";

    public int getProductId() { return productId; }
    public void setProductId(int v) { productId = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String v) { imageUrl = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { category = v; }
    public String getBrand() { return brand; }
    public void setBrand(String v) { brand = v; }
    public String getModelNumber() { return modelNumber; }
    public void setModelNumber(String v) { modelNumber = v; }
    public Integer getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(Integer v) { warrantyMonths = v; }
    public String getMaterial() { return material; }
    public void setMaterial(String v) { material = v; }
    public String getDimensions() { return dimensions; }
    public void setDimensions(String v) { dimensions = v; }
    public String getAuthor() { return author; }
    public void setAuthor(String v) { author = v; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String v) { isbn = v; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String v) { publisher = v; }

    public boolean isNew() { return productId == 0; }
}
