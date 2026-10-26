package com.pricescout.filter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.pricescout.model.User;

/**
 * Server-side role checks (not just hidden buttons):
 *  - cart, watchlist and price history need a signed-in user
 *  - /admin needs an ADMIN
 */
@WebFilter(urlPatterns = {"/cart", "/cart/*", "/watchlist", "/watchlist/*",
                          "/history", "/history/*", "/admin", "/admin/*"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

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
