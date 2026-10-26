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

import com.pricescout.dao.CartDAO;
import com.pricescout.model.CartItem;
import com.pricescout.model.User;

/** AuthFilter already guarantees a signed-in user for every /cart URL. */
@WebServlet({"/cart", "/cart/add", "/cart/update", "/cart/remove"})
public class CartServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String VIEW = "/WEB-INF/views/cart.jsp";

    private final CartDAO cartDAO = new CartDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/cart".equals(req.getServletPath())) {
            resp.sendRedirect(req.getContextPath() + "/cart");
            return;
        }
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        req.setAttribute("flash", session.getAttribute("cartFlash"));
        session.removeAttribute("cartFlash");
        try {
            List<CartItem> items = cartDAO.getItems(user.getId());
            BigDecimal subtotal = BigDecimal.ZERO;
            int count = 0;
            for (CartItem item : items) {
                subtotal = subtotal.add(item.getLineTotal());
                count += item.getQuantity();
            }
            req.setAttribute("items", items);
            req.setAttribute("subtotal", subtotal);
            req.setAttribute("itemCount", count);
            session.setAttribute("cartCount", count);
        } catch (SQLException e) {
            log("Loading cart failed", e);
            req.setAttribute("dbError", "Could not load your cart. Try again in a moment.");
        }
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        String flash;
        try {
            switch (req.getServletPath()) {
                case "/cart/add": {
                    int productId = parseInt(req.getParameter("productId"), -1);
                    String store = LoginServlet.trim(req.getParameter("store"));
                    int qty = clamp(parseInt(req.getParameter("quantity"), 1));
                    String label = productId < 0 || store.isEmpty() ? null
                                 : cartDAO.addItem(user.getId(), productId, store, qty);
                    flash = label == null ? "Sorry, that item isn't available anymore." : "Added " + label + " to your cart.";
                    break;
                }
                case "/cart/update": {
                    int itemId = parseInt(req.getParameter("itemId"), -1);
                    int qty = parseInt(req.getParameter("quantity"), -1);
                    if (qty == 0) {
                        cartDAO.removeItem(user.getId(), itemId);
                        flash = "Item removed from your cart.";
                    } else if (qty < 0) {
                        flash = "Enter a quantity between 0 and " + CartDAO.MAX_QUANTITY + ".";
                    } else {
                        cartDAO.updateQuantity(user.getId(), itemId, clamp(qty));
                        flash = "Cart updated.";
                    }
                    break;
                }
                case "/cart/remove": {
                    cartDAO.removeItem(user.getId(), parseInt(req.getParameter("itemId"), -1));
                    flash = "Item removed from your cart.";
                    break;
                }
                default:
                    flash = null;
            }
            session.setAttribute("cartCount", cartDAO.countItems(user.getId()));
        } catch (SQLException e) {
            log("Cart update failed", e);
            flash = "Could not update your cart. Try again in a moment.";
        }
        if (flash != null) session.setAttribute("cartFlash", flash);
        resp.sendRedirect(req.getContextPath() + "/cart");
    }

    private static int parseInt(String s, int fallback) {
        try {
            return s == null ? fallback : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int clamp(int qty) {
        return Math.max(1, Math.min(qty, CartDAO.MAX_QUANTITY));
    }
}
