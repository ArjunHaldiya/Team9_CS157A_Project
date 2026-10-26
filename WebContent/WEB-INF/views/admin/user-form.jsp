<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.text.SimpleDateFormat, com.pricescout.model.UserSummary" %>
<%
    String ctx = request.getContextPath();
    UserSummary target = (UserSummary) request.getAttribute("target");
    String name = (String) request.getAttribute("name");
    String email = (String) request.getAttribute("email");
    String role = (String) request.getAttribute("role");
    String error = (String) request.getAttribute("error");
    boolean isMe = target.getId() == ((User) session.getAttribute("user")).getId();
    SimpleDateFormat dateFmt = new SimpleDateFormat("MMM d, yyyy");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Edit <%= HtmlUtil.esc(target.getName()) %> · PriceScout Admin</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body class="cart-page">
<%@ include file="header.jspf" %>
<main class="cart-wrap admin-wrap">
    <a class="hist-back" href="<%= ctx %>/admin/users">&larr; All users</a>
    <h1>Edit user</h1>
    <% if (error != null) { %><div class="auth-error" role="alert"><%= HtmlUtil.esc(error) %></div><% } %>

    <section class="admin-card">
        <p class="admin-help">Joined <%= target.getCreatedAt() == null ? "" : dateFmt.format(target.getCreatedAt()) %>
            · <%= target.getCartItems() %> item<%= target.getCartItems() == 1 ? "" : "s" %> in cart
            · <%= target.getAlerts() %> price alert<%= target.getAlerts() == 1 ? "" : "s" %></p>
        <form method="post" action="<%= ctx %>/admin/users/save" class="admin-form">
            <input type="hidden" name="userId" value="<%= target.getId() %>">
            <label for="name">Name</label>
            <input id="name" name="name" maxlength="100" required value="<%= HtmlUtil.esc(name) %>">
            <label for="email">Email</label>
            <input id="email" name="email" type="email" maxlength="150" required value="<%= HtmlUtil.esc(email) %>">
            <label for="role">Role</label>
            <select id="role" name="role" <%= isMe ? "disabled" : "" %>>
                <option value="CUSTOMER" <%= "CUSTOMER".equals(role) ? "selected" : "" %>>Customer</option>
                <option value="ADMIN" <%= "ADMIN".equals(role) ? "selected" : "" %>>Admin</option>
            </select>
            <% if (isMe) { %>
            <input type="hidden" name="role" value="ADMIN">
            <p class="admin-help">You can't change your own role.</p>
            <% } %>
            <label for="newPassword">Reset password</label>
            <input id="newPassword" name="newPassword" type="password" autocomplete="new-password" minlength="8" placeholder="Leave blank to keep the current password">
            <p class="admin-help">Role changes take effect on the user's next page load.</p>
            <button type="submit" class="auth-btn admin-save">Save user</button>
        </form>
    </section>
</main>
</body>
</html>
