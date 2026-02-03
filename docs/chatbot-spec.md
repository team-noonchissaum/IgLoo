# Chatbot DB 명세 및 구현 요약

## DB 명세
### chat_scenarios
- `id` (PK, bigint)
- `scenario_key` (varchar 100, unique)
- `title` (varchar 200)
- `description` (varchar 500, nullable)
- `active` (boolean)
- `created_at`, `updated_at`

### chat_nodes
- `id` (PK, bigint)
- `scenario_id` (FK -> chat_scenarios.id)
- `node_key` (varchar 120, unique per scenario)
- `text` (varchar 1000)
- `is_root` (boolean)
- `is_terminal` (boolean)
- `sort_order` (int)
- `created_at`, `updated_at`

### chat_options
- `id` (PK, bigint)
- `node_id` (FK -> chat_nodes.id)
- `label` (varchar 200)
- `next_node_id` (FK -> chat_nodes.id, nullable)
- `action_type` (enum: NONE, LINK, API)
- `action_target` (varchar 500, nullable)
- `sort_order` (int)
- `created_at`, `updated_at`

## 구현 클래스
### 엔티티
- `backend/src/main/java/noonchissaum/backend/domain/chatbot/entity/ChatScenario.java`
- `backend/src/main/java/noonchissaum/backend/domain/chatbot/entity/ChatNode.java`
- `backend/src/main/java/noonchissaum/backend/domain/chatbot/entity/ChatOption.java`
- `backend/src/main/java/noonchissaum/backend/domain/chatbot/entity/ChatActionType.java`

### 레포지토리
- `backend/src/main/java/noonchissaum/backend/domain/chatbot/repository/ChatScenarioRepository.java`
- `backend/src/main/java/noonchissaum/backend/domain/chatbot/repository/ChatNodeRepository.java`
- `backend/src/main/java/noonchissaum/backend/domain/chatbot/repository/ChatOptionRepository.java`

### DTO
- `backend/src/main/java/noonchissaum/backend/domain/chatbot/dto/req/ChatNextReq.java`
- `backend/src/main/java/noonchissaum/backend/domain/chatbot/dto/res/ScenarioSummaryRes.java`
- `backend/src/main/java/noonchissaum/backend/domain/chatbot/dto/res/ChatNodeRes.java`
- `backend/src/main/java/noonchissaum/backend/domain/chatbot/dto/res/ChatOptionRes.java`
- `backend/src/main/java/noonchissaum/backend/domain/chatbot/dto/res/ChatActionRes.java`
- `backend/src/main/java/noonchissaum/backend/domain/chatbot/dto/res/ChatNextRes.java`

### 서비스/컨트롤러
- `backend/src/main/java/noonchissaum/backend/domain/chatbot/service/ChatbotService.java`
- `backend/src/main/java/noonchissaum/backend/domain/chatbot/controller/ChatbotController.java`

## 시나리오 흐름 (Mermaid)
```mermaid
flowchart TB
  %% Scenario 1: auction_flow
  subgraph S1[입찰/경매 진행]
    S1_root([어떤 도움이 필요하신가요?])
    S1_bid([입찰은 경매 상세 페이지에서 진행됩니다.\n현재가보다 높은 금액을 입력해 주세요.])
    S1_reg([경매 등록은 상품 정보와\n시작/종료 시간을 입력하면 완료됩니다.])

    S1_root -->|경매 목록 보기| S1_link_auctions([LINK /auctions])
    S1_root -->|경매 등록하기| S1_link_register([LINK /auctions/register])
    S1_root -->|입찰 방법 안내| S1_bid
    S1_root -->|등록 방법 안내| S1_reg

    S1_bid -->|입찰 가능한 경매로 이동| S1_link_auctions
    S1_bid -->|처음으로| S1_root

    S1_reg -->|등록 페이지로 이동| S1_link_register
    S1_reg -->|처음으로| S1_root
  end

  %% Scenario 2: payment_flow
  subgraph S2[낙찰/결제]
    S2_root([낙찰/결제는 아래 메뉴에서 선택할 수 있어요.])
    S2_guide([결제는 마이페이지 또는\n결제 진행 페이지에서 확인할 수 있습니다.])

    S2_root -->|결제 진행 페이지로 이동| S2_link_login([LINK /login])
    S2_root -->|결제 상태 확인 (API)| S2_api_payments([API /payments])
    S2_root -->|결제 안내 보기| S2_guide

    S2_guide -->|로그인 페이지로 이동| S2_link_login
    S2_guide -->|처음으로| S2_root
  end
```
