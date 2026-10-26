CREATE TABLE demo_product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type VARCHAR(40) NOT NULL,
    name VARCHAR(80) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_product_price CHECK (price BETWEEN 0.01 AND 99999999.99),
    CONSTRAINT chk_product_name CHECK (CHAR_LENGTH(TRIM(name)) BETWEEN 1 AND 80),
    CONSTRAINT chk_product_type CHECK (CHAR_LENGTH(TRIM(type)) BETWEEN 1 AND 40)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE demo_order_line (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NULL,
    product_name VARCHAR(80) NOT NULL,
    product_type VARCHAR(40) NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(16, 2) NOT NULL,
    source VARCHAR(16) NOT NULL DEFAULT 'CATALOG',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_product (order_id, product_id),
    CONSTRAINT fk_line_order FOREIGN KEY (order_id) REFERENCES demo_order(id),
    CONSTRAINT fk_line_product FOREIGN KEY (product_id) REFERENCES demo_product(id),
    CONSTRAINT chk_line_quantity CHECK (quantity BETWEEN 1 AND 1000),
    CONSTRAINT chk_line_price CHECK (unit_price BETWEEN 0.01 AND 99999999.99),
    CONSTRAINT chk_line_amount CHECK (line_total = unit_price * quantity),
    CONSTRAINT chk_line_source CHECK (
        (source = 'CATALOG' AND product_id IS NOT NULL AND product_type IS NOT NULL)
        OR (source = 'LEGACY' AND product_id IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO demo_order_line (order_id, product_id, product_name, product_type, unit_price, quantity, line_total, source)
SELECT id, NULL, item_name, NULL, unit_price, quantity, total_amount, 'LEGACY' FROM demo_order;

ALTER TABLE demo_order
    DROP CHECK chk_demo_order_quantity,
    DROP CHECK chk_demo_order_price,
    DROP COLUMN item_name,
    DROP COLUMN quantity,
    DROP COLUMN unit_price;
