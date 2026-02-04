-- Chatbot demo scenarios
INSERT INTO chat_scenarios (id, scenario_key, title, description, active)
VALUES
  (1, 'auction_flow', '입찰/경매 진행', '경매 참여와 등록 관련 안내', true),
  (2, 'payment_flow', '낙찰/결제', '낙찰 이후 결제 및 정산 안내', true);

-- Nodes for scenario 1
INSERT INTO chat_nodes (id, scenario_id, node_key, text, is_root, is_terminal, sort_order)
VALUES
  (101, 1, 'root', '어떤 도움이 필요하신가요?', true, false, 0),
  (102, 1, 'how_to_bid', '입찰은 경매 상세 페이지에서 진행됩니다. 현재가보다 높은 금액을 입력해 주세요.', false, false, 1),
  (103, 1, 'register_guide', '경매 등록은 상품 정보와 시작/종료 시간을 입력하면 완료됩니다.', false, false, 2);

-- Options for scenario 1 root
INSERT INTO chat_options (id, node_id, label, next_node_id, action_type, action_target, sort_order)
VALUES
  (1001, 101, '경매 목록 보기', NULL, 'LINK', '/auctions', 1),
  (1002, 101, '경매 등록하기', NULL, 'LINK', '/auctions/register', 2),
  (1003, 101, '입찰 방법 안내', 102, 'NONE', NULL, 3),
  (1004, 101, '등록 방법 안내', 103, 'NONE', NULL, 4);

-- Options for scenario 1: how_to_bid
INSERT INTO chat_options (id, node_id, label, next_node_id, action_type, action_target, sort_order)
VALUES
  (1011, 102, '입찰 가능한 경매로 이동', NULL, 'LINK', '/auctions', 1),
  (1012, 102, '처음으로', 101, 'NONE', NULL, 2);

-- Options for scenario 1: register_guide
INSERT INTO chat_options (id, node_id, label, next_node_id, action_type, action_target, sort_order)
VALUES
  (1021, 103, '등록 페이지로 이동', NULL, 'LINK', '/auctions/register', 1),
  (1022, 103, '처음으로', 101, 'NONE', NULL, 2);

-- Nodes for scenario 2
INSERT INTO chat_nodes (id, scenario_id, node_key, text, is_root, is_terminal, sort_order)
VALUES
  (201, 2, 'root', '낙찰/결제는 아래 메뉴에서 선택할 수 있어요.', true, false, 0),
  (202, 2, 'payment_guide', '결제는 마이페이지 또는 결제 진행 페이지에서 확인할 수 있습니다.', false, false, 1);

-- Options for scenario 2 root
INSERT INTO chat_options (id, node_id, label, next_node_id, action_type, action_target, sort_order)
VALUES
  (2001, 201, '결제 진행 페이지로 이동', NULL, 'LINK', '/login', 1),
  (2002, 201, '결제 상태 확인 (API)', NULL, 'API', '/payments', 2),
  (2003, 201, '결제 안내 보기', 202, 'NONE', NULL, 3);

-- Options for scenario 2: payment_guide
INSERT INTO chat_options (id, node_id, label, next_node_id, action_type, action_target, sort_order)
VALUES
  (2011, 202, '로그인 페이지로 이동', NULL, 'LINK', '/login', 1),
  (2012, 202, '처음으로', 201, 'NONE', NULL, 2);
