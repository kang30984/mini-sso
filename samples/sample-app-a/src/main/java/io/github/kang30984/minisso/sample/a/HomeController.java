package io.github.kang30984.minisso.sample.a;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
class HomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    String home(@AuthenticationPrincipal OidcUser user) {
        return """
                <!doctype html>
                <html lang="ko">
                <head><meta charset="utf-8"><title>Sample App A</title></head>
                <body>
                  <h1>Sample App A</h1>
                  <p>mini-sso로 로그인했습니다.</p>
                  <table border="1" cellpadding="6">
                    <tr><th>sub (계정 ID)</th><td>%s</td></tr>
                    <tr><th>로그인 ID</th><td>%s</td></tr>
                    <tr><th>이름</th><td>%s</td></tr>
                    <tr><th>부서</th><td>%s</td></tr>
                    <tr><th>이메일</th><td>%s</td></tr>
                    <tr><th>발급자</th><td>%s</td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escape(user.getSubject()),
                escape(user.getPreferredUsername()),
                escape(user.getFullName()),
                escape(user.getClaimAsString("department")),
                escape(user.getEmail()),
                escape(String.valueOf(user.getIssuer())));
    }

    /** 토큰 클레임도 외부 입력이므로 HTML에 넣기 전에 이스케이프한다 */
    private static String escape(String value) {
        return value == null ? "-" : HtmlUtils.htmlEscape(value);
    }
}
