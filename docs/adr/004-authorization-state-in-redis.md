# ADR-004 인가 상태와 로그인 세션을 Redis에 저장

## 배경
인가 서버는 인가 코드, 액세스·리프레시·ID 토큰과 그 메타데이터(인가 상태)를 저장해야 한다. 기본 구현은 메모리에 저장하므로 재시작하면 모든 토큰이 사라지고, 서버를 여러 대로 늘리면 인스턴스끼리 상태를 공유하지 못한다. 로그인 세션(SSO 세션)도 같은 문제가 있다.

## 선택지
1. 메모리 (기본 구현)
2. MySQL (`JdbcOAuth2AuthorizationService`)
3. Redis

## 결정
3번. 인가 상태는 직접 구현한 `RedisOAuth2AuthorizationService`에, 로그인 세션은 Spring Session으로 Redis에 저장한다.

## 결과
- 인가 상태는 짧게 살다 사라지는 데이터다 (코드 5분, 액세스 토큰 10분, 리프레시 토큰 14일). 토큰 만료 시각을 그대로 TTL로 쓰면 만료 데이터 정리 배치가 필요 없다. MySQL이라면 만료 데이터를 지우는 배치와 그로 인한 부하를 따로 관리해야 한다.
- 토큰 조회는 로그인·API 호출마다 발생하는 고빈도 단건 조회라 키-값 저장소가 적합하다.
- 토큰 원문 대신 SHA-256 해시를 인덱스 키로 써서 Redis 키 목록이 노출되어도 토큰을 재구성할 수 없게 했다.
- 직렬화는 JDK 직렬화를 쓴다. Spring Authorization Server의 `OAuth2Authorization`과 그 안의 토큰, 인증 주체가 모두 `Serializable`이라 별도 매핑 없이 저장할 수 있다. 대신 클래스 구조가 바뀌면 기존 데이터를 읽지 못할 수 있으므로, 배포 시 인가 상태가 초기화될 수 있음을 전제로 한다 (사용자는 재로그인). JSON 직렬화는 인증 주체 타입마다 Jackson Mixin 허용 목록 관리가 필요해 6주차 이후 과제로 남긴다.
- Redis 장애 시 로그인과 토큰 발급이 중단된다. 이는 메모리 구현보다 나쁘지 않으며(그쪽은 재시작만으로 전부 유실), 운영에서는 Redis 복제·Sentinel로 대응한다.
- 한계: Spring Authorization Server가 `sid` 계산에 쓰는 `SessionRegistry`는 아직 메모리 구현이다. 여러 인스턴스 환경에서는 Spring Session의 인덱스 저장소(`FindByIndexNameSessionRepository`) 기반 레지스트리로 바꿔야 한다.
