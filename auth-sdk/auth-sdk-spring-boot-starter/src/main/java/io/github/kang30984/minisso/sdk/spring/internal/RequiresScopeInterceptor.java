package io.github.kang30984.minisso.sdk.spring.internal;

import io.github.kang30984.minisso.sdk.core.AuthUser;
import io.github.kang30984.minisso.sdk.spring.api.RequiresScope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.stream.Stream;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public final class RequiresScopeInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        String[] required = Stream.of(
                        AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequiresScope.class),
                        AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequiresScope.class))
                .filter(java.util.Objects::nonNull)
                .flatMap(annotation -> Arrays.stream(annotation.value()))
                .toArray(String[]::new);
        if (required.length == 0) {
            return true;
        }

        AuthUser user = CurrentUserArgumentResolver.currentUser();
        if (user != null && Arrays.stream(required).allMatch(user::hasScope)) {
            return true;
        }
        ProblemDetailResponses.write(response, 403, "Forbidden", "필요한 scope가 없습니다.", "MINISSO-403");
        return false;
    }
}
