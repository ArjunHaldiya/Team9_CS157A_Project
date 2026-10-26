package com.pricescout.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.pricescout.dao.ProductDAO;
import com.pricescout.model.Product;

/**
 * Handles the Guest homepage: initial load, keyword search, and
 * category filtering. Forwards results to index.jsp for rendering.
 * This is the Application (Web) layer piece of the 3-tier design --
 * it talks to the DAO (Database layer) and never touches JDBC itself.
 */
@WebServlet({"", "/search"})
public class HomeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String keyword = request.getParameter("q");
        String category = request.getParameter("category");

        try {
            List<Product> products = productDAO.search(keyword, category);
            List<String> categories = productDAO.getCategories();

            request.setAttribute("products", products);
            request.setAttribute("categories", categories);
            request.setAttribute("selectedCategory", category == null ? "All" : category);
            request.setAttribute("keyword", keyword == null ? "" : keyword);

        } catch (SQLException e) {
            // Keep the page usable even if the DB isn't reachable yet.
            request.setAttribute("dbError",
                "Could not load products from the database. Check that MySQL is running "
                + "and the pricescout schema is loaded. (" + e.getMessage() + ")");
        }

        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}
