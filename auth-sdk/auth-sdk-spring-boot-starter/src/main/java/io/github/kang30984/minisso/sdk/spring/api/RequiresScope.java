package io.github.kang30984.minisso.sdk.spring.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 토큰에 지정한 scope가 모두 있어야 호출할 수 있다. 없으면 403 ProblemDetail로 응답한다.
 * 클래스에 붙이면 모든 핸들러에 적용되고, 메서드에 붙인 값이 함께 검사된다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresScope {
    String[] value();
}
