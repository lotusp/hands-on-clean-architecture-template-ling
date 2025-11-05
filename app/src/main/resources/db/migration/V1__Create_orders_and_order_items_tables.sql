-- Create orders table
CREATE TABLE orders (
    id VARCHAR(36) PRIMARY KEY,
    order_number VARCHAR(20) NOT NULL UNIQUE,
    user_id VARCHAR(36) NOT NULL,
    merchant_id VARCHAR(36) NOT NULL,
    
    -- Delivery information
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(11) NOT NULL,
    address VARCHAR(500) NOT NULL,
    
    -- Remark
    remark VARCHAR(200),
    
    -- Status
    status VARCHAR(20) NOT NULL,
    
    -- Pricing information
    items_total DECIMAL(10, 2) NOT NULL,
    packaging_fee DECIMAL(10, 2) NOT NULL,
    delivery_fee DECIMAL(10, 2) NOT NULL,
    final_amount DECIMAL(10, 2) NOT NULL,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    
    INDEX idx_user_id (user_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_order_number (order_number),
    INDEX idx_created_at (created_at)
);

-- Create order_items table
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    dish_id VARCHAR(36) NOT NULL,
    dish_name VARCHAR(200) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id)
);
