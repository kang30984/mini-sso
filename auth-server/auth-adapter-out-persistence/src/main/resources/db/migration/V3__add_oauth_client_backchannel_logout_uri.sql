-- OIDC Back-Channel Logout 1.0: 로그아웃 시 Logout Token을 받을 클라이언트 엔드포인트
ALTER TABLE oauth_client
    ADD COLUMN backchannel_logout_uri VARCHAR(500) NULL AFTER scopes;
