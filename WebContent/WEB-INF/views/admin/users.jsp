<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, java.text.SimpleDateFormat, com.pricescout.model.UserSummary" %>
<%
    String ctx = request.getContextPath();
    @SuppressWarnings("unchecked") List<UserSummary> users = (List<UserSummary>) request.getAttribute("users");
    String flash = (String) request.getAttribute("flash");
    int myId = ((User) session.getAttribute("user")).getId();
    SimpleDateFormat dateFmt = new SimpleDateFormat("MMM d, yyyy");
    int admins = 0;
    for (UserSummary u : users) if (u.isAdmin()) admins++;
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Users · PriceScout Admin</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body class="cart-page">
<%@ include file="header.jspf" %>
<main class="cart-wrap admin-wrap">
    <div class="admin-head">
        <h1>Users <span class="admin-count"><%= users.size() %></span></h1>
        <span class="admin-help"><%= admins %> admin<%= admins == 1 ? "" : "s" %> · <%= users.size() - admins %> customer<%= users.size() - admins == 1 ? "" : "s" %></span>
    </div>
    <% if (flash != null) { %><div class="cart-flash" role="status"><%= HtmlUtil.esc(flash) %></div><% } %>

    <div class="admin-table-wrap">
    <table class="admin-table">
        <thead>
        <tr><th>Name</th><th>Email</th><th>Role</th><th>Joined</th><th class="num">Cart items</th><th class="num">Alerts</th><th class="actions">Actions</th></tr>
        </thead>
        <tbody>
        <% for (UserSummary u : users) { boolean isMe = u.getId() == myId; %>
        <tr>
            <td class="admin-name"><%= HtmlUtil.esc(u.getName()) %><% if (isMe) { %> <span class="admin-you">You</span><% } %></td>
            <td><%= HtmlUtil.esc(u.getEmail()) %></td>
            <td><span class="role-badge <%= u.isAdmin() ? "admin" : "" %>"><%= u.isAdmin() ? "Admin" : "Customer" %></span></td>
            <td><%= u.getCreatedAt() == null ? "" : dateFmt.format(u.getCreatedAt()) %></td>
            <td class="num"><%= u.getCartItems() %></td>
            <td class="num"><%= u.getAlerts() %></td>
            <td class="actions">
                <a href="<%= ctx %>/admin/users/edit?id=<%= u.getId() %>">Edit</a>
                <% if (!isMe) { %>
                <form method="post" action="<%= ctx %>/admin/users/delete"
                      onsubmit="return confirm('Delete this user? Their cart and price alerts are removed too.');">
                    <input type="hidden" name="userId" value="<%= u.getId() %>">
                    <button type="submit" class="cart-link-btn admin-delete">Delete</button>
                </form>
                <% } %>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    </div>
</main>
</body>
</html>
