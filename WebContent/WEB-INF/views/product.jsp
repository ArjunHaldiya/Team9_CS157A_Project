<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, java.util.Map, java.math.BigDecimal, java.text.SimpleDateFormat, java.net.URLEncoder" %>
<%@ page import="com.pricescout.model.ProductDetail, com.pricescout.model.StoreListing, com.pricescout.model.StoreHistory" %>
<%@ page import="com.pricescout.model.PriceAlert, com.pricescout.model.User, com.pricescout.util.HtmlUtil" %>
<%
    String ctx = request.getContextPath();
    User user = (User) session.getAttribute("user");
    int cartCount = session.getAttribute("cartCount") instanceof Integer ? (Integer) session.getAttribute("cartCount") : 0;
    ProductDetail product = (ProductDetail) request.getAttribute("product");
    @SuppressWarnings("unchecked")
    List<StoreHistory> histories = (List<StoreHistory>) request.getAttribute("histories");
    PriceAlert alert = (PriceAlert) request.getAttribute("alert");
    String flash = (String) request.getAttribute("flash");
    String dbError = (String) request.getAttribute("dbError");
    SimpleDateFormat dateFmt = new SimpleDateFormat("MMM d, yyyy");
    // Guests are sent to the login page and then back to this product.
    String loginHref = ctx + "/login";
    if (product != null) {
        loginHref += "?next=" + URLEncoder.encode(ctx + "/product?id=" + product.getProductId(), "UTF-8");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= product != null ? HtmlUtil.esc(product.getName()) : "Product" %> · PriceScout</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body class="cart-page">
<header class="mini-topbar">
    <a class="mini-logo" href="<%= ctx %>/">Price<span>Scout</span></a>
    <nav>
        <a href="<%= ctx %>/">Continue shopping</a>
        <a href="<%= ctx %>/watchlist">Watchlist</a>
        <a href="<%= ctx %>/cart">Cart (<%= cartCount %>)</a>
        <% if (user != null) { %>
        <span class="mini-user">Hello, <%= HtmlUtil.esc(user.getName()) %></span>
        <a href="<%= ctx %>/logout">Sign out</a>
        <% } else { %>
        <a href="<%= loginHref %>">Sign in</a>
        <% } %>
    </nav>
</header>

<main class="cart-wrap">
    <a class="hist-back" href="<%= ctx %>/">&larr; Back to all products</a>

    <% if (flash != null) { %>
    <div class="cart-flash" role="status"><%= HtmlUtil.esc(flash) %></div>
    <% } %>

    <% if (dbError != null || product == null) { %>
    <div class="auth-error" role="alert"><%= HtmlUtil.esc(dbError != null ? dbError : "Product not found.") %></div>
    <% } else {
           StoreListing best = product.getLowestListing(); %>

    <section class="pd-hero">
        <div class="pd-image">
            <img src="<%= product.getImageUrl() != null && !product.getImageUrl().isEmpty() ? HtmlUtil.esc(product.getImageUrl()) : ctx + "/images/placeholder.png" %>"
                 alt="<%= HtmlUtil.esc(product.getName()) %>"
                 onerror="this.onerror=null;this.src='<%= ctx %>/images/placeholder.png'">
        </div>
        <div class="pd-info">
            <span class="category-tag"><%= HtmlUtil.esc(product.getCategory()) %></span>
            <h1 class="hist-title"><%= HtmlUtil.esc(product.getName()) %></h1>
            <% if (best != null) { %>
            <p class="pd-best">From <strong>$<%= best.getPrice() %></strong> at <%= HtmlUtil.esc(best.getStoreName()) %></p>
            <% } %>
            <% if (product.getDescription() != null && !product.getDescription().trim().isEmpty()) { %>
            <p class="pd-desc"><%= HtmlUtil.esc(product.getDescription()) %></p>
            <% } %>
            <% if (!product.getAttributes().isEmpty()) { %>
            <table class="pd-attrs">
                <% for (Map.Entry<String, String> a : product.getAttributes().entrySet()) { %>
                <tr><th><%= HtmlUtil.esc(a.getKey()) %></th><td><%= HtmlUtil.esc(a.getValue()) %></td></tr>
                <% } %>
            </table>
            <% } %>
        </div>
    </section>

    <section class="pd-card">
        <h2>Price comparison</h2>
        <% if (product.getListings().isEmpty()) { %>
        <p class="hist-empty">No store lists this product right now.</p>
        <% } else { %>
        <table class="pd-table">
            <thead><tr><th>Store</th><th>Price</th><th>Last updated</th><th></th></tr></thead>
            <tbody>
            <% for (StoreListing l : product.getListings()) { %>
                <tr class="<%= l.isLowest() ? "pd-lowest" : "" %>">
                    <td><%= HtmlUtil.esc(l.getStoreName()) %>
                        <% if (l.isLowest()) { %><span class="best-flag">Best Price</span><% } %></td>
                    <td class="pd-price">$<%= l.getPrice() %></td>
                    <td><%= l.getLastUpdated() == null ? "&mdash;" : dateFmt.format(l.getLastUpdated()) %></td>
                    <td class="pd-buy">
                        <% if (l.getProductUrl() != null) { %>
                        <a href="<%= HtmlUtil.esc(l.getProductUrl()) %>" target="_blank" rel="noopener noreferrer">Buy at <%= HtmlUtil.esc(l.getStoreName()) %> &#8599;</a>
                        <% } %>
                    </td>
                </tr>
            <% } %>
            </tbody>
        </table>

        <% if (user != null) { %>
        <form class="pd-cart-form" method="post" action="<%= ctx %>/cart/add">
            <input type="hidden" name="productId" value="<%= product.getProductId() %>">
            <label for="pd-store">Add to cart from</label>
            <select id="pd-store" name="store" class="store-select">
                <% for (StoreListing l : product.getListings()) { %>
                <option value="<%= HtmlUtil.esc(l.getStoreName()) %>" <%= l.isLowest() ? "selected" : "" %>><%= HtmlUtil.esc(l.getStoreName()) %> · $<%= l.getPrice() %></option>
                <% } %>
            </select>
            <label for="pd-qty">Qty</label>
            <input id="pd-qty" type="number" name="quantity" min="1" max="99" value="1">
            <button type="submit" class="btn btn-primary">Add to Cart</button>
        </form>
        <% } else { %>
        <p class="pd-note"><a href="<%= loginHref %>">Log in</a> or <a href="<%= ctx %>/register">create an account</a> to add an item to your cart.</p>
        <% } %>
        <% } %>
    </section>

    <section class="pd-card">
        <h2>Price history</h2>
        <% if (user == null) { %>
        <p class="pd-note"><a href="<%= loginHref %>">Sign in</a> to see how this product's prices have changed.</p>
        <% } else if (histories == null || histories.isEmpty()) { %>
        <p class="hist-empty">No price history yet.</p>
        <% } else { %>
        <ul class="pd-trends">
            <% for (StoreHistory h : histories) {
                   BigDecimal change = h.getChangePercent(); %>
            <li>
                <strong><%= HtmlUtil.esc(h.getStoreName()) %></strong>
                <span>$<%= h.getCurrentPrice() %> now</span>
                <% if (change != null) { %>
                <span class="hist-trend <%= change.signum() < 0 ? "down" : change.signum() > 0 ? "up" : "" %>">
                    <%= change.signum() < 0 ? "&darr; " + change.abs() + "%" : change.signum() > 0 ? "&uarr; " + change + "%" : "No change" %>
                    since <%= dateFmt.format(h.getPoints().get(0).getRecordedAt()) %></span>
                <span class="hist-low">Lowest recorded: $<%= h.getLowestPrice() %></span>
                <% } else { %>
                <span class="hist-low">No price changes recorded yet</span>
                <% } %>
            </li>
            <% } %>
        </ul>
        <a class="btn-link" href="<%= ctx %>/history?productId=<%= product.getProductId() %>">See the full price history &rarr;</a>
        <% } %>
    </section>

    <section class="pd-card">
        <h2>Price drop alert</h2>
        <% if (user == null) { %>
        <p class="pd-note"><a href="<%= loginHref %>">Sign in</a> to get an alert when this product reaches your target price.</p>
        <% } else { %>
        <% if (alert != null) { %>
        <p class="pd-alert-status <%= alert.isReached() ? "reached" : "" %>">
            <% if (alert.isReached()) { %>
            Target reached! Your alert is set at <strong>$<%= alert.getTargetPrice() %></strong>.
            <% } else { %>
            Watching for <strong>$<%= alert.getTargetPrice() %></strong> or less.
            <% } %>
        </p>
        <% } %>
        <form class="pd-alert-form" method="post" action="<%= ctx %>/watchlist/set">
            <input type="hidden" name="productId" value="<%= product.getProductId() %>">
            <input type="hidden" name="from" value="product">
            <label for="pd-target">Alert me at or below $</label>
            <input id="pd-target" type="text" inputmode="decimal" name="targetPrice" required
                   placeholder="e.g. 749.99" value="<%= alert != null ? alert.getTargetPrice() : "" %>">
            <button type="submit" class="btn btn-primary"><%= alert == null ? "Set alert" : "Update alert" %></button>
        </form>
        <% if (alert != null) { %>
        <form method="post" action="<%= ctx %>/watchlist/delete" class="pd-alert-remove">
            <input type="hidden" name="alertId" value="<%= alert.getAlertId() %>">
            <input type="hidden" name="productId" value="<%= product.getProductId() %>">
            <input type="hidden" name="from" value="product">
            <button type="submit" class="cart-link-btn">Remove this alert</button>
        </form>
        <% } %>
        <% } %>
    </section>
    <% } %>
</main>
</body>
</html>
