<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, java.math.BigDecimal, java.text.SimpleDateFormat" %>
<%@ page import="com.pricescout.model.CartItem, com.pricescout.model.User, com.pricescout.util.HtmlUtil" %>
<%
    String ctx = request.getContextPath();
    User user = (User) session.getAttribute("user");
    @SuppressWarnings("unchecked")
    List<CartItem> items = (List<CartItem>) request.getAttribute("items");
    BigDecimal subtotal = (BigDecimal) request.getAttribute("subtotal");
    Integer itemCount = (Integer) request.getAttribute("itemCount");
    String flash = (String) request.getAttribute("flash");
    String dbError = (String) request.getAttribute("dbError");
    SimpleDateFormat dateFmt = new SimpleDateFormat("MMM d, yyyy");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Your cart · PriceScout</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body class="cart-page">
<header class="mini-topbar">
    <a class="mini-logo" href="<%= ctx %>/">Price<span>Scout</span></a>
    <nav>
        <a href="<%= ctx %>/">Continue shopping</a>
        <span class="mini-user">Hello, <%= HtmlUtil.esc(user.getName()) %></span>
        <a href="<%= ctx %>/logout">Sign out</a>
    </nav>
</header>

<main class="cart-wrap">
    <h1>Your cart</h1>

    <% if (flash != null) { %>
    <div class="cart-flash" role="status"><%= HtmlUtil.esc(flash) %></div>
    <% } %>
    <% if (dbError != null) { %>
    <div class="auth-error" role="alert"><%= HtmlUtil.esc(dbError) %></div>
    <% } %>

    <% if (items != null && items.isEmpty()) { %>
    <div class="cart-empty">
        <p>Your cart is empty.</p>
        <a class="auth-btn cart-shop-btn" href="<%= ctx %>/">Start comparing prices</a>
    </div>
    <% } else if (items != null) { %>
    <div class="cart-layout">
        <ul class="cart-list">
            <% for (CartItem item : items) { %>
            <li class="cart-item">
                <div class="cart-item-info">
                    <span class="category-tag"><%= HtmlUtil.esc(item.getCategory()) %></span>
                    <h2 class="cart-item-name"><%= HtmlUtil.esc(item.getProductName()) %></h2>
                    <p class="cart-item-store">From <strong><%= HtmlUtil.esc(item.getStoreName()) %></strong> · $<%= item.getPrice() %> each</p>
                    <% if (item.getAddedAt() != null) { %>
                    <p class="cart-item-added">Added <%= dateFmt.format(item.getAddedAt()) %></p>
                    <% } %>
                    <% if (item.isCheaperElsewhere()) { %>
                    <p class="cart-item-tip">Cheaper at <%= HtmlUtil.esc(item.getBestStore()) %>: $<%= item.getBestPrice() %></p>
                    <% } %>
                </div>
                <div class="cart-item-actions">
                    <form method="post" action="<%= ctx %>/cart/update" class="qty-form">
                        <input type="hidden" name="itemId" value="<%= item.getItemId() %>">
                        <label for="qty-<%= item.getItemId() %>">Qty</label>
                        <input id="qty-<%= item.getItemId() %>" type="number" name="quantity" min="0" max="99"
                               value="<%= item.getQuantity() %>">
                        <button type="submit" class="cart-btn">Update</button>
                    </form>
                    <form method="post" action="<%= ctx %>/cart/remove">
                        <input type="hidden" name="itemId" value="<%= item.getItemId() %>">
                        <button type="submit" class="cart-link-btn">Remove</button>
                    </form>
                </div>
                <div class="cart-item-total">$<%= item.getLineTotal() %></div>
            </li>
            <% } %>
        </ul>
        <aside class="cart-summary">
            <p>Subtotal (<%= itemCount %> <%= itemCount == 1 ? "item" : "items" %>):</p>
            <p class="cart-subtotal">$<%= subtotal %></p>
            <p class="cart-note">Prices shown are each store's current listing price.</p>
        </aside>
    </div>
    <% } %>
</main>
</body>
</html>
