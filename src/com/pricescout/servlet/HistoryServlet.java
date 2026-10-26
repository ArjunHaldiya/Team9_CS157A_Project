package com.pricescout.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.pricescout.dao.PriceHistoryDAO;
import com.pricescout.model.StoreHistory;

/** Price history is for signed-in users only; AuthFilter sends guests to the login page. */
@WebServlet("/history")
public class HistoryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String VIEW = "/WEB-INF/views/history.jsp";

    private final PriceHistoryDAO historyDAO = new PriceHistoryDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int productId;
        try {
            productId = Integer.parseInt(LoginServlet.trim(req.getParameter("productId")));
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Product not found");
            return;
        }
        try {
            String[] info = historyDAO.getProductInfo(productId);
            if (info == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Product not found");
                return;
            }
            List<StoreHistory> histories = historyDAO.getHistory(productId);
            String bestStore = null;
            for (StoreHistory h : histories) {
                if (bestStore == null || h.getCurrentPrice().compareTo(currentPrice(histories, bestStore)) < 0) {
                    bestStore = h.getStoreName();
                }
            }
            req.setAttribute("productName", info[0]);
            req.setAttribute("category", info[1]);
            req.setAttribute("histories", histories);
            req.setAttribute("bestStore", bestStore);
        } catch (SQLException e) {
            log("Loading price history failed", e);
            req.setAttribute("dbError", "Could not load price history. Try again in a moment.");
        }
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    private static java.math.BigDecimal currentPrice(List<StoreHistory> histories, String store) {
        for (StoreHistory h : histories) {
            if (h.getStoreName().equals(store)) return h.getCurrentPrice();
        }
        return null;
    }
}
