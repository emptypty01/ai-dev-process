CREATE TABLE demo_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(40) NOT NULL,
    item_name VARCHAR(80) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_amount DECIMAL(16, 2) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_demo_order_no (order_no),
    CONSTRAINT chk_demo_order_quantity CHECK (quantity BETWEEN 1 AND 1000),
    CONSTRAINT chk_demo_order_price CHECK (unit_price BETWEEN 0.01 AND 99999999.99),
    CONSTRAINT chk_demo_order_status CHECK (status IN ('NEW', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
