package com.pricescout.servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.pricescout.dao.AdminPriceDAO;
import com.pricescout.model.User;
import com.pricescout.util.InputUtil;

/**
 * Admin price management (FR10).
 *   GET  /admin/prices          - every listing, with a name filter (?q=)
 *   POST /admin/prices/update   - change a listing's price (records history, triggers alerts)
 *   POST /admin/prices/add      - add a store's listing of a product
 *   POST /admin/prices/delete   - remove a listing
 * AuthFilter only lets ADMIN users reach /admin/*; each request re-checks the role as well.
 */
@WebServlet({"/admin/prices", "/admin/prices/update", "/admin/prices/add", "/admin/prices/delete"})
public class AdminPriceServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String VIEW = "/WEB-INF/views/admin-prices.jsp";
    private static final int MYSQL_DUPLICATE_KEY = 1062;
    private static final int MYSQL_FOREIGN_KEY_FAIL = 1452;

    private final AdminPriceDAO dao = new AdminPriceDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireAdmin(req, resp)) return;
        if (!"/admin/prices".equals(req.getServletPath())) {
            resp.sendRedirect(req.getContextPath() + "/admin/prices");
            return;
        }
        HttpSession session = req.getSession();
        req.setAttribute("flash", session.getAttribute("adminFlash"));
        req.setAttribute("flashError", session.getAttribute("adminFlashError"));
        session.removeAttribute("adminFlash");
        session.removeAttribute("adminFlashError");

        String q = req.getParameter("q") == null ? "" : req.getParameter("q").trim();
        req.setAttribute("q", q);
        try {
            req.setAttribute("listings", dao.listListings(q));
            req.setAttribute("productOptions", dao.productOptions());
            req.setAttribute("storeOptions", dao.storeOptions());
        } catch (SQLException e) {
            log("Loading admin prices failed", e);
            req.setAttribute("dbError", "Could not load listings. Try again in a moment.");
        }
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requireAdmin(req, resp)) return;
        req.setCharacterEncoding("UTF-8");
        String message;
        boolean error = false;
        try {
            switch (req.getServletPath()) {
                case "/admin/prices/update": {
                    Integer listingId = InputUtil.parseInt(req.getParameter("listingId"));
                    BigDecimal price = InputUtil.parsePrice(req.getParameter("price"));
                    if (listingId == null) {
                        message = "That listing doesn't exist.";
                        error = true;
                    } else if (price == null) {
                        message = "Enter a price like 749.99 (up to 99,999,999.99).";
                        error = true;
                    } else {
                        AdminPriceDAO.PriceChange change = dao.updatePrice(listingId, price);
                        if (change == null) {
                            message = "That listing no longer exists.";
                            error = true;
                        } else if (!change.isChanged()) {
                            message = change.productName + " at " + change.storeName + " is already $" + price + ".";
                        } else {
                            message = change.productName + " at " + change.storeName + ": $" + change.oldPrice
                                    + " → $" + change.newPrice + " (history recorded"
                                    + (change.alertsTriggered > 0
                                        ? ", " + change.alertsTriggered + " price alert" + (change.alertsTriggered == 1 ? "" : "s") + " triggered)."
                                        : ").");
                        }
                    }
                    break;
                }
                case "/admin/prices/add": {
                    Integer productId = InputUtil.parseInt(req.getParameter("productId"));
                    Integer storeId = InputUtil.parseInt(req.getParameter("storeId"));
                    BigDecimal price = InputUtil.parsePrice(req.getParameter("price"));
                    String rawUrl = req.getParameter("productUrl");
                    String url = rawUrl == null || rawUrl.trim().isEmpty() ? null : InputUtil.webUrlOrNull(rawUrl);
                    if (productId == null || storeId == null) {
                        message = "Choose a product and a store.";
                        error = true;
                    } else if (price == null) {
                        message = "Enter a price like 749.99 (up to 99,999,999.99).";
                        error = true;
                    } else if (rawUrl != null && !rawUrl.trim().isEmpty() && (url == null || url.length() > 500)) {
                        message = "The store link must start with http:// or https:// (500 characters max).";
                        error = true;
                    } else {
                        int triggered = dao.addListing(productId, storeId, price, url);
                        message = "Listing added at $" + price
                                + (triggered > 0 ? " (" + triggered + " price alert" + (triggered == 1 ? "" : "s") + " triggered)." : ".");
                    }
                    break;
                }
                case "/admin/prices/delete": {
                    Integer listingId = InputUtil.parseInt(req.getParameter("listingId"));
                    AdminPriceDAO.Removal removal = listingId == null ? null : dao.deleteListing(listingId);
                    if (removal == null) {
                        message = "That listing no longer exists.";
                        error = true;
                    } else {
                        message = "Removed " + removal.storeName + "'s listing of " + removal.productName
                                + " (" + removal.cartItemsRemoved + " cart item" + (removal.cartItemsRemoved == 1 ? "" : "s")
                                + " and " + removal.historyRowsRemoved + " history record" + (removal.historyRowsRemoved == 1 ? "" : "s")
                                + " removed with it).";
                    }
                    break;
                }
                default:
                    message = null;
            }
        } catch (SQLException e) {
            error = true;
            if (e.getErrorCode() == MYSQL_DUPLICATE_KEY) {
                message = "That store already has a listing for this product. Edit its price instead.";
            } else if (e.getErrorCode() == MYSQL_FOREIGN_KEY_FAIL) {
                message = "That product or store doesn't exist.";
            } else {
                log("Admin price change failed", e);
                message = "The change could not be saved, and nothing was changed. Try again in a moment.";
            }
        }
        HttpSession session = req.getSession();
        if (message != null) {
            session.setAttribute("adminFlash", message);
            session.setAttribute("adminFlashError", error);
        }
        String q = req.getParameter("q") == null ? "" : req.getParameter("q").trim();
        resp.sendRedirect(req.getContextPath() + "/admin/prices"
                + (q.isEmpty() ? "" : "?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8)));
    }

    /** Belt and braces: AuthFilter already blocks non-admins, but a missing filter must not open this up. */
    private static boolean requireAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null || !user.isAdmin()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Admins only");
            return false;
        }
        return true;
    }
}
