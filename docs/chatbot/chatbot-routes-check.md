# 챗봇 경로 검증 결과

## 프론트엔드 라우트 vs 챗봇 SQL 경로 비교

### ✅ 올바르게 매핑된 경로

| 챗봇 SQL 경로 | 프론트엔드 라우트 | 상태 | 설명 |
|--------------|-----------------|------|------|
| `/` | `/` | ✅ | 홈 페이지 (경매 목록) |
| `/auctions/new` | `/auctions/new` | ✅ | 경매 등록 페이지 |
| `/credits/charge` | `/credits/charge` | ✅ | 크레딧 충전 페이지 |
| `/me/charges` | `/me/charges` | ✅ | 충전 대기 목록 페이지 |

### 📋 프론트엔드 전체 라우트 목록

```typescript
// App.tsx에서 정의된 라우트
/                           → HomePage (경매 목록)
/auctions/:auctionId        → AuctionDetailPage
/auctions/new               → AuctionRegisterPage (인증 필요)
/auctions/:id/live          → AuctionLivePage
/auctions/:id/result        → AuctionResultPage
/payments/result            → PaymentResultPage
/credits/charge             → CreditsChargePage (인증 필요)
/wallet                     → WalletPage (인증 필요)
/notifications              → NotificationsPage (인증 필요)
/me                         → MePage (인증 필요)
/me/edit                    → MeEditPage (인증 필요)
/me/wishes                  → MeWishesPage (인증 필요)
/me/charges                 → ChargesPendingPage (인증 필요)
/delivery                   → DeliveryPage (인증 필요)
/admin                      → AdminPage (관리자 필요)
/chat                       → ChatPage (인증 필요)
/login                      → LoginPage
/oauth/callback             → OAuthCallbackPage
```

### 🔍 백엔드 API 엔드포인트 (참고용)

백엔드 API는 `/api/...` 형태이지만, 챗봇의 `action_target`은 **프론트엔드 라우트 경로**를 사용해야 합니다.

| 백엔드 API | 프론트엔드 라우트 | 챗봇 action_target |
|-----------|-----------------|-------------------|
| `/api/auctions` | `/` | `/` |
| `/api/auctions` (POST) | `/auctions/new` | `/auctions/new` |
| `/api/auctions/{id}` | `/auctions/:auctionId` | `/auctions/{id}` (동적 경로는 사용 불가) |
| `/api/charges/unchecked` | `/me/charges` | `/me/charges` |
| `/api/payments/prepare` | `/credits/charge` | `/credits/charge` |
| `/api/wallets/me` | `/wallet` | `/wallet` |
| `/api/mypage` | `/me` | `/me` |

### ✅ 검증 완료

현재 `chatbot_scenarios_fixed.sql`의 모든 경로가 프론트엔드 라우트와 정확히 일치합니다.

### 📝 참고사항

1. **동적 경로**: 챗봇에서는 `/auctions/:auctionId` 같은 동적 경로를 직접 사용할 수 없으므로, 일반적인 페이지로 이동하는 링크만 사용합니다.

2. **인증 필요 페이지**: `/auctions/new`, `/credits/charge`, `/me/charges` 등은 모두 `RequireAuth`로 보호되어 있어, 로그인하지 않은 사용자는 자동으로 로그인 페이지로 리다이렉트됩니다.

3. **로그인 링크 필터링**: 프론트엔드에서 로그인된 상태에서 `/login` 링크는 자동으로 필터링됩니다.
