-- =====================================================================
-- PriceScout database schema -- CS157A Project Data Model & DB Design
-- Full 11-relation schema; every table has >= 10 real-world rows
-- except stores, which intentionally holds exactly the 3 retailers
-- PriceScout compares (Best Buy, Target, Walmart).
-- Run this whole script in MySQL Workbench (File > Open SQL Script,
-- then the lightning-bolt Execute button) against a local MySQL 8 server.
-- =====================================================================

DROP DATABASE IF EXISTS pricescout;
CREATE DATABASE pricescout;
USE pricescout;

-- 1. Users
CREATE TABLE users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('CUSTOMER', 'ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Products (shared attributes; ISA superclass)
CREATE TABLE products (
    product_id   INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    image_url    VARCHAR(500)
);

-- 3. Electronics (ISA subclass of Products)
CREATE TABLE electronics (
    product_id      INT PRIMARY KEY,
    brand           VARCHAR(100),
    model_number    VARCHAR(100),
    warranty_months INT,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- 4. Household (ISA subclass of Products)
CREATE TABLE household (
    product_id INT PRIMARY KEY,
    brand      VARCHAR(100),
    material   VARCHAR(100),
    dimensions VARCHAR(100),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- 5. Books (ISA subclass of Products)
CREATE TABLE books (
    product_id INT PRIMARY KEY,
    author     VARCHAR(150),
    isbn       VARCHAR(20),
    publisher  VARCHAR(150),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- 6. Stores
CREATE TABLE stores (
    store_id   INT AUTO_INCREMENT PRIMARY KEY,
    store_name VARCHAR(100) NOT NULL UNIQUE
);

-- 7. PriceListing (associative entity for the M:N 'Listed At' relationship)
CREATE TABLE price_listing (
    listing_id   INT AUTO_INCREMENT PRIMARY KEY,
    product_id   INT NOT NULL,
    store_id     INT NOT NULL,
    price        DECIMAL(10, 2) NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    product_url  VARCHAR(500),
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    FOREIGN KEY (store_id) REFERENCES stores(store_id),
    UNIQUE KEY unique_product_store (product_id, store_id)
);

-- 8. PriceHistory (weak entity; identifying owner = PriceListing)
CREATE TABLE price_history (
    history_id  INT AUTO_INCREMENT PRIMARY KEY,
    listing_id  INT NOT NULL,
    price       DECIMAL(10, 2) NOT NULL,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (listing_id) REFERENCES price_listing(listing_id)
);

-- 9. Cart (1:1 with Users)
CREATE TABLE cart (
    cart_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 10. CartItem (weak entity; identifying owner = Cart)
CREATE TABLE cart_item (
    cart_item_id INT AUTO_INCREMENT PRIMARY KEY,
    cart_id      INT NOT NULL,
    listing_id   INT NOT NULL,
    quantity     INT NOT NULL DEFAULT 1,
    added_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cart_id) REFERENCES cart(cart_id),
    FOREIGN KEY (listing_id) REFERENCES price_listing(listing_id),
    UNIQUE KEY unique_cart_listing (cart_id, listing_id)
);

-- 11. PriceAlert (associative entity for the M:N 'Watches' relationship)
CREATE TABLE price_alert (
    alert_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    product_id   INT NOT NULL,
    target_price DECIMAL(10, 2) NOT NULL,
    triggered    BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    UNIQUE KEY unique_user_product_alert (user_id, product_id)
);

-- ============================= Seed data =============================

-- 12 rows
INSERT INTO users (name, email, password_hash, role) VALUES
    ('Arjun Haldiya', 'arjun.haldiya@sjsu.edu', '$2b$12$48eb46c86dd877a095650322868c261b6ceb47812ddd275f9f5ff', 'CUSTOMER'),
    ('Priya Natarajan', 'priya.n@sjsu.edu', '$2b$12$bf666f6caae91011ec1e584b4fa2c5a94f4ba7168523566fd4613', 'CUSTOMER'),
    ('Miguel Torres', 'miguel.torres@sjsu.edu', '$2b$12$06f256559440ea3d8fa809ac51ffac96fc2139f273a482c54849a', 'CUSTOMER'),
    ('Sarah Kim', 'sarah.kim@sjsu.edu', '$2b$12$2cbcc84cb364a162d33bdf11810f83fb0dfe9e972946d3c6b9dea', 'CUSTOMER'),
    ('Daniel Nguyen', 'daniel.nguyen@sjsu.edu', '$2b$12$e0c57bb58a6805f37e27bf86a9faab7de8d577a854fda1659741b', 'CUSTOMER'),
    ('Fatima Rahman', 'fatima.rahman@sjsu.edu', '$2b$12$adc75f62695145356e847808155a65f18ecf2c007d83f5b0beb0f', 'CUSTOMER'),
    ('Ethan Brooks', 'ethan.brooks@sjsu.edu', '$2b$12$c61640278418e215890bec1d626c9025682b1696395c43592a974', 'CUSTOMER'),
    ('Grace Lee', 'grace.lee@sjsu.edu', '$2b$12$9996c15a6e9cb381489f93da7cbac848807690dbfb79ad6172f55', 'CUSTOMER'),
    ('Omar Haddad', 'omar.haddad@sjsu.edu', '$2b$12$4bf2b5f69f2ae668e9adef49e2c1e0c829928403090c62c0137e1', 'CUSTOMER'),
    ('Isabella Ruiz', 'isabella.ruiz@sjsu.edu', '$2b$12$3423d88742d994410337834b10c97b573383ea2bc1e46062807d3', 'CUSTOMER'),
    ('Jordan Avery', 'jordan.avery@pricescout.io', '$2b$12$6ff2935c4ce90cfe320dbef38978d877e5216ec6d66b5cc0fbcef', 'ADMIN'),
    ('Taylor Whitfield', 'taylor.whitfield@pricescout.io', '$2b$12$75e847658806a3aca169bc3b0335d7419c67b5f6c0de6ff46a6e9', 'ADMIN');

-- 36 rows
INSERT INTO products (name, description) VALUES
    ('iPhone 15', '128GB, colors vary by retailer'),
    ('Samsung Galaxy S24', '256GB unlocked smartphone'),
    ('Sony WH-1000XM5 Headphones', 'Noise-cancelling over-ear headphones'),
    ('Apple Watch Series 9', 'GPS, 45mm aluminum case'),
    ('iPad Air (5th Gen)', '10.9-inch, Wi-Fi, 64GB'),
    ('Dell XPS 13 Laptop', '13.4-inch FHD+, Intel Core i7, 16GB RAM'),
    ('Nintendo Switch OLED', '7-inch OLED screen console'),
    ('Bose SoundLink Flex', 'Portable Bluetooth speaker'),
    ('Canon EOS R50 Camera', 'Mirrorless camera with 18-45mm lens'),
    ('Amazon Echo Dot (5th Gen)', 'Smart speaker with Alexa'),
    ('Keurig K-Classic Coffee Maker', 'Single-serve K-Cup pod coffee maker'),
    ('Dyson V8 Cordless Vacuum', 'Lightweight cordless stick vacuum'),
    ('Instant Pot Duo 7-in-1', '6-quart electric pressure cooker'),
    ('Ninja Air Fryer Max XL', '5.5-quart digital air fryer'),
    ('KitchenAid Stand Mixer', '5-quart tilt-head stand mixer'),
    ('iRobot Roomba 694', 'Wi-Fi connected robot vacuum'),
    ('Vitamix 5200 Blender', '64 oz professional-grade blender'),
    ('Cuisinart Toaster Oven', '0.6 cu ft convection toaster oven'),
    ('Honeywell HPA300 Air Purifier', 'True HEPA air purifier, large rooms'),
    ('Lodge Cast Iron Skillet', '12-inch pre-seasoned cast iron skillet'),
    ('Atomic Habits', 'By James Clear'),
    ('The Pragmatic Programmer', '20th Anniversary Edition'),
    ('Clean Code', 'A Handbook of Agile Software Craftsmanship'),
    ('Sapiens', 'A Brief History of Humankind'),
    ('The Midnight Library', 'A novel'),
    ('Project Hail Mary', 'A novel'),
    ('Educated', 'A Memoir'),
    ('Designing Data-Intensive Applications', 'The Big Ideas Behind Reliable Systems'),
    ('The Lean Startup', 'How Constant Innovation Creates Radically Successful Businesses'),
    ('1984', 'A novel'),
    ('Levi''s 501 Original Jeans', 'Men''s straight fit jeans'),
    ('Nike Dri-FIT T-Shirt', 'Men''s training t-shirt'),
    ('LEGO Star Wars Millennium Falcon', '1,353-piece building set'),
    ('Catan Board Game', 'Classic strategy board game, 3-4 players'),
    ('Yeti Rambler 20oz Tumbler', 'Stainless steel insulated tumbler'),
    ('Kirkland Signature Organic Coffee', '2 lb whole bean, Colombian');

-- 10 rows
INSERT INTO electronics (product_id, brand, model_number, warranty_months) VALUES
    (1, 'Apple', 'A3090', 12),
    (2, 'Samsung', 'SM-S921U', 12),
    (3, 'Sony', 'WH1000XM5', 12),
    (4, 'Apple', 'A2984', 12),
    (5, 'Apple', 'A2588', 12),
    (6, 'Dell', 'XPS9340', 12),
    (7, 'Nintendo', 'HEG-001', 12),
    (8, 'Bose', 'SLFLEX2', 12),
    (9, 'Canon', 'EOSR50', 12),
    (10, 'Amazon', 'ECHODOT5', 12);

-- 10 rows
INSERT INTO household (product_id, brand, material, dimensions) VALUES
    (11, 'Keurig', 'Plastic/Metal', '13.3 x 9.8 x 13 in'),
    (12, 'Dyson', 'Plastic/Metal', '9.8 x 9.8 x 49.2 in'),
    (13, 'Instant Pot', 'Stainless Steel', '13.4 x 12.2 x 12.5 in'),
    (14, 'Ninja', 'Plastic/Metal', '11.5 x 11.9 x 12 in'),
    (15, 'KitchenAid', 'Metal', '14.1 x 8.7 x 14 in'),
    (16, 'iRobot', 'Plastic', '13.3 x 13.3 x 3.6 in'),
    (17, 'Vitamix', 'Plastic/Stainless Steel', '8.75 x 7.25 x 20.5 in'),
    (18, 'Cuisinart', 'Stainless Steel', '18.5 x 15.6 x 11 in'),
    (19, 'Honeywell', 'Plastic', '20.2 x 10.4 x 22.4 in'),
    (20, 'Lodge', 'Cast Iron', '12 x 12 x 2 in');

-- 10 rows
INSERT INTO books (product_id, author, isbn, publisher) VALUES
    (21, 'James Clear', '978-0735211292', 'Avery'),
    (22, 'David Thomas & Andrew Hunt', '978-0135957059', 'Addison-Wesley'),
    (23, 'Robert C. Martin', '978-0132350884', 'Prentice Hall'),
    (24, 'Yuval Noah Harari', '978-0062316097', 'Harper'),
    (25, 'Matt Haig', '978-0525559474', 'Viking'),
    (26, 'Andy Weir', '978-0593135204', 'Ballantine Books'),
    (27, 'Tara Westover', '978-0399590504', 'Random House'),
    (28, 'Martin Kleppmann', '978-1449373320', 'O''Reilly Media'),
    (29, 'Eric Ries', '978-0307887894', 'Crown Business'),
    (30, 'George Orwell', '978-0451524935', 'Signet Classics');

-- 3 rows
INSERT INTO stores (store_name) VALUES
    ('Best Buy'),
    ('Target'),
    ('Walmart');

-- 66 rows
INSERT INTO price_listing (product_id, store_id, price, product_url) VALUES
    (1, 1, 799.00, 'https://best-buy.com/p/iphone-15'),
    (1, 2, 807.00, 'https://target.com/p/iphone-15'),
    (2, 1, 799.99, 'https://best-buy.com/p/samsung-galaxy-s24'),
    (2, 2, 807.99, 'https://target.com/p/samsung-galaxy-s24'),
    (3, 1, 349.99, 'https://best-buy.com/p/sony-wh-1000xm5-headphones'),
    (3, 2, 357.99, 'https://target.com/p/sony-wh-1000xm5-headphones'),
    (4, 1, 399.00, 'https://best-buy.com/p/apple-watch-series-9'),
    (4, 2, 407.00, 'https://target.com/p/apple-watch-series-9'),
    (5, 1, 599.00, 'https://best-buy.com/p/ipad-air-5th-gen'),
    (5, 2, 607.00, 'https://target.com/p/ipad-air-5th-gen'),
    (6, 1, 1099.00, 'https://best-buy.com/p/dell-xps-13-laptop'),
    (6, 2, 1107.00, 'https://target.com/p/dell-xps-13-laptop'),
    (7, 1, 349.99, 'https://best-buy.com/p/nintendo-switch-oled'),
    (7, 2, 357.99, 'https://target.com/p/nintendo-switch-oled'),
    (8, 1, 149.00, 'https://best-buy.com/p/bose-soundlink-flex'),
    (8, 2, 157.00, 'https://target.com/p/bose-soundlink-flex'),
    (9, 1, 799.00, 'https://best-buy.com/p/canon-eos-r50-camera'),
    (9, 2, 807.00, 'https://target.com/p/canon-eos-r50-camera'),
    (10, 1, 49.99, 'https://best-buy.com/p/amazon-echo-dot-5th-gen'),
    (10, 2, 57.99, 'https://target.com/p/amazon-echo-dot-5th-gen'),
    (11, 2, 86.99, 'https://target.com/p/keurig-k-classic-coffee-maker'),
    (11, 3, 91.99, 'https://walmart.com/p/keurig-k-classic-coffee-maker'),
    (12, 2, 376.99, 'https://target.com/p/dyson-v8-cordless-vacuum'),
    (12, 3, 381.99, 'https://walmart.com/p/dyson-v8-cordless-vacuum'),
    (13, 2, 96.00, 'https://target.com/p/instant-pot-duo-7-in-1'),
    (13, 3, 101.00, 'https://walmart.com/p/instant-pot-duo-7-in-1'),
    (14, 2, 126.99, 'https://target.com/p/ninja-air-fryer-max-xl'),
    (14, 3, 131.99, 'https://walmart.com/p/ninja-air-fryer-max-xl'),
    (15, 2, 376.99, 'https://target.com/p/kitchenaid-stand-mixer'),
    (15, 3, 381.99, 'https://walmart.com/p/kitchenaid-stand-mixer'),
    (16, 2, 271.99, 'https://target.com/p/irobot-roomba-694'),
    (16, 3, 276.99, 'https://walmart.com/p/irobot-roomba-694'),
    (17, 2, 446.95, 'https://target.com/p/vitamix-5200-blender'),
    (17, 3, 451.95, 'https://walmart.com/p/vitamix-5200-blender'),
    (18, 2, 96.95, 'https://target.com/p/cuisinart-toaster-oven'),
    (18, 3, 101.95, 'https://walmart.com/p/cuisinart-toaster-oven'),
    (19, 2, 246.99, 'https://target.com/p/honeywell-hpa300-air-purifier'),
    (19, 3, 251.99, 'https://walmart.com/p/honeywell-hpa300-air-purifier'),
    (20, 2, 31.90, 'https://target.com/p/lodge-cast-iron-skillet'),
    (20, 3, 36.90, 'https://walmart.com/p/lodge-cast-iron-skillet'),
    (21, 3, 11.99, 'https://walmart.com/p/atomic-habits'),
    (21, 2, 10.49, 'https://target.com/p/atomic-habits'),
    (22, 3, 44.99, 'https://walmart.com/p/the-pragmatic-programmer'),
    (22, 2, 43.49, 'https://target.com/p/the-pragmatic-programmer'),
    (23, 3, 39.99, 'https://walmart.com/p/clean-code'),
    (23, 2, 38.49, 'https://target.com/p/clean-code'),
    (24, 3, 16.99, 'https://walmart.com/p/sapiens'),
    (24, 2, 15.49, 'https://target.com/p/sapiens'),
    (25, 3, 13.99, 'https://walmart.com/p/the-midnight-library'),
    (25, 2, 12.49, 'https://target.com/p/the-midnight-library'),
    (26, 3, 18.00, 'https://walmart.com/p/project-hail-mary'),
    (26, 2, 16.50, 'https://target.com/p/project-hail-mary'),
    (27, 3, 14.99, 'https://walmart.com/p/educated'),
    (27, 2, 13.49, 'https://target.com/p/educated'),
    (28, 3, 49.99, 'https://walmart.com/p/designing-data-intensive-applications'),
    (28, 2, 48.49, 'https://target.com/p/designing-data-intensive-applications'),
    (29, 3, 17.00, 'https://walmart.com/p/the-lean-startup'),
    (29, 2, 15.50, 'https://target.com/p/the-lean-startup'),
    (30, 3, 9.99, 'https://walmart.com/p/1984'),
    (30, 2, 8.49, 'https://target.com/p/1984'),
    (31, 3, 69.50, 'https://walmart.com/p/levi-s-501-original-jeans'),
    (32, 2, 28.00, 'https://target.com/p/nike-dri-fit-t-shirt'),
    (33, 3, 169.99, 'https://walmart.com/p/lego-star-wars-millennium-falcon'),
    (34, 2, 44.99, 'https://target.com/p/catan-board-game'),
    (35, 3, 35.00, 'https://walmart.com/p/yeti-rambler-20oz-tumbler'),
    (36, 2, 14.49, 'https://target.com/p/kirkland-signature-organic-coffee');

-- 30 rows
INSERT INTO price_history (listing_id, price, recorded_at) VALUES
    (1, 918.85, DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (1, 846.94, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (2, 928.05, DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (2, 855.42, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (3, 919.99, DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (3, 847.99, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (4, 929.19, DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (4, 856.47, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (5, 402.49, DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (5, 370.99, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (6, 411.69, DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (6, 379.47, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (7, 458.85, DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (7, 422.94, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (8, 468.05, DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (8, 431.42, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (9, 688.85, DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (9, 634.94, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (10, 698.05, DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (10, 643.42, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (11, 1263.85, DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (11, 1164.94, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (12, 1273.05, DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (12, 1173.42, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (13, 402.49, DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (13, 370.99, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (14, 411.69, DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (14, 379.47, DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (15, 171.35, DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (15, 157.94, DATE_SUB(NOW(), INTERVAL 30 DAY));

-- 10 rows
INSERT INTO cart (user_id) VALUES
    (1),
    (2),
    (3),
    (4),
    (5),
    (6),
    (7),
    (8),
    (9),
    (10);

-- 18 rows
INSERT INTO cart_item (cart_id, listing_id, quantity) VALUES
    (1, 1, 1),
    (1, 41, 2),
    (2, 6, 1),
    (2, 22, 1),
    (3, 9, 1),
    (4, 27, 1),
    (4, 45, 1),
    (5, 13, 1),
    (5, 4, 2),
    (6, 32, 1),
    (6, 52, 1),
    (7, 7, 1),
    (7, 63, 1),
    (8, 35, 1),
    (9, 57, 1),
    (9, 18, 1),
    (10, 40, 2),
    (10, 65, 1);

-- 12 rows
INSERT INTO price_alert (user_id, product_id, target_price) VALUES
    (1, 1, 750.00),
    (1, 24, 15.00),
    (2, 3, 320.00),
    (3, 12, 380.00),
    (4, 6, 999.00),
    (5, 27, 12.00),
    (6, 15, 300.00),
    (7, 8, 100.00),
    (8, 19, 220.00),
    (9, 10, 40.00),
    (10, 30, 8.00),
    (3, 4, 350.00);

-- ===================================================================
-- Row-count sanity check: every table should return >= 10, except
-- stores (exactly 3 by design).
-- ===================================================================
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'electronics', COUNT(*) FROM electronics
UNION ALL SELECT 'household', COUNT(*) FROM household
UNION ALL SELECT 'books', COUNT(*) FROM books
UNION ALL SELECT 'stores', COUNT(*) FROM stores
UNION ALL SELECT 'price_listing', COUNT(*) FROM price_listing
UNION ALL SELECT 'price_history', COUNT(*) FROM price_history
UNION ALL SELECT 'cart', COUNT(*) FROM cart
UNION ALL SELECT 'cart_item', COUNT(*) FROM cart_item
UNION ALL SELECT 'price_alert', COUNT(*) FROM price_alert
;
