<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, java.math.BigDecimal, com.pricescout.model.Product, com.pricescout.model.Store" %>
<%
    String ctx = request.getContextPath();
    @SuppressWarnings("unchecked") List<Product> products = (List<Product>) request.getAttribute("products");
    @SuppressWarnings("unchecked") List<Store> stores = (List<Store>) request.getAttribute("stores");
    String flash = (String) request.getAttribute("flash");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Products · PriceScout Admin</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body class="cart-page">
<%@ include file="header.jspf" %>
<main class="cart-wrap admin-wrap">
    <div class="admin-head">
        <h1>Products &amp; prices <span class="admin-count"><%= products.size() %></span></h1>
        <a class="auth-btn admin-add" href="<%= ctx %>/admin/products/new">+ Add product</a>
    </div>
    <% if (flash != null) { %><div class="cart-flash" role="status"><%= HtmlUtil.esc(flash) %></div><% } %>

    <div class="admin-table-wrap">
    <table class="admin-table">
        <thead>
        <tr>
            <th>Product</th><th>Category</th>
            <% for (Store s : stores) { %><th class="num"><%= HtmlUtil.esc(s.getName()) %></th><% } %>
            <th class="actions">Actions</th>
        </tr>
        </thead>
        <tbody>
        <% for (Product p : products) { %>
        <tr>
            <td class="admin-name"><%= HtmlUtil.esc(p.getName()) %>
                <% if (p.getStorePrices().isEmpty()) { %><span class="admin-warn">Hidden: no store carries it</span><% } %>
            </td>
            <td><span class="category-tag"><%= HtmlUtil.esc(p.getCategory()) %></span></td>
            <% for (Store s : stores) { BigDecimal price = p.getStorePrices().get(s.getName()); %>
            <td class="num"><%= price == null ? "<span class=\"admin-muted\">Not listed</span>" : "$" + price %></td>
            <% } %>
            <td class="actions">
                <a href="<%= ctx %>/admin/products/edit?id=<%= p.getProductId() %>">Edit</a>
                <form method="post" action="<%= ctx %>/admin/products/delete"
                      onsubmit="return confirm('Delete this product? Its store listings, price history, alerts and cart entries are removed too.');">
                    <input type="hidden" name="productId" value="<%= p.getProductId() %>">
                    <button type="submit" class="cart-link-btn admin-delete">Delete</button>
                </form>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    </div>
</main>
</body>
</html>
