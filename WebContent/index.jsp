<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, java.math.BigDecimal" %>
<%@ page import="com.pricescout.model.Product" %>
<%@ page import="com.pricescout.model.User, com.pricescout.util.HtmlUtil" %>
<%
    @SuppressWarnings("unchecked")
    List<Product> products = (List<Product>) request.getAttribute("products");
    @SuppressWarnings("unchecked")
    List<String> categories = (List<String>) request.getAttribute("categories");
    String selectedCategory = (String) request.getAttribute("selectedCategory");
    String keyword = (String) request.getAttribute("keyword");
    String dbError = (String) request.getAttribute("dbError");
    User currentUser = (User) session.getAttribute("user");
    int cartCount = session.getAttribute("cartCount") instanceof Integer ? (Integer) session.getAttribute("cartCount") : 0;
    if (selectedCategory == null) selectedCategory = "All";
    if (keyword == null) keyword = "";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PriceScout — Compare prices across Best Buy, Target &amp; Walmart</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>

    <!-- ===== Row 1: dark top bar (logo / search / account / cart) ===== -->
    <header class="topbar">
        <div class="topbar-inner">
            <a href="<%= request.getContextPath() %>/" class="logo">
                Price<span>Scout</span>
            </a>

            <div class="deliver-to">
                <span class="deliver-label">Comparing prices at</span>
                <span class="deliver-value">Best Buy · Target · Walmart</span>
            </div>

            <form class="search-form" action="<%= request.getContextPath() %>/search" method="get">
                <select class="search-category" name="category" aria-label="Search category">
                    <option value="All" <%= "All".equals(selectedCategory) ? "selected" : "" %>>All</option>
                    <%
                        if (categories != null) {
                            for (String cat : categories) {
                    %>
                        <option value="<%= cat %>" <%= cat.equals(selectedCategory) ? "selected" : "" %>><%= cat %></option>
                    <%
                            }
                        }
                    %>
                </select>
                <input
                    type="text"
                    name="q"
                    class="search-input"
                    placeholder="Search PriceScout"
                    value="<%= keyword %>"
                    autocomplete="off">
                <button type="submit" class="search-btn" aria-label="Search">
                    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="11" cy="11" r="7"></circle>
                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    </svg>
                </button>
            </form>

            <nav class="topbar-nav">
                <% if (currentUser == null) { %>
                <a href="<%= request.getContextPath() %>/login" class="topbar-link">
                    <span class="topbar-link-top">Sign In</span>
                    <span class="topbar-link-bottom">Account</span>
                </a>
                <% } else { %>
                <a href="<%= request.getContextPath() %>/logout" class="topbar-link" title="Sign out">
                    <span class="topbar-link-top">Hello, <%= HtmlUtil.esc(currentUser.getName()) %></span>
                    <span class="topbar-link-bottom">Sign Out</span>
                </a>
                <% } %>
                <% if (currentUser != null && currentUser.isAdmin()) { %>
                <a href="<%= request.getContextPath() %>/admin" class="topbar-link">
                    <span class="topbar-link-top">PriceScout</span>
                    <span class="topbar-link-bottom">Admin</span>
                </a>
                <% } %>
                <a href="<%= request.getContextPath() %>/login" class="topbar-link">
                    <span class="topbar-link-top">Your</span>
                    <span class="topbar-link-bottom">Watchlist</span>
                </a>
                <a href="<%= request.getContextPath() %>/cart" class="cart-link">
                    <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="9" cy="21" r="1"></circle>
                        <circle cx="20" cy="21" r="1"></circle>
                        <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
                    </svg>
                    <span class="cart-count"><%= cartCount %></span>
                    <span class="topbar-link-bottom">Cart</span>
                </a>
            </nav>
        </div>
    </header>

    <!-- ===== Row 2: category strip ===== -->
    <nav class="catbar" aria-label="Browse by category">
        <div class="catbar-inner">
            <button class="catbar-all" id="allCategoriesBtn" type="button">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="3" y1="6" x2="21" y2="6"></line>
                    <line x1="3" y1="12" x2="21" y2="12"></line>
                    <line x1="3" y1="18" x2="21" y2="18"></line>
                </svg>
                All Categories
            </button>
            <a href="<%= request.getContextPath() %>/search?category=All&q=<%= keyword %>"
               class="catbar-link <%= "All".equals(selectedCategory) ? "active" : "" %>">All Deals</a>
            <%
                if (categories != null) {
                    for (String cat : categories) {
                        boolean active = cat.equals(selectedCategory);
            %>
                <a href="<%= request.getContextPath() %>/search?category=<%= cat %>&q=<%= keyword %>"
                   class="catbar-link <%= active ? "active" : "" %>"><%= cat %></a>
            <%
                    }
                }
            %>
        </div>
    </nav>

    <!-- ===== Hero banner ===== -->
    <section class="hero">
        <div class="hero-text">
            <span class="hero-eyebrow">PriceScout</span>
            <h1>Same product. Three stores. One winner.</h1>
            <p>We check Best Buy, Target, and Walmart at once and flag the lowest price for you.</p>
            <a href="#productGrid" class="btn btn-primary btn-large">Start Comparing</a>
        </div>
    </section>

    <div class="page-body">

        <!-- ===== Sidebar filters ===== -->
        <aside class="sidebar">
            <h4 class="sidebar-title">Department</h4>
            <ul class="sidebar-list">
                <li>
                    <a href="<%= request.getContextPath() %>/search?category=All&q=<%= keyword %>"
                       class="<%= "All".equals(selectedCategory) ? "active" : "" %>">All Departments</a>
                </li>
                <%
                    if (categories != null) {
                        for (String cat : categories) {
                            boolean active = cat.equals(selectedCategory);
                %>
                    <li>
                        <a href="<%= request.getContextPath() %>/search?category=<%= cat %>&q=<%= keyword %>"
                           class="<%= active ? "active" : "" %>"><%= cat %></a>
                    </li>
                <%
                        }
                    }
                %>
            </ul>

            <h4 class="sidebar-title">Retailer</h4>
            <ul class="sidebar-list sidebar-list-static">
                <li>Best Buy</li>
                <li>Target</li>
                <li>Walmart</li>
            </ul>
        </aside>

        <main class="content">

            <div class="results-toolbar">
                <span class="results-count">
                    <%= products == null ? 0 : products.size() %> result<%= (products != null && products.size() == 1) ? "" : "s" %>
                    <% if (!keyword.isEmpty()) { %> for "<%= keyword %>"<% } %>
                </span>
                <label class="sort-label">
                    Sort by:
                    <select id="sortSelect">
                        <option value="name">Featured</option>
                        <option value="price-asc">Price: Low to High</option>
                        <option value="store">Store</option>
                    </select>
                </label>
            </div>

            <!-- ===== DB error fallback ===== -->
            <% if (dbError != null) { %>
                <div class="db-error">
                    <strong>Heads up:</strong> <%= dbError %>
                </div>
            <% } %>

            <!-- ===== Product grid ===== -->
            <section class="product-grid" id="productGrid">
            <%
                if (products != null) {
                    for (Product p : products) {
                        String lowestStore = p.getLowestPriceStore();
                        BigDecimal lowestPrice = p.getLowestPrice();
            %>
                <article class="product-card" data-name="<%= p.getName() %>">
                    <a href="<%= request.getContextPath() %>/login" class="product-image">
                        <img src="<%= p.getImageUrl() != null ? p.getImageUrl() : "images/placeholder.png" %>"
                             alt="<%= p.getName() %>"
                             onerror="this.onerror=null;this.src='images/placeholder.png'">
                    </a>

                    <div class="product-body">
                        <span class="category-tag"><%= p.getCategory() %></span>
                        <h3 class="product-name"><a href="<%= request.getContextPath() %>/login"><%= p.getName() %></a></h3>

                        <div class="price-headline">
                            <span class="price-currency">$</span><span class="price-amount"><%= lowestPrice %></span>
                            <span class="price-store-badge">at <%= lowestStore %></span>
                        </div>

                        <ul class="price-list">
                            <%
                                for (java.util.Map.Entry<String, BigDecimal> entry : p.getStorePrices().entrySet()) {
                                    boolean isLowest = entry.getKey().equals(lowestStore);
                            %>
                            <li class="price-row <%= isLowest ? "lowest" : "" %>" data-price="<%= entry.getValue() %>">
                                <span class="store-name"><%= entry.getKey() %></span>
                                <span class="store-price">$<%= entry.getValue() %></span>
                                <% if (isLowest) { %><span class="best-flag">Best Price</span><% } %>
                            </li>
                            <%
                                }
                            %>
                        </ul>
                    </div>

                    <div class="product-footer">
                        <% if (currentUser == null) { %>
                        <a class="btn btn-primary btn-full" href="<%= request.getContextPath() %>/login">Add to Cart</a>
                        <% } else { %>
                        <form class="add-to-cart" method="post" action="<%= request.getContextPath() %>/cart/add">
                            <input type="hidden" name="productId" value="<%= p.getProductId() %>">
                            <label class="visually-hidden" for="store-<%= p.getProductId() %>">Store</label>
                            <select id="store-<%= p.getProductId() %>" name="store" class="store-select">
                                <% for (java.util.Map.Entry<String, BigDecimal> opt : p.getStorePrices().entrySet()) { %>
                                <option value="<%= HtmlUtil.esc(opt.getKey()) %>" <%= opt.getKey().equals(lowestStore) ? "selected" : "" %>><%= HtmlUtil.esc(opt.getKey()) %> · $<%= opt.getValue() %></option>
                                <% } %>
                            </select>
                            <button type="submit" class="btn btn-primary btn-full">Add to Cart</button>
                        </form>
                        <% } %>
                        <a class="btn-link" href="<%= request.getContextPath() %>/history?productId=<%= p.getProductId() %>"><%= currentUser == null ? "Sign in to see price history" : "See price history" %></a>
                    </div>
                </article>
            <%
                    }
                }
                if (products != null && products.isEmpty()) {
            %>
                <p class="empty-state">No products matched your search. Try a different keyword or category.</p>
            <%
                }
            %>
            </section>
        </main>
    </div>

    <footer class="site-footer">
        <p>PriceScout &middot; CS 157A Team 9 project &middot; Prices shown are for demonstration only.</p>
    </footer>

    <script src="js/main.js"></script>
</body>
</html>
