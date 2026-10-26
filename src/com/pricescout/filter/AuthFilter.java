package com.pricescout.filter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.pricescout.dao.UserDAO;
import com.pricescout.model.User;

/**
 * Server-side role checks (not just hidden buttons):
 *  - cart, watchlist and price history need a signed-in user
 *  - /admin needs an ADMIN
 * The user is re-read from the database on every protected request, so a role change or
 * account deletion by an admin takes effect immediately, not at the user's next login.
 */
@WebFilter(urlPatterns = {"/cart", "/cart/*", "/watchlist", "/watchlist/*",
                          "/history", "/history/*", "/admin", "/admin/*"})
public class AuthFilter implements Filter {

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user != null) {
            try {
                User fresh = userDAO.findById(user.getId());
                if (fresh == null) {            // account was deleted
                    session.invalidate();
                    user = null;
                } else {
                    session.setAttribute("user", fresh);
                    user = fresh;
                }
            } catch (SQLException e) {
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Database unavailable");
                return;
            }
        }
        if (user == null) {
            String target = req.getRequestURI() + (req.getQueryString() == null ? "" : "?" + req.getQueryString());
            resp.sendRedirect(req.getContextPath() + "/login?next=" + URLEncoder.encode(target, StandardCharsets.UTF_8));
            return;
        }
        if (req.getServletPath().startsWith("/admin") && !user.isAdmin()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Admins only");
            return;
        }
        chain.doFilter(request, response);
    }
}
