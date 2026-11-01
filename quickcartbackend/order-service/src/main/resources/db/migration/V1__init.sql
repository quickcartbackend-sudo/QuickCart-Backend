CREATE SEQUENCE order_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE carts (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL,   -- no FK: belongs to user-service
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_carts_user_id UNIQUE (user_id)
);

CREATE TABLE cart_items (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cart_id    BIGINT  NOT NULL,
    product_id BIGINT  NOT NULL,       -- no FK: belongs to catalog-service
    quantity   INTEGER NOT NULL,
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT uq_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT ck_cart_items_quantity CHECK (quantity BETWEEN 1 AND 20)
);

CREATE TABLE orders (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_number            VARCHAR(20)    NOT NULL,
    user_id                 BIGINT         NOT NULL,
    status                  VARCHAR(20)    NOT NULL,
    subtotal                NUMERIC(10, 2) NOT NULL,
    delivery_fee            NUMERIC(10, 2) NOT NULL,
    total                   NUMERIC(10, 2) NOT NULL,
    payment_method          VARCHAR(20)    NOT NULL,
    payment_status          VARCHAR(20)    NOT NULL,
    delivery_recipient_name VARCHAR(100)   NOT NULL,
    delivery_phone          VARCHAR(10)    NOT NULL,
    delivery_line1          VARCHAR(150)   NOT NULL,
    delivery_line2          VARCHAR(150),
    delivery_city           VARCHAR(60)    NOT NULL,
    delivery_state          VARCHAR(60)    NOT NULL,
    delivery_pincode        VARCHAR(6)     NOT NULL,
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_orders_order_number UNIQUE (order_number),
    CONSTRAINT ck_orders_status CHECK (status IN
        ('PLACED', 'CONFIRMED', 'PACKED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT ck_orders_payment_method CHECK (payment_method IN ('MOCK_PAYMENT', 'COD')),
    CONSTRAINT ck_orders_payment_status CHECK (payment_status IN ('PAID', 'PENDING'))
);

CREATE INDEX idx_orders_user_id ON orders (user_id);

CREATE TABLE order_items (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id     BIGINT         NOT NULL,
    product_id   BIGINT         NOT NULL,   -- no FK: belongs to catalog-service
    product_name VARCHAR(150)   NOT NULL,
    unit_price   NUMERIC(10, 2) NOT NULL,
    quantity     INTEGER        NOT NULL,
    line_total   NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);