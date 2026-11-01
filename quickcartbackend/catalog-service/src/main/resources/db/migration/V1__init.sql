CREATE TABLE categories (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255),
    image_url   VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- case-insensitive uniqueness (Section 7.3)
CREATE UNIQUE INDEX uq_categories_name_lower ON categories (lower(name));

CREATE TABLE products (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id BIGINT         NOT NULL,
    name        VARCHAR(150)   NOT NULL,
    description VARCHAR(1000),
    price       NUMERIC(10, 2) NOT NULL,
    unit        VARCHAR(30)    NOT NULL,
    image_url   VARCHAR(500),
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_products_price CHECK (price > 0)
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_name_lower ON products (lower(name));

CREATE TABLE inventory (
    product_id BIGINT      NOT NULL PRIMARY KEY,
    quantity   INTEGER     NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT ck_inventory_quantity CHECK (quantity >= 0)
);