package com.pricescout.servlet;

import java.io.IOException;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.pricescout.dao.CartDAO;
import com.pricescout.dao.UserDAO;
import com.pricescout.model.User;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    static final String VIEW = "/WEB-INF/views/login.jsp";

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (currentUser(req) != null) {
            resp.sendRedirect(req.getContextPath() + "/");
            return;
        }
        req.setAttribute("next", req.getParameter("next"));
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String email = trim(req.getParameter("email")).toLowerCase();
        String password = req.getParameter("password") == null ? "" : req.getParameter("password");
        String next = req.getParameter("next");

        if (email.isEmpty() || password.isEmpty()) {
            fail(req, resp, "Enter your email and password.", email, next);
            return;
        }
        try {
            User user = userDAO.authenticate(email, password);
            if (user == null) {
                fail(req, resp, "Incorrect email or password.", email, next);
                return;
            }
            startSession(req, user);
            try {
                req.getSession().setAttribute("cartCount", new CartDAO().countItems(user.getId()));
            } catch (SQLException e) {
                log("Could not load cart count", e);
            }
            resp.sendRedirect(safeNext(req, next));
        } catch (SQLException e) {
            log("Login failed", e);
            fail(req, resp, "Could not reach the database. Try again in a moment.", email, next);
        }
    }

    private void fail(HttpServletRequest req, HttpServletResponse resp, String error, String email, String next)
            throws ServletException, IOException {
        req.setAttribute("error", error);
        req.setAttribute("email", email);
        req.setAttribute("next", next);
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    /** New session id on login prevents session fixation. */
    static void startSession(HttpServletRequest req, User user) {
        HttpSession session = req.getSession(true);
        req.changeSessionId();
        session.setAttribute("user", user);
        session.setAttribute("cartCount", 0);
    }

    static User currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }

    /** Only redirect back inside this app, never to another site. */
    static String safeNext(HttpServletRequest req, String next) {
        String home = req.getContextPath() + "/";
        if (next == null || next.isEmpty()) return home;
        if (!next.startsWith(home) || next.startsWith("//") || next.contains("\\")) return home;
        return next;
    }

    static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
