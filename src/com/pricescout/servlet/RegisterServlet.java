package com.pricescout.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.regex.Pattern;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.pricescout.dao.UserDAO;
import com.pricescout.model.User;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String VIEW = "/WEB-INF/views/register.jsp";
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 128;

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (LoginServlet.currentUser(req) != null) {
            resp.sendRedirect(req.getContextPath() + "/");
            return;
        }
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String name = LoginServlet.trim(req.getParameter("name"));
        String email = LoginServlet.trim(req.getParameter("email")).toLowerCase();
        String password = req.getParameter("password") == null ? "" : req.getParameter("password");
        String confirm = req.getParameter("confirm") == null ? "" : req.getParameter("confirm");

        String error = validate(name, email, password, confirm);
        if (error != null) {
            fail(req, resp, error, name, email);
            return;
        }
        try {
            if (userDAO.emailExists(email)) {
                fail(req, resp, "An account with that email already exists. Try signing in.", name, email);
                return;
            }
            User user = userDAO.register(name, email, password);
            LoginServlet.startSession(req, user);
            resp.sendRedirect(req.getContextPath() + "/");
        } catch (SQLIntegrityConstraintViolationException e) {
            fail(req, resp, "An account with that email already exists. Try signing in.", name, email);
        } catch (SQLException e) {
            log("Registration failed", e);
            fail(req, resp, "Could not reach the database. Try again in a moment.", name, email);
        }
    }

    private static String validate(String name, String email, String password, String confirm) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) return "Fill in your name, email and password.";
        if (name.length() > 100) return "Name must be 100 characters or fewer.";
        if (email.length() > 150 || !EMAIL.matcher(email).matches()) return "Enter a valid email address.";
        if (password.length() < MIN_PASSWORD) return "Password must be at least " + MIN_PASSWORD + " characters.";
        if (password.length() > MAX_PASSWORD) return "Password must be " + MAX_PASSWORD + " characters or fewer.";
        if (!password.equals(confirm)) return "Passwords don't match.";
        return null;
    }

    private void fail(HttpServletRequest req, HttpServletResponse resp, String error, String name, String email)
            throws ServletException, IOException {
        req.setAttribute("error", error);
        req.setAttribute("name", name);
        req.setAttribute("email", email);
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }
}
