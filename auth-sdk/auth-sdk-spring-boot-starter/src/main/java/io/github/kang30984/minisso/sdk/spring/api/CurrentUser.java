package io.github.kang30984.minisso.sdk.spring.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 현재 사용자({@link io.github.kang30984.minisso.sdk.core.AuthUser})를 주입한다.
 *
 * <pre>{@code
 * @GetMapping("/me")
 * public MyInfo me(@CurrentUser AuthUser user) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
