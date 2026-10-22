package io.github.kang30984.minisso.sample.a;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.HtmlUtils;

/**
 * 로그인할 때 받은 액세스 토큰으로 다른 서비스(샘플 앱 B)의 API를 호출한다.
 * B는 토큰을 SDK로 검증하고, 토큰의 사용자(sub)와 호출한 서비스(aud)를 응답한다.
 * 액세스 토큰이 만료되었으면 Spring Security가 리프레시 토큰으로 자동 갱신한다.
 */
@RestController
class AppBClientController {

    private final RestClient restClient;

    AppBClientController(@Value("${sample.app-b-url}") String appBUrl) {
        this.restClient = RestClient.builder().baseUrl(appBUrl).build();
    }

    @GetMapping(value = "/call-app-b", produces = MediaType.TEXT_HTML_VALUE)
    String callAppB(@RegisteredOAuth2AuthorizedClient("mini-sso") OAuth2AuthorizedClient client) {
        String result;
        try {
            result = restClient.get()
                    .uri("/api/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + client.getAccessToken().getTokenValue())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            result = e.getStatusCode() + " " + e.getResponseBodyAsString();
        } catch (Exception e) {
            result = "샘플 앱 B에 연결할 수 없습니다: " + e.getMessage();
        }
        return """
                <!doctype html>
                <html lang="ko">
                <head><meta charset="utf-8"><title>Sample App A → B</title></head>
                <body>
                  <h1>샘플 앱 B 응답</h1>
                  <pre>%s</pre>
                  <p><a href="/">돌아가기</a></p>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(String.valueOf(result)));
    }
}
