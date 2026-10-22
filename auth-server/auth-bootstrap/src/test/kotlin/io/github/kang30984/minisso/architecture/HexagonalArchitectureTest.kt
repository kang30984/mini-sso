package io.github.kang30984.minisso.architecture

import com.tngtech.archunit.base.DescribedPredicate.not
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices

/**
 * DESIGN.md 4.1 의존 규칙을 테스트로 강제한다.
 * 패키지: domain / application / adapter.{inbound|outbound}.{name}
 */
@AnalyzeClasses(
    packages = ["io.github.kang30984.minisso"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class HexagonalArchitectureTest {

    @ArchTest
    val `도메인은 JDK와 Kotlin 표준 라이브러리 외에 의존하지 않는다`: ArchRule =
        classes().that().resideInAPackage("..minisso.domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..minisso.domain..", "java..", "kotlin..", "org.jetbrains.annotations..")
            .allowEmptyShould(true)

    @ArchTest
    val `애플리케이션은 어댑터에 의존하지 않는다`: ArchRule =
        noClasses().that().resideInAPackage("..minisso.application..")
            .should().dependOnClassesThat().resideInAPackage("..minisso.adapter..")
            .allowEmptyShould(true)

    @ArchTest
    val `애플리케이션은 spring-tx 외의 Spring에 의존하지 않는다 (ADR-006)`: ArchRule =
        noClasses().that().resideInAPackage("..minisso.application..")
            .should().dependOnClassesThat(
                resideInAPackage("org.springframework..")
                    .and(not(resideInAPackage("org.springframework.transaction.."))),
            )
            .allowEmptyShould(true)

    @ArchTest
    val `어댑터끼리는 서로 의존하지 않는다`: ArchRule =
        slices().matching("..minisso.adapter.(*).(*)..")
            .should().notDependOnEachOther()
            .allowEmptyShould(true)
}
