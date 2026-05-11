CREATE TABLE users (
    id                BINARY(16)   NOT NULL,
    email             VARCHAR(255) NOT NULL,
    username          VARCHAR(100) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    shipping_line1    VARCHAR(255),
    shipping_line2    VARCHAR(255),
    shipping_city     VARCHAR(100),
    shipping_region   VARCHAR(100),
    shipping_postal   VARCHAR(20),
    shipping_country  VARCHAR(2),
    billing_line1     VARCHAR(255),
    billing_line2     VARCHAR(255),
    billing_city      VARCHAR(100),
    billing_region    VARCHAR(100),
    billing_postal    VARCHAR(20),
    billing_country   VARCHAR(2),
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payment_methods (
    id          BINARY(16)   NOT NULL,
    user_id     BINARY(16)   NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    brand       VARCHAR(40),
    last4       VARCHAR(4),
    token_ref   VARCHAR(128),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_payment_methods_user (user_id),
    CONSTRAINT fk_payment_methods_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
