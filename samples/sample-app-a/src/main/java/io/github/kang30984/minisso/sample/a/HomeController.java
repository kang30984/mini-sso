package io.github.kang30984.minisso.sample.a;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
class HomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    String home(@AuthenticationPrincipal OidcUser user, CsrfToken csrfToken) {
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
                  <p><a href="/call-app-b">내 토큰으로 샘플 앱 B API 호출하기</a></p>
                  <form method="post" action="/logout" style="margin-top:16px">
                    <input type="hidden" name="%s" value="%s">
                    <button type="submit">로그아웃 (SSO 세션까지 종료)</button>
                  </form>
                </body>
                </html>
                """.formatted(
                escape(user.getSubject()),
                escape(user.getPreferredUsername()),
                escape(user.getFullName()),
                escape(user.getClaimAsString("department")),
                escape(user.getEmail()),
                escape(String.valueOf(user.getIssuer())),
                escape(csrfToken.getParameterName()),
                escape(csrfToken.getToken()));
    }

    /** 토큰 클레임도 외부 입력이므로 HTML에 넣기 전에 이스케이프한다 */
    private static String escape(String value) {
        return value == null ? "-" : HtmlUtils.htmlEscape(value);
    }
}
