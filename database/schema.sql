CREATE DATABASE IF NOT EXISTS shopdb;
USE shopdb;

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role ENUM('CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
  created_at DATETIME
);

CREATE TABLE products (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(2000) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  stock INT NOT NULL,
  category VARCHAR(255) NOT NULL,
  image_url VARCHAR(1000),
  active BIT NOT NULL DEFAULT 1,
  version BIGINT,
  INDEX idx_product_category (category),
  INDEX idx_product_name (name)
);

CREATE TABLE orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  status ENUM('PENDING','PAID','SHIPPED','DELIVERED','CANCELLED') NOT NULL,
  total DECIMAL(12,2) NOT NULL,
  shipping_address VARCHAR(1000),
  gateway_order_id VARCHAR(255),
  payment_ref VARCHAR(255),
  created_at DATETIME,
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_order_user (user_id),
  INDEX idx_order_status (status),
  INDEX idx_order_created (created_at)
);

CREATE TABLE order_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  FOREIGN KEY (order_id) REFERENCES orders(id),
  FOREIGN KEY (product_id) REFERENCES products(id)
);