package com.pricescout.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Pattern;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.pricescout.dao.AdminUserDAO;
import com.pricescout.model.User;
import com.pricescout.model.UserSummary;

/**
 * Admin user management. AuthFilter blocks non-admins from every /admin URL.
 * Guard rails: admins can't delete or demote themselves, and the last admin can't be removed.
 */
@WebServlet({"/admin/users", "/admin/users/edit", "/admin/users/save", "/admin/users/delete"})
public class AdminUserServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String LIST_VIEW = "/WEB-INF/views/admin/users.jsp";
    private static final String FORM_VIEW = "/WEB-INF/views/admin/user-form.jsp";
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final AdminUserDAO dao = new AdminUserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        moveFlash(req);
        try {
            if ("/admin/users/edit".equals(req.getServletPath())) {
                UserSummary u = dao.getUser(parseInt(req.getParameter("id")));
                if (u == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found");
                    return;
                }
                showForm(req, resp, u, u.getName(), u.getEmail(), u.getRole(), null);
                return;
            }
            if (!"/admin/users".equals(req.getServletPath())) {
                resp.sendRedirect(req.getContextPath() + "/admin/users");
                return;
            }
            req.setAttribute("users", dao.listUsers());
            req.getRequestDispatcher(LIST_VIEW).forward(req, resp);
        } catch (SQLException e) {
            log("Admin users page failed", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String list = req.getContextPath() + "/admin/users";
        User me = (User) req.getSession().getAttribute("user");
        try {
            if ("/admin/users/delete".equals(req.getServletPath())) {
                int id = parseInt(req.getParameter("userId"));
                UserSummary target = dao.getUser(id);
                if (target == null) {
                    flash(req, "That user was already deleted.");
                } else if (target.getId() == me.getId()) {
                    flash(req, "You can't delete your own account while signed in as it.");
                } else if (target.isAdmin() && dao.countAdmins() <= 1) {
                    flash(req, "You can't delete the last admin.");
                } else {
                    dao.deleteUser(id);
                    flash(req, "Deleted " + target.getName() + " (" + target.getEmail() + ").");
                }
                resp.sendRedirect(list);
                return;
            }
            if (!"/admin/users/save".equals(req.getServletPath())) {
                resp.sendRedirect(list);
                return;
            }

            int id = parseInt(req.getParameter("userId"));
            UserSummary target = dao.getUser(id);
            if (target == null) {
                flash(req, "That user no longer exists.");
                resp.sendRedirect(list);
                return;
            }
            String name = LoginServlet.trim(req.getParameter("name"));
            String email = LoginServlet.trim(req.getParameter("email")).toLowerCase();
            String role = LoginServlet.trim(req.getParameter("role"));
            String password = req.getParameter("newPassword") == null ? "" : req.getParameter("newPassword");

            String error = null;
            if (name.isEmpty() || name.length() > 100) error = "Name is required (100 characters max).";
            else if (email.length() > 150 || !EMAIL.matcher(email).matches()) error = "Enter a valid email address.";
            else if (!"CUSTOMER".equals(role) && !"ADMIN".equals(role)) error = "Pick a role.";
            else if (!password.isEmpty() && (password.length() < 8 || password.length() > 128)) error = "New password must be 8 to 128 characters.";
            else if (target.getId() == me.getId() && !"ADMIN".equals(role)) error = "You can't remove your own admin role.";
            else if (target.isAdmin() && !"ADMIN".equals(role) && dao.countAdmins() <= 1) error = "You can't remove the last admin.";
            else if (dao.emailTakenByOther(email, id)) error = "Another account already uses that email.";

            if (error != null) {
                showForm(req, resp, target, name, email, role, error);
                return;
            }
            dao.updateUser(id, name, email, role, password.isEmpty() ? null : password);
            if (id == me.getId()) {
                req.getSession().setAttribute("user", new User(id, name, email, role));
            }
            flash(req, "Saved " + name + "." + (password.isEmpty() ? "" : " Password was reset."));
            resp.sendRedirect(list);
        } catch (SQLException e) {
            log("Admin user update failed", e);
            flash(req, "Database error: " + e.getMessage());
            resp.sendRedirect(list);
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, UserSummary target,
                          String name, String email, String role, String error) throws ServletException, IOException {
        req.setAttribute("target", target);
        req.setAttribute("name", name);
        req.setAttribute("email", email);
        req.setAttribute("role", role);
        req.setAttribute("error", error);
        req.getRequestDispatcher(FORM_VIEW).forward(req, resp);
    }

    private static void flash(HttpServletRequest req, String msg) {
        req.getSession().setAttribute("adminFlash", msg);
    }

    private static void moveFlash(HttpServletRequest req) {
        HttpSession s = req.getSession();
        req.setAttribute("flash", s.getAttribute("adminFlash"));
        s.removeAttribute("adminFlash");
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(LoginServlet.trim(s));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
