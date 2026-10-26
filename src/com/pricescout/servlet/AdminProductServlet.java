package com.pricescout.servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.pricescout.dao.AdminProductDAO;
import com.pricescout.model.Listing;
import com.pricescout.model.ProductForm;
import com.pricescout.model.Store;

/** Admin product and price management. AuthFilter blocks non-admins from every /admin URL. */
@WebServlet({"/admin", "/admin/products", "/admin/products/new", "/admin/products/edit",
             "/admin/products/save", "/admin/products/delete", "/admin/prices/save"})
public class AdminProductServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String LIST_VIEW = "/WEB-INF/views/admin/products.jsp";
    private static final String FORM_VIEW = "/WEB-INF/views/admin/product-form.jsp";
    private static final BigDecimal MAX_PRICE = new BigDecimal("99999999.99");

    private final AdminProductDAO dao = new AdminProductDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ctx = req.getContextPath();
        moveFlash(req);
        try {
            switch (req.getServletPath()) {
                case "/admin/products":
                    req.setAttribute("products", dao.listProducts());
                    req.setAttribute("stores", dao.listStores());
                    req.getRequestDispatcher(LIST_VIEW).forward(req, resp);
                    return;
                case "/admin/products/new":
                    showForm(req, resp, new ProductForm(), null);
                    return;
                case "/admin/products/edit": {
                    ProductForm form = dao.getProduct(parseInt(req.getParameter("id")));
                    if (form == null) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Product not found");
                        return;
                    }
                    showForm(req, resp, form, null);
                    return;
                }
                default:
                    resp.sendRedirect(ctx + "/admin/products");
            }
        } catch (SQLException e) {
            log("Admin page failed", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String ctx = req.getContextPath();
        try {
            switch (req.getServletPath()) {
                case "/admin/products/save":
                    saveProduct(req, resp);
                    return;
                case "/admin/prices/save":
                    savePrices(req, resp);
                    return;
                case "/admin/products/delete": {
                    String name = dao.deleteProduct(parseInt(req.getParameter("productId")));
                    flash(req, name == null ? "That product was already deleted." : "Deleted \"" + name + "\".");
                    resp.sendRedirect(ctx + "/admin/products");
                    return;
                }
                default:
                    resp.sendRedirect(ctx + "/admin/products");
            }
        } catch (SQLException e) {
            log("Admin update failed", e);
            flash(req, "Database error: " + e.getMessage());
            resp.sendRedirect(ctx + "/admin/products");
        }
    }

    private void saveProduct(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        ProductForm f = new ProductForm();
        f.setProductId(Math.max(0, parseInt(req.getParameter("productId"))));
        f.setName(param(req, "name"));
        f.setDescription(param(req, "description"));
        f.setImageUrl(param(req, "imageUrl"));
        f.setCategory(param(req, "category"));
        f.setBrand("Household".equals(f.getCategory()) ? param(req, "h_brand") : param(req, "e_brand"));
        f.setModelNumber(param(req, "modelNumber"));
        f.setMaterial(param(req, "material"));
        f.setDimensions(param(req, "dimensions"));
        f.setAuthor(param(req, "author"));
        f.setIsbn(param(req, "isbn"));
        f.setPublisher(param(req, "publisher"));

        String error = null;
        String warranty = param(req, "warrantyMonths");
        if (!warranty.isEmpty()) {
            try {
                int w = Integer.parseInt(warranty);
                if (w < 0 || w > 240) error = "Warranty must be between 0 and 240 months.";
                else f.setWarrantyMonths(w);
            } catch (NumberFormatException e) {
                error = "Warranty must be a whole number of months.";
            }
        }
        if (f.getName().isEmpty()) error = "Product name is required.";
        else if (f.getName().length() > 200) error = "Product name must be 200 characters or fewer.";
        else if (f.getDescription().length() > 2000) error = "Description must be 2000 characters or fewer.";
        else if (!f.getImageUrl().isEmpty() && !isHttpUrl(f.getImageUrl(), 500)) error = "Image URL must start with http:// or https:// (max 500 characters).";
        else if (!Arrays.asList(ProductForm.CATEGORIES).contains(f.getCategory())) error = "Pick a category.";
        else if (f.getBrand().length() > 100 || f.getModelNumber().length() > 100 || f.getMaterial().length() > 100
                 || f.getDimensions().length() > 100) error = "Brand, model, material and dimensions must be 100 characters or fewer.";
        else if (f.getAuthor().length() > 150 || f.getPublisher().length() > 150) error = "Author and publisher must be 150 characters or fewer.";
        else if (f.getIsbn().length() > 20) error = "ISBN must be 20 characters or fewer.";

        if (error != null) {
            showForm(req, resp, f, error);
            return;
        }
        boolean wasNew = f.isNew();
        int id = dao.saveProduct(f);
        if (id < 0) {
            flash(req, "That product no longer exists.");
            resp.sendRedirect(req.getContextPath() + "/admin/products");
            return;
        }
        flash(req, wasNew ? "Created \"" + f.getName() + "\". Add store prices below so it shows up in the catalog."
                          : "Saved \"" + f.getName() + "\".");
        resp.sendRedirect(req.getContextPath() + "/admin/products/edit?id=" + id);
    }

    private void savePrices(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        int productId = parseInt(req.getParameter("productId"));
        String back = req.getContextPath() + "/admin/products/edit?id=" + productId;
        if (dao.getProduct(productId) == null) {
            flash(req, "That product no longer exists.");
            resp.sendRedirect(req.getContextPath() + "/admin/products");
            return;
        }
        List<Listing> wanted = new ArrayList<>();
        for (Store s : dao.listStores()) {
            String raw = param(req, "price_" + s.getId()).replace("$", "").replace(",", "");
            String url = param(req, "url_" + s.getId());
            BigDecimal price = null;
            if (!raw.isEmpty()) {
                try {
                    price = new BigDecimal(raw);
                } catch (NumberFormatException e) {
                    flash(req, s.getName() + ": \"" + raw + "\" isn't a valid price. Nothing was saved.");
                    resp.sendRedirect(back);
                    return;
                }
                if (price.signum() <= 0 || price.scale() > 2 || price.compareTo(MAX_PRICE) > 0) {
                    flash(req, s.getName() + ": price must be more than $0 with at most 2 decimal places. Nothing was saved.");
                    resp.sendRedirect(back);
                    return;
                }
            }
            if (!url.isEmpty() && !isHttpUrl(url, 500)) {
                flash(req, s.getName() + ": product URL must start with http:// or https://. Nothing was saved.");
                resp.sendRedirect(back);
                return;
            }
            wanted.add(new Listing(0, s.getId(), price, url));
        }
        int[] r = dao.savePrices(productId, wanted);
        flash(req, r[0] + r[1] + r[2] == 0 ? "No price changes."
                : "Prices saved: " + r[0] + " added, " + r[1] + " updated, " + r[2] + " removed.");
        resp.sendRedirect(back);
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, ProductForm form, String error)
            throws SQLException, ServletException, IOException {
        req.setAttribute("form", form);
        req.setAttribute("error", error);
        req.setAttribute("stores", dao.listStores());
        if (!form.isNew()) req.setAttribute("listings", dao.getListings(form.getProductId()));
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

    private static String param(HttpServletRequest req, String name) {
        return LoginServlet.trim(req.getParameter(name));
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(LoginServlet.trim(s));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static boolean isHttpUrl(String s, int max) {
        return s.length() <= max && (s.startsWith("http://") || s.startsWith("https://")) && !s.contains(" ");
    }
}
