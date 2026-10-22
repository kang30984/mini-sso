CREATE TABLE account
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    login_id            VARCHAR(30)  NOT NULL,
    email               VARCHAR(255) NOT NULL,
    name                VARCHAR(50)  NOT NULL,
    department          VARCHAR(100) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    failed_login_count  INT          NOT NULL,
    locked_until        DATETIME(6)  NULL,
    password_changed_at DATETIME(6)  NOT NULL,
    last_login_at       DATETIME(6)  NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    version             BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_account_login_id UNIQUE (login_id),
    CONSTRAINT uk_account_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 계정과 FK를 두지 않는다 (ADR-009). 존재하지 않는 계정에 대한 시도도 기록하므로 account_id는 NULL을 허용한다.
CREATE TABLE login_history
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    account_id     BIGINT       NULL,
    login_id       VARCHAR(100) NOT NULL,
    client_id      VARCHAR(100) NULL,
    ip             VARCHAR(45)  NOT NULL,
    user_agent     VARCHAR(512) NULL,
    outcome        VARCHAR(30)  NOT NULL,
    failure_reason VARCHAR(30)  NULL,
    occurred_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_login_history_occurred_at (occurred_at),
    INDEX idx_login_history_account_occurred (account_id, occurred_at),
    INDEX idx_login_history_login_id_occurred (login_id, occurred_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
