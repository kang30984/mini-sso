package io.github.kang30984.minisso.sdk.spring.internal;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 인증·인가 실패를 RFC 9457 ProblemDetail 형식으로 응답한다.
 * 모든 서비스가 같은 형식과 코드(MINISSO-401, MINISSO-403)를 쓰게 해 클라이언트가 일관되게 처리할 수 있다.
 */
final class ProblemDetailResponses {

    private ProblemDetailResponses() {
    }

    static void write(HttpServletResponse response, int status, String title, String detail, String code)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // 값은 모두 SDK 내부 상수이므로 이스케이프가 필요한 문자가 없다
        response.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
                        + ",\"detail\":\"" + detail + "\",\"code\":\"" + code + "\"}");
    }
}
