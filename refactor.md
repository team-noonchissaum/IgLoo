# 백엔드 리팩토링 로그

## 2026-02-06
- 오타 및 네이밍 일관성 수정
- order 리포지토리 패키지 경로 `repositroy` -> `repository` 변경 및 import 전면 수정
- `CategoryService.getcategory` -> `getCategory`로 변경 및 호출부 수정
- `ItemService.addImages`의 썸네일 지정 조건 오류 수정
- `checkDeadLine`/`markDeadLine` -> `checkDeadline`/`markDeadline`로 변경
- `BidSuccessedPayload`/`sendBidSuccessed` -> `BidSucceededPayload`/`sendBidSucceeded`로 변경
- `registWallet` -> `registerWallet`로 변경 및 호출부 수정
- `BidService` 내부 변수명 `bidSuccessedPayload` -> `bidSucceededPayload`로 정리

