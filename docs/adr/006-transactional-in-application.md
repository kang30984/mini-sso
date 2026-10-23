# ADR-006 application 계층에 @Transactional만 허용

## 배경

헥사고날 구조에서는 application 계층도 프레임워크에 의존하지 않는 게 이상적이다. 그런데 실제로는 유스케이스 하나가 곧 트랜잭션 하나인 경우가 대부분이다.

## 검토한 방법

1. application을 완전히 순수하게 두고, 트랜잭션은 바깥에서 프록시로 감싸기
2. `TransactionPort` 같은 추상화 만들기
3. `spring-tx`의 `@Transactional`만 예외로 허용하기

## 결정

3번으로 했다.

## 결과

트랜잭션 경계가 유스케이스 코드 바로 위에 보여서 읽기 쉽다. 허용 범위는 `org.springframework.transaction` 패키지로 좁혔고, 그 밖의 Spring 의존은 ArchUnit 테스트에서 막는다.

1번과 2번이 더 깔끔하긴 하지만, 지금 규모에서는 추상화를 하나 더 두는 비용이 얻는 것보다 크다고 봤다. 나중에 프레임워크를 바꿀 일이 생기면 그때 다시 생각하면 된다.

이 결정 때문에 생긴 고민도 하나 있었다. 낙관적 락 충돌을 재시도하려면 Spring의 예외 타입을 알아야 하는데, application 계층에서는 이걸 참조할 수 없다. 그래서 재시도는 트랜잭션 바깥인 인바운드 어댑터에서 하도록 했다 ([ADR-011](011-concurrent-login-optimistic-lock.md)).
