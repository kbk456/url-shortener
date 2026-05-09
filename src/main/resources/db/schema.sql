CREATE TABLE IF NOT EXISTS url (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    short_code        VARCHAR(10)   NULL,
    original_url      TEXT          NOT NULL,
    original_url_hash VARCHAR(64)   NOT NULL,
    is_active         TINYINT(1)    NOT NULL DEFAULT 1,
    click_count       BIGINT        NOT NULL DEFAULT 0,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE  KEY uk_short_code  (short_code),
    INDEX         idx_hash     (original_url_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS url_click_log (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    short_code  VARCHAR(10)   NOT NULL,
    ip_address  VARCHAR(45)   NULL,
    user_agent  VARCHAR(500)  NULL,
    referer     VARCHAR(500)  NULL,
    clicked_at  DATETIME      NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_short_code (short_code),
    INDEX idx_clicked_at (clicked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
