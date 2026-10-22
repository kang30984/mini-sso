package io.github.kang30984.minisso.sdk.spring.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** 401(토큰 없음·무효)과 403(권한 부족) 응답. RFC 6750에 따라 WWW-Authenticate 헤더도 함께 보낸다. */
public final class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        boolean invalidToken = exception instanceof InvalidBearerTokenException;
        response.setHeader("WWW-Authenticate", invalidToken ? "Bearer error=\"invalid_token\"" : "Bearer");
        ProblemDetailResponses.write(response, 401, "Unauthorized",
                invalidToken ? "액세스 토큰이 유효하지 않거나 만료되었습니다." : "액세스 토큰이 필요합니다.",
                "MINISSO-401");
    }

    public static AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> {
            response.setHeader("WWW-Authenticate", "Bearer error=\"insufficient_scope\"");
            ProblemDetailResponses.write(response, 403, "Forbidden", "이 요청을 수행할 권한이 없습니다.", "MINISSO-403");
        };
    }
}
