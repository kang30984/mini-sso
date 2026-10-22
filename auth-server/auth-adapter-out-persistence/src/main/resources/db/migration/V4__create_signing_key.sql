-- 토큰 서명 키 (ADR-008). 개인키는 AES-256-GCM으로 암호화해 저장한다.
CREATE TABLE signing_key
(
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    kid                   VARCHAR(100)  NOT NULL,
    status                VARCHAR(20)   NOT NULL,
    public_jwk            VARCHAR(4000) NOT NULL,
    encrypted_private_jwk VARCHAR(4000) NOT NULL,
    activated_at          DATETIME(6)   NOT NULL,
    rotated_at            DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_signing_key_kid UNIQUE (kid)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
