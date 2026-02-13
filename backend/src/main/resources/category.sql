-- =========================
-- 1. 전자기기 / IT
-- =========================
INSERT INTO categories (category_id, name, parent_id) VALUES
                                                          (1, '전자기기/IT', NULL),
                                                          (2, '모바일', 1),
                                                          (3, '컴퓨터', 1),
                                                          (4, '카메라/촬영기기', 1),
                                                          (5, '게임', 1);

-- =========================
-- 2. 패션 / 의류
-- =========================
INSERT INTO categories (category_id, name, parent_id) VALUES
                                                          (6, '패션/의류', NULL),
                                                          (7, '의류', 6),
                                                          (8, '신발', 6),
                                                          (9, '가방', 6),
                                                          (10, '잡화', 6);

-- =========================
-- 3. 뷰티 / 미용
-- =========================
INSERT INTO categories (category_id, name, parent_id) VALUES
                                                          (11, '뷰티/미용', NULL),
                                                          (12, '화장품', 11),
                                                          (13, '미용기기', 11);

-- =========================
-- 4. 생활 / 가정
-- =========================
INSERT INTO categories (category_id, name, parent_id) VALUES
                                                          (14, '생활/가정', NULL),
                                                          (15, '가전', 14),
                                                          (16, '가구', 14),
                                                          (17, '생활용품', 14);

-- =========================
-- 5. 스포츠 / 레저
-- =========================
INSERT INTO categories (category_id, name, parent_id) VALUES
                                                          (18, '스포츠/레저', NULL),
                                                          (19, '운동기구', 18),
                                                          (20, '아웃도어', 18),
                                                          (21, '스포츠용품', 18);

-- =========================
-- 6. 자동차 / 모빌리티
-- =========================
INSERT INTO categories (category_id, name, parent_id) VALUES
                                                          (22, '자동차/모빌리티', NULL),
                                                          (23, '자동차', 22),
                                                          (24, '오토바이', 22),
                                                          (25, '부품/용품', 22);

-- =========================
-- 7. 도서 / 취미
-- =========================
INSERT INTO categories (category_id, name, parent_id) VALUES
                                                          (26, '도서/취미', NULL),
                                                          (27, '도서', 26),
                                                          (28, '취미', 26),
                                                          (29, '수집품', 26);

-- =========================
-- 8. 티켓 / 상품권
-- =========================
INSERT INTO categories (category_id, name, parent_id) VALUES
                                                          (30, '티켓/상품권', NULL),
                                                          (31, '공연/이벤트', 30),
                                                          (32, '상품권', 30);

-- =========================
-- 9. 기타
-- =========================
INSERT INTO categories (category_id, name, parent_id) VALUES
                                                          (33, '기타', NULL),
                                                          (34, '기타물품', 33);