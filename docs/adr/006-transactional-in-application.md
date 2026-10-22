# ADR-006 application 계층에 @Transactional 허용

## 배경
헥사고날 원칙상 application 계층은 프레임워크에 의존하지 않는 것이 이상적이다. 그러나 유스케이스가 곧 트랜잭션 경계인 경우가 대부분이다.

## 선택지
1. application을 완전히 순수하게 유지하고, 트랜잭션은 어댑터 또는 bootstrap에서 프록시로 감싼다
2. `TransactionPort` 같은 추상화를 만든다
3. `spring-tx`의 `@Transactional`만 예외적으로 허용한다

## 결정
3번.

## 결과
- 트랜잭션 경계가 유스케이스 코드 바로 위에 보여 가독성이 높다.
- 허용 범위를 `org.springframework.transaction..` 패키지로 한정하고, 그 외 Spring 의존은 ArchUnit 테스트가 막는다.
- 1, 2번은 순수성은 높지만 이 규모에서는 추상화 비용이 이득보다 크다고 판단했다. 프레임워크를 교체할 일이 생기면 이 결정을 재검토한다.
