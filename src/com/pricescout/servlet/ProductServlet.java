package com.pricescout.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.pricescout.dao.AlertDAO;
import com.pricescout.dao.PriceHistoryDAO;
import com.pricescout.dao.ProductDetailDAO;
import com.pricescout.model.ProductDetail;
import com.pricescout.model.StoreHistory;
import com.pricescout.model.User;
import com.pricescout.util.InputUtil;

/**
 * Product detail page (FR3): /product?id=N. Open to guests (description, attributes, price
 * comparison). Price history and the price alert box are only loaded for signed-in customers.
 */
@WebServlet("/product")
public class ProductServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String VIEW = "/WEB-INF/views/product.jsp";

    private final ProductDetailDAO detailDAO = new ProductDetailDAO();
    private final PriceHistoryDAO historyDAO = new PriceHistoryDAO();
    private final AlertDAO alertDAO = new AlertDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer id = InputUtil.parseInt(req.getParameter("id"));
        if (id == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Product not found");
            return;
        }
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        try {
            ProductDetail product = detailDAO.find(id);
            if (product == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Product not found");
                return;
            }
            req.setAttribute("product", product);
            if (user != null) {
                List<StoreHistory> histories = historyDAO.getHistory(id);
                req.setAttribute("histories", histories);
                req.setAttribute("alert", alertDAO.find(user.getId(), id));
            }
        } catch (SQLException e) {
            log("Loading product failed", e);
            req.setAttribute("dbError", "Could not load this product. Try again in a moment.");
        }
        if (session != null) {
            req.setAttribute("flash", session.getAttribute("alertFlash"));
            session.removeAttribute("alertFlash");
        }
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }
}
