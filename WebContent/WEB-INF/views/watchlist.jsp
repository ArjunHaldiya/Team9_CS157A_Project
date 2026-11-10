<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="com.pricescout.model.PriceAlert, com.pricescout.model.User, com.pricescout.util.HtmlUtil" %>
<%
    String ctx = request.getContextPath();
    User user = (User) session.getAttribute("user");
    int cartCount = session.getAttribute("cartCount") instanceof Integer ? (Integer) session.getAttribute("cartCount") : 0;
    @SuppressWarnings("unchecked")
    List<PriceAlert> alerts = (List<PriceAlert>) request.getAttribute("alerts");
    String flash = (String) request.getAttribute("flash");
    String dbError = (String) request.getAttribute("dbError");
    int reachedCount = 0;
    if (alerts != null) {
        for (PriceAlert a : alerts) if (a.isReached()) reachedCount++;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Your watchlist · PriceScout</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body class="cart-page">
<header class="mini-topbar">
    <a class="mini-logo" href="<%= ctx %>/">Price<span>Scout</span></a>
    <nav>
        <a href="<%= ctx %>/">Continue shopping</a>
        <a href="<%= ctx %>/cart">Cart (<%= cartCount %>)</a>
        <span class="mini-user">Hello, <%= HtmlUtil.esc(user.getName()) %></span>
        <a href="<%= ctx %>/logout">Sign out</a>
    </nav>
</header>

<main class="cart-wrap">
    <h1>Your watchlist</h1>

    <% if (flash != null) { %>
    <div class="cart-flash" role="status"><%= HtmlUtil.esc(flash) %></div>
    <% } %>
    <% if (dbError != null) { %>
    <div class="auth-error" role="alert"><%= HtmlUtil.esc(dbError) %></div>
    <% } %>

    <% if (alerts != null && alerts.isEmpty()) { %>
    <div class="cart-empty">
        <p>You aren't watching any prices yet.</p>
        <p class="pd-note">Open a product and set a target price to get a price drop alert here.</p>
        <a class="auth-btn cart-shop-btn" href="<%= ctx %>/">Browse products</a>
    </div>
    <% } else if (alerts != null) { %>
    <% if (reachedCount > 0) { %>
    <p class="wl-summary"><strong><%= reachedCount %></strong> of your <%= alerts.size() %> alert<%= alerts.size() == 1 ? "" : "s" %> reached the target price.</p>
    <% } %>
    <table class="pd-table wl-table">
        <thead>
            <tr><th>Product</th><th>Your target</th><th>Lowest price now</th><th>Status</th><th></th></tr>
        </thead>
        <tbody>
        <% for (PriceAlert a : alerts) { %>
            <tr class="<%= a.isReached() ? "pd-lowest" : "" %>">
                <td><a href="<%= ctx %>/product?id=<%= a.getProductId() %>"><%= HtmlUtil.esc(a.getProductName()) %></a></td>
                <td class="pd-price">$<%= a.getTargetPrice() %></td>
                <td>
                    <% if (a.getLowestPrice() == null) { %>
                    <span class="hist-low">Not listed</span>
                    <% } else { %>
                    $<%= a.getLowestPrice() %> <span class="hist-low">at <%= HtmlUtil.esc(a.getLowestStore()) %></span>
                    <% } %>
                </td>
                <td>
                    <% if (a.isReached()) { %><span class="best-flag">Target reached</span><% } else { %><span class="wl-watching">Watching</span><% } %>
                </td>
                <td class="wl-actions">
                    <form method="post" action="<%= ctx %>/watchlist/set" class="qty-form">
                        <input type="hidden" name="productId" value="<%= a.getProductId() %>">
                        <label class="visually-hidden" for="t-<%= a.getAlertId() %>">New target</label>
                        <input id="t-<%= a.getAlertId() %>" type="text" inputmode="decimal" name="targetPrice" required
                               value="<%= a.getTargetPrice() %>">
                        <button type="submit" class="cart-btn">Update</button>
                    </form>
                    <form method="post" action="<%= ctx %>/watchlist/delete">
                        <input type="hidden" name="alertId" value="<%= a.getAlertId() %>">
                        <button type="submit" class="cart-link-btn">Remove</button>
                    </form>
                </td>
            </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</main>
</body>
</html>
