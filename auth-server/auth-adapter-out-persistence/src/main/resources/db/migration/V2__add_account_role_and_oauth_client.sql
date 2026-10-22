ALTER TABLE account
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' AFTER login_id;

-- scope와 리다이렉트 URI는 공백 구분 문자열로 저장한다 (OAuth2 규격상 값에 공백이 없음)
CREATE TABLE oauth_client
(
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    client_id                 VARCHAR(50)   NOT NULL,
    client_name               VARCHAR(100)  NOT NULL,
    client_secret_hash        VARCHAR(255)  NOT NULL,
    redirect_uris             VARCHAR(2000) NOT NULL,
    post_logout_redirect_uris VARCHAR(2000) NOT NULL,
    scopes                    VARCHAR(500)  NOT NULL,
    created_at                DATETIME(6)   NOT NULL,
    updated_at                DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_oauth_client_client_id UNIQUE (client_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
