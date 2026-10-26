<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, java.math.BigDecimal, java.text.SimpleDateFormat" %>
<%@ page import="com.pricescout.model.StoreHistory, com.pricescout.model.PricePoint, com.pricescout.model.User, com.pricescout.util.HtmlUtil" %>
<%!
    /** "↓ $71.91" (green) or "↑ $8.00" (red) compared with the previous price. */
    private static String changeCell(BigDecimal prev, BigDecimal now) {
        if (prev == null) return "<td class=\"hist-change\">First recorded</td>";
        int cmp = now.compareTo(prev);
        if (cmp == 0) return "<td class=\"hist-change\">No change</td>";
        BigDecimal diff = now.subtract(prev).abs();
        return cmp < 0 ? "<td class=\"hist-change down\">&darr; $" + diff + "</td>"
                       : "<td class=\"hist-change up\">&uarr; $" + diff + "</td>";
    }
%>
<%
    String ctx = request.getContextPath();
    User user = (User) session.getAttribute("user");
    String productName = (String) request.getAttribute("productName");
    String category = (String) request.getAttribute("category");
    @SuppressWarnings("unchecked")
    List<StoreHistory> histories = (List<StoreHistory>) request.getAttribute("histories");
    String bestStore = (String) request.getAttribute("bestStore");
    String dbError = (String) request.getAttribute("dbError");
    SimpleDateFormat dateFmt = new SimpleDateFormat("MMM d, yyyy");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Price history<%= productName != null ? " · " + HtmlUtil.esc(productName) : "" %> · PriceScout</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body class="cart-page">
<header class="mini-topbar">
    <a class="mini-logo" href="<%= ctx %>/">Price<span>Scout</span></a>
    <nav>
        <a href="<%= ctx %>/">Continue shopping</a>
        <a href="<%= ctx %>/cart">Cart</a>
        <span class="mini-user">Hello, <%= HtmlUtil.esc(user.getName()) %></span>
        <a href="<%= ctx %>/logout">Sign out</a>
    </nav>
</header>

<main class="cart-wrap">
    <a class="hist-back" href="<%= ctx %>/">&larr; Back to all products</a>
    <% if (dbError != null) { %>
    <div class="auth-error" role="alert"><%= HtmlUtil.esc(dbError) %></div>
    <% } else { %>
    <span class="category-tag"><%= HtmlUtil.esc(category) %></span>
    <h1 class="hist-title">Price history: <%= HtmlUtil.esc(productName) %></h1>

    <div class="hist-grid">
        <% for (StoreHistory h : histories) {
               boolean isBest = h.getStoreName().equals(bestStore);
               BigDecimal change = h.getChangePercent(); %>
        <section class="hist-card <%= isBest ? "best" : "" %>">
            <div class="hist-card-head">
                <h2><%= HtmlUtil.esc(h.getStoreName()) %></h2>
                <% if (isBest) { %><span class="best-flag">Best Price</span><% } %>
            </div>
            <p class="hist-now">$<%= h.getCurrentPrice() %> <span>now</span></p>
            <% if (change != null) { %>
            <p class="hist-trend <%= change.signum() < 0 ? "down" : change.signum() > 0 ? "up" : "" %>">
                <%= change.signum() < 0 ? "&darr; " + change.abs() + "% since " : change.signum() > 0 ? "&uarr; " + change + "% since " : "No change since " %><%= dateFmt.format(h.getPoints().get(0).getRecordedAt()) %>
            </p>
            <p class="hist-low">Lowest recorded: $<%= h.getLowestPrice() %></p>
            <% } %>

            <% if (h.getPoints().isEmpty()) { %>
            <p class="hist-empty">No price changes recorded yet.</p>
            <% } else { %>
            <table class="hist-table">
                <thead><tr><th>Date</th><th>Price</th><th>Change</th></tr></thead>
                <tbody>
                <% BigDecimal prev = null;
                   for (PricePoint pt : h.getPoints()) { %>
                    <tr>
                        <td><%= dateFmt.format(pt.getRecordedAt()) %></td>
                        <td>$<%= pt.getPrice() %></td>
                        <%= changeCell(prev, pt.getPrice()) %>
                    </tr>
                <%     prev = pt.getPrice();
                   } %>
                    <tr class="hist-current">
                        <td>Now</td>
                        <td>$<%= h.getCurrentPrice() %></td>
                        <%= changeCell(prev, h.getCurrentPrice()) %>
                    </tr>
                </tbody>
            </table>
            <% } %>
        </section>
        <% } %>
    </div>
    <% } %>
</main>
</body>
</html>
