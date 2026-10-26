<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.pricescout.util.HtmlUtil" %>
<%
    String ctx = request.getContextPath();
    String error = (String) request.getAttribute("error");
    String name = (String) request.getAttribute("name");
    String email = (String) request.getAttribute("email");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Create account · PriceScout</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body class="auth-page">
<main class="auth-wrap">
    <a class="auth-logo" href="<%= ctx %>/">Price<span>Scout</span></a>
    <div class="auth-card">
        <h1>Create account</h1>
        <% if (error != null) { %>
        <div class="auth-error" role="alert"><%= HtmlUtil.esc(error) %></div>
        <% } %>
        <form method="post" action="<%= ctx %>/register" novalidate>
            <label for="name">Your name</label>
            <input id="name" name="name" type="text" autocomplete="name" maxlength="100" required autofocus
                   value="<%= HtmlUtil.esc(name) %>">
            <label for="email">Email</label>
            <input id="email" name="email" type="email" autocomplete="email" maxlength="150" required
                   value="<%= HtmlUtil.esc(email) %>">
            <label for="password">Password</label>
            <input id="password" name="password" type="password" autocomplete="new-password" minlength="8" required>
            <p class="auth-hint">At least 8 characters.</p>
            <label for="confirm">Re-enter password</label>
            <input id="confirm" name="confirm" type="password" autocomplete="new-password" required>
            <button type="submit" class="auth-btn">Create your PriceScout account</button>
        </form>
    </div>
    <p class="auth-switch">Already have an account? <a href="<%= ctx %>/login">Sign in</a></p>
    <p class="auth-back"><a href="<%= ctx %>/">&larr; Keep browsing as a guest</a></p>
</main>
</body>
</html>
