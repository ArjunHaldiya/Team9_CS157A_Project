package com.pricescout.servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.pricescout.dao.AlertDAO;
import com.pricescout.model.PriceAlert;
import com.pricescout.model.User;
import com.pricescout.util.InputUtil;

/**
 * Price drop alerts (FR7).
 *   GET  /watchlist         - the customer's alerts (fired ones first)
 *   POST /watchlist/set     - create or change the target price on a product
 *   POST /watchlist/delete  - remove an alert
 * AuthFilter already guarantees a signed-in user for every /watchlist URL.
 */
@WebServlet({"/watchlist", "/watchlist/set", "/watchlist/delete"})
public class WatchlistServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String VIEW = "/WEB-INF/views/watchlist.jsp";

    private final AlertDAO alertDAO = new AlertDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/watchlist".equals(req.getServletPath())) {
            resp.sendRedirect(req.getContextPath() + "/watchlist");
            return;
        }
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        req.setAttribute("flash", session.getAttribute("alertFlash"));
        session.removeAttribute("alertFlash");
        try {
            List<PriceAlert> alerts = alertDAO.findByUser(user.getId());
            req.setAttribute("alerts", alerts);
        } catch (SQLException e) {
            log("Loading alerts failed", e);
            req.setAttribute("dbError", "Could not load your alerts. Try again in a moment.");
        }
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        Integer productId = InputUtil.parseInt(req.getParameter("productId"));
        // "from=product" sends the customer back to the product page they came from.
        boolean fromProduct = "product".equals(req.getParameter("from")) && productId != null;
        String flash;
        try {
            switch (req.getServletPath()) {
                case "/watchlist/set": {
                    BigDecimal target = InputUtil.parsePrice(req.getParameter("targetPrice"));
                    if (productId == null) {
                        flash = "Sorry, that product isn't available.";
                    } else if (target == null) {
                        flash = "Enter a target price like 749.99 (up to 99,999,999.99).";
                    } else {
                        AlertDAO.SetResult result = alertDAO.setAlert(user.getId(), productId, target);
                        if (result == null) {
                            flash = "Sorry, that product isn't available.";
                        } else if (result.triggered) {
                            flash = "Alert saved at $" + target + ". Good news: " + result.lowestStore
                                  + " already has it for $" + result.lowestPrice + ".";
                        } else {
                            flash = "Alert saved. We'll flag it when any store reaches $" + target + " or less.";
                        }
                    }
                    break;
                }
                case "/watchlist/delete": {
                    Integer alertId = InputUtil.parseInt(req.getParameter("alertId"));
                    boolean removed = alertId != null && alertDAO.delete(user.getId(), alertId);
                    flash = removed ? "Alert removed." : "That alert no longer exists.";
                    break;
                }
                default:
                    flash = null;
            }
        } catch (SQLException e) {
            log("Alert update failed", e);
            flash = "Could not update your alert. Try again in a moment.";
        }
        if (flash != null) session.setAttribute("alertFlash", flash);
        resp.sendRedirect(req.getContextPath() + (fromProduct ? "/product?id=" + productId : "/watchlist"));
    }
}
