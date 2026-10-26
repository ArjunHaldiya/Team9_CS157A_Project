<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.pricescout.util.HtmlUtil" %>
<%
    String ctx = request.getContextPath();
    String error = (String) request.getAttribute("error");
    String email = (String) request.getAttribute("email");
    String next = (String) request.getAttribute("next");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sign in · PriceScout</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body class="auth-page">
<main class="auth-wrap">
    <a class="auth-logo" href="<%= ctx %>/">Price<span>Scout</span></a>
    <div class="auth-card">
        <h1>Sign in</h1>
        <% if (error != null) { %>
        <div class="auth-error" role="alert"><%= HtmlUtil.esc(error) %></div>
        <% } %>
        <form method="post" action="<%= ctx %>/login" novalidate>
            <input type="hidden" name="next" value="<%= HtmlUtil.esc(next) %>">
            <label for="email">Email</label>
            <input id="email" name="email" type="email" autocomplete="email" required autofocus
                   value="<%= HtmlUtil.esc(email) %>">
            <label for="password">Password</label>
            <input id="password" name="password" type="password" autocomplete="current-password" required>
            <button type="submit" class="auth-btn">Sign in</button>
        </form>
    </div>
    <p class="auth-switch">New to PriceScout? <a href="<%= ctx %>/register">Create your account</a></p>
    <p class="auth-back"><a href="<%= ctx %>/">&larr; Keep browsing as a guest</a></p>
</main>
</body>
</html>
