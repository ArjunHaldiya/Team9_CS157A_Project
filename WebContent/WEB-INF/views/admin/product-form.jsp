<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, java.util.Map, com.pricescout.model.ProductForm, com.pricescout.model.Store, com.pricescout.model.Listing" %>
<%
    String ctx = request.getContextPath();
    ProductForm f = (ProductForm) request.getAttribute("form");
    @SuppressWarnings("unchecked") List<Store> stores = (List<Store>) request.getAttribute("stores");
    @SuppressWarnings("unchecked") Map<Integer, Listing> listings = (Map<Integer, Listing>) request.getAttribute("listings");
    String error = (String) request.getAttribute("error");
    String flash = (String) request.getAttribute("flash");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= f.isNew() ? "Add product" : "Edit " + HtmlUtil.esc(f.getName()) %> · PriceScout Admin</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body class="cart-page">
<%@ include file="header.jspf" %>
<main class="cart-wrap admin-wrap">
    <a class="hist-back" href="<%= ctx %>/admin/products">&larr; All products</a>
    <h1><%= f.isNew() ? "Add product" : "Edit product" %></h1>
    <% if (flash != null) { %><div class="cart-flash" role="status"><%= HtmlUtil.esc(flash) %></div><% } %>
    <% if (error != null) { %><div class="auth-error" role="alert"><%= HtmlUtil.esc(error) %></div><% } %>

    <section class="admin-card">
        <h2>Product details</h2>
        <form method="post" action="<%= ctx %>/admin/products/save" class="admin-form">
            <input type="hidden" name="productId" value="<%= f.getProductId() %>">
            <label for="name">Name *</label>
            <input id="name" name="name" maxlength="200" required value="<%= HtmlUtil.esc(f.getName()) %>">
            <label for="description">Description</label>
            <textarea id="description" name="description" rows="3" maxlength="2000"><%= HtmlUtil.esc(f.getDescription()) %></textarea>
            <label for="imageUrl">Image URL</label>
            <input id="imageUrl" name="imageUrl" type="url" maxlength="500" placeholder="https://..." value="<%= HtmlUtil.esc(f.getImageUrl()) %>">
            <label for="category">Category *</label>
            <select id="category" name="category">
                <% for (String c : ProductForm.CATEGORIES) { %>
                <option value="<%= c %>" <%= c.equals(f.getCategory()) ? "selected" : "" %>><%= c %></option>
                <% } %>
            </select>

            <fieldset data-cat="Electronics" class="admin-sub">
                <legend>Electronics details</legend>
                <label for="e_brand">Brand</label><input id="e_brand" name="e_brand" maxlength="100" value="<%= "Electronics".equals(f.getCategory()) ? HtmlUtil.esc(f.getBrand()) : "" %>">
                <label for="modelNumber">Model number</label><input id="modelNumber" name="modelNumber" maxlength="100" value="<%= HtmlUtil.esc(f.getModelNumber()) %>">
                <label for="warrantyMonths">Warranty (months)</label><input id="warrantyMonths" name="warrantyMonths" type="number" min="0" max="240" value="<%= f.getWarrantyMonths() == null ? "" : f.getWarrantyMonths() %>">
            </fieldset>
            <fieldset data-cat="Household" class="admin-sub">
                <legend>Household details</legend>
                <label for="h_brand">Brand</label><input id="h_brand" name="h_brand" maxlength="100" value="<%= "Household".equals(f.getCategory()) ? HtmlUtil.esc(f.getBrand()) : "" %>">
                <label for="material">Material</label><input id="material" name="material" maxlength="100" value="<%= HtmlUtil.esc(f.getMaterial()) %>">
                <label for="dimensions">Dimensions</label><input id="dimensions" name="dimensions" maxlength="100" value="<%= HtmlUtil.esc(f.getDimensions()) %>">
            </fieldset>
            <fieldset data-cat="Books" class="admin-sub">
                <legend>Book details</legend>
                <label for="author">Author</label><input id="author" name="author" maxlength="150" value="<%= HtmlUtil.esc(f.getAuthor()) %>">
                <label for="isbn">ISBN</label><input id="isbn" name="isbn" maxlength="20" value="<%= HtmlUtil.esc(f.getIsbn()) %>">
                <label for="publisher">Publisher</label><input id="publisher" name="publisher" maxlength="150" value="<%= HtmlUtil.esc(f.getPublisher()) %>">
            </fieldset>
            <button type="submit" class="auth-btn admin-save"><%= f.isNew() ? "Create product" : "Save details" %></button>
        </form>
    </section>

    <% if (!f.isNew()) { %>
    <section class="admin-card">
        <h2>Store prices</h2>
        <p class="admin-help">Leave a price blank if the store doesn't carry this product (removes its listing).
            Price changes are saved to price history automatically.</p>
        <form method="post" action="<%= ctx %>/admin/prices/save">
            <input type="hidden" name="productId" value="<%= f.getProductId() %>">
            <table class="admin-table admin-prices">
                <thead><tr><th>Store</th><th>Price ($)</th><th>Product URL</th><th>Status</th></tr></thead>
                <tbody>
                <% for (Store s : stores) { Listing l = listings == null ? null : listings.get(s.getId()); %>
                <tr>
                    <td><label for="price_<%= s.getId() %>"><%= HtmlUtil.esc(s.getName()) %></label></td>
                    <td><input id="price_<%= s.getId() %>" name="price_<%= s.getId() %>" inputmode="decimal" placeholder="Not carried"
                               value="<%= l == null ? "" : l.getPrice() %>"></td>
                    <td><input name="url_<%= s.getId() %>" type="url" maxlength="500" placeholder="https://..." aria-label="<%= HtmlUtil.esc(s.getName()) %> product URL"
                               value="<%= l == null || l.getProductUrl() == null ? "" : HtmlUtil.esc(l.getProductUrl()) %>"></td>
                    <td><%= l == null ? "<span class=\"admin-muted\">Not carried</span>" : "<span class=\"admin-ok\">Listed</span>" %></td>
                </tr>
                <% } %>
                </tbody>
            </table>
            <button type="submit" class="auth-btn admin-save">Save prices</button>
        </form>
    </section>

    <section class="admin-card admin-danger">
        <h2>Delete product</h2>
        <p class="admin-help">Removes the product with its store listings, price history, alerts and cart entries.</p>
        <form method="post" action="<%= ctx %>/admin/products/delete"
              onsubmit="return confirm('Delete this product? This cannot be undone.');">
            <input type="hidden" name="productId" value="<%= f.getProductId() %>">
            <button type="submit" class="admin-delete-btn">Delete product</button>
        </form>
    </section>
    <% } %>
</main>
<script>
(function () {
    var sel = document.getElementById('category');
    function sync() {
        document.querySelectorAll('[data-cat]').forEach(function (fs) {
            fs.hidden = fs.getAttribute('data-cat') !== sel.value;
        });
    }
    sel.addEventListener('change', sync);
    sync();
})();
</script>
</body>
</html>
