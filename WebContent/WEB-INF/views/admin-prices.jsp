<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, java.util.Map, java.text.SimpleDateFormat" %>
<%@ page import="com.pricescout.model.AdminListing, com.pricescout.model.User, com.pricescout.util.HtmlUtil" %>
<%
    String ctx = request.getContextPath();
    User user = (User) session.getAttribute("user");
    @SuppressWarnings("unchecked")
    List<AdminListing> listings = (List<AdminListing>) request.getAttribute("listings");
    @SuppressWarnings("unchecked")
    Map<Integer, String> productOptions = (Map<Integer, String>) request.getAttribute("productOptions");
    @SuppressWarnings("unchecked")
    Map<Integer, String> storeOptions = (Map<Integer, String>) request.getAttribute("storeOptions");
    String q = (String) request.getAttribute("q");
    if (q == null) q = "";
    String flash = (String) request.getAttribute("flash");
    boolean flashError = Boolean.TRUE.equals(request.getAttribute("flashError"));
    String dbError = (String) request.getAttribute("dbError");
    SimpleDateFormat dateFmt = new SimpleDateFormat("MMM d, yyyy h:mm a");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Manage prices · PriceScout Admin</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body class="cart-page">
<header class="mini-topbar">
    <a class="mini-logo" href="<%= ctx %>/">Price<span>Scout</span> <small class="admin-tag">Admin</small></a>
    <nav>
        <a href="<%= ctx %>/">View store</a>
        <span class="mini-user">Hello, <%= HtmlUtil.esc(user.getName()) %></span>
        <a href="<%= ctx %>/logout">Sign out</a>
    </nav>
</header>

<main class="cart-wrap">
    <h1>Manage prices</h1>

    <% if (flash != null) { %>
    <div class="<%= flashError ? "auth-error" : "cart-flash" %>" role="<%= flashError ? "alert" : "status" %>"><%= HtmlUtil.esc(flash) %></div>
    <% } %>
    <% if (dbError != null) { %>
    <div class="auth-error" role="alert"><%= HtmlUtil.esc(dbError) %></div>
    <% } %>

    <% if (productOptions != null && storeOptions != null) { %>
    <section class="pd-card">
        <h2>Add a store listing</h2>
        <form method="post" action="<%= ctx %>/admin/prices/add" class="adm-add">
            <input type="hidden" name="q" value="<%= HtmlUtil.esc(q) %>">
            <label>Product
                <select name="productId" required>
                    <% for (Map.Entry<Integer, String> p : productOptions.entrySet()) { %>
                    <option value="<%= p.getKey() %>"><%= HtmlUtil.esc(p.getValue()) %></option>
                    <% } %>
                </select>
            </label>
            <label>Store
                <select name="storeId" required>
                    <% for (Map.Entry<Integer, String> s : storeOptions.entrySet()) { %>
                    <option value="<%= s.getKey() %>"><%= HtmlUtil.esc(s.getValue()) %></option>
                    <% } %>
                </select>
            </label>
            <label>Price ($)
                <input type="text" inputmode="decimal" name="price" required placeholder="e.g. 749.99">
            </label>
            <label>Store link (optional)
                <input type="text" name="productUrl" maxlength="500" placeholder="https://...">
            </label>
            <button type="submit" class="btn btn-primary">Add listing</button>
        </form>
    </section>
    <% } %>

    <section class="pd-card">
        <h2>Current listings</h2>
        <form method="get" action="<%= ctx %>/admin/prices" class="adm-filter">
            <label class="visually-hidden" for="adm-q">Filter by product name</label>
            <input id="adm-q" type="text" name="q" value="<%= HtmlUtil.esc(q) %>" placeholder="Filter by product name">
            <button type="submit" class="cart-btn">Filter</button>
            <% if (!q.isEmpty()) { %><a class="btn-link" href="<%= ctx %>/admin/prices">Clear</a><% } %>
        </form>

        <% if (listings != null && listings.isEmpty()) { %>
        <p class="hist-empty">No listings match.</p>
        <% } else if (listings != null) { %>
        <table class="pd-table adm-table">
            <thead>
                <tr><th>Product</th><th>Store</th><th>Price</th><th>Last updated</th><th>Change price</th><th></th></tr>
            </thead>
            <tbody>
            <% for (AdminListing l : listings) { %>
                <tr>
                    <td><a href="<%= ctx %>/product?id=<%= l.getProductId() %>"><%= HtmlUtil.esc(l.getProductName()) %></a>
                        <span class="category-tag"><%= HtmlUtil.esc(l.getCategory()) %></span></td>
                    <td><%= HtmlUtil.esc(l.getStoreName()) %></td>
                    <td class="pd-price">$<%= l.getPrice() %></td>
                    <td><%= l.getLastUpdated() == null ? "&mdash;" : dateFmt.format(l.getLastUpdated()) %></td>
                    <td>
                        <form method="post" action="<%= ctx %>/admin/prices/update" class="qty-form">
                            <input type="hidden" name="listingId" value="<%= l.getListingId() %>">
                            <input type="hidden" name="q" value="<%= HtmlUtil.esc(q) %>">
                            <label class="visually-hidden" for="p-<%= l.getListingId() %>">New price</label>
                            <input id="p-<%= l.getListingId() %>" type="text" inputmode="decimal" name="price" required
                                   value="<%= l.getPrice() %>">
                            <button type="submit" class="cart-btn">Save</button>
                        </form>
                    </td>
                    <td>
                        <form method="post" action="<%= ctx %>/admin/prices/delete"
                              onsubmit="return confirm('Remove this listing? It is also removed from customers\' carts and its price history is deleted.');">
                            <input type="hidden" name="listingId" value="<%= l.getListingId() %>">
                            <input type="hidden" name="q" value="<%= HtmlUtil.esc(q) %>">
                            <button type="submit" class="cart-link-btn">Delete</button>
                        </form>
                    </td>
                </tr>
            <% } %>
            </tbody>
        </table>
        <% } %>
    </section>
</main>
</body>
</html>
