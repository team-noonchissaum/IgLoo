package noonchissaum.backend.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.chat.dto.res.ChatRoomRes;
import noonchissaum.backend.domain.chat.dto.res.MyChatRoomRes;
import noonchissaum.backend.domain.chat.entity.ChatRoom;
import noonchissaum.backend.domain.chat.repository.ChatRoomRepository;
import noonchissaum.backend.domain.notification.constants.NotificationConstants;
import noonchissaum.backend.domain.notification.entity.NotificationType;
import noonchissaum.backend.domain.notification.service.NotificationService;
import noonchissaum.backend.domain.order.entity.Order;
import noonchissaum.backend.domain.order.entity.DeliveryType;
import noonchissaum.backend.domain.order.repository.OrderRepository;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    /**
     * DIRECT 거래 선택 시 채팅방 생성
     * auctionId 기준으로 UNIQUE를 걸어두면 "경매당 채팅방 1개"가 보장되며 중복 클릭/재시도/동시 요청에도 멱등하게 동작한다.
     * deliveryType == DIRECT 인 경우에만 호출
     */
    @Transactional
    public ChatRoomRes createRoom(Order order){

        // 1. 직거래 가능 상태인지 필수 값 체크
        validateOrderForDirectTrade(order);

        Long auctionId = order.getAuction().getId();
        Long userId = order.getBuyer().getId();

        // 2. 이미 해당 경매에 대한 채팅방이 존재하는지 1차 확인 (성능 최적화)
        // 존재하면 즉시 반환, 없으면 생성 로직(createRoomDedup) 실행
        return chatRoomRepository.findByAuctionId(auctionId)
                .map(room -> ChatRoomRes.from(room, userId))
                .orElseGet(() -> createRoomDedup(order, userId));
    }

    private ChatRoomRes createRoomDedup(Order order, Long userId) {
        Long auctionId = order.getAuction().getId();

        try {
            ChatRoom room = ChatRoom.builder()
                    .auction(order.getAuction())
                    .seller(order.getSeller())
                    .buyer(order.getBuyer())
                    .build();

            ChatRoom saved = chatRoomRepository.save(room);

            String message = String.format(
                    NotificationConstants.MSG_CHAT_CREATED,
                    order.getBuyer().getNickname()
            );
            // 판매자에게 알림(1번만)
            notificationService.sendNotification(
                    order.getSeller().getId(),
                    NotificationType.CHAT_CREATED,
                    message,
                    NotificationConstants.REF_TYPE_CHATROOM,
                    saved.getId()
            );


            return ChatRoomRes.from(saved, userId);

        } catch (DataIntegrityViolationException e) {

            /**
             * [동시성 처리] 동시에 save() 호출 시 하나는 성공하고 하나는 중복 에러 발생
            에러 발생 시 로그를 남기고, 먼저 생성된 방을 다시 조회해서 반환
             */
            log.info("ChatRoom unique conflict (auctionId={}), re-fetching...", auctionId);

            return chatRoomRepository.findByAuctionId(auctionId)
                    .map(room -> ChatRoomRes.from(room, userId))
                    .orElseThrow(() -> e);
        }
    }

    @Transactional(readOnly = true)
    public List<MyChatRoomRes> getMyRooms(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository.findMyRooms(userId);
        return rooms.stream()
                .map(room -> MyChatRoomRes.from(room, userId))
                .toList();
    }

    /**
     * 채팅방 상세 조회
     * 관리자(ADMIN)이거나 방 참여자(Member)인 경우에만 허용
     */
    @Transactional(readOnly = true)
    public ChatRoomRes getRoom(Long roomId, UserPrincipal userPrincipal) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 1. 권한 확인
        boolean isAdmin = isAdmin(userPrincipal);
        boolean isMember = isParticipant(room, userPrincipal.getUserId());

        // 2. 관리자도 아니고 참여자도 아니면 차단
        if (!isMember && !isAdmin) {
            throw new ApiException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        Long viewBasisUserId = isMember ? userPrincipal.getUserId() : room.getBuyer().getId();

        return ChatRoomRes.from(room, viewBasisUserId);
    }

    /**
     * [관리자 전용] 관리자 페이지 등에서 사용할 채팅방 정보 조회 (참여자 체크 없음)
     */
    @Transactional(readOnly = true)
    public ChatRoomRes getRoomForAdmin(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 관리자 뷰에서는 구매자를 기준으로 상대방(판매자) 정보를 렌더링하도록 설정
        return ChatRoomRes.from(room, room.getBuyer().getId());
    }

    // 공통 검증 로직

    private boolean isAdmin(UserPrincipal userPrincipal) {
        return userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isParticipant(ChatRoom room, Long userId) {
        return room.getBuyer().getId().equals(userId) ||
                room.getSeller().getId().equals(userId);
    }

    /**
     * 직거래 채팅방 생성이 가능한 주문인지 유효성 검증
     */
    private void validateOrderForDirectTrade(Order order) {
        if (order == null || order.getAuction() == null ||
                order.getBuyer() == null || order.getSeller() == null) {
            throw new ApiException(ErrorCode.CHAT_INVALID_REQUEST);
        }
    }

    /**
     * 경매 기준 채팅방 생성 또는 조회
     * - 구매자: deliveryType null이면 DIRECT 설정 후 채팅방 생성, DIRECT면 기존 방 반환, SHIPMENT면 예외
     * - 판매자: 채팅방이 있으면 반환, 없으면 예외
     */
    @Transactional
    public ChatRoomRes ensureRoomForAuction(Long auctionId, Long userId) {
        Order order = orderRepository.findByAuction_IdAndBuyer_Id(auctionId, userId)
                .or(() -> orderRepository.findByAuction_IdAndSeller_Id(auctionId, userId))
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));

        boolean isBuyer = order.getBuyer().getId().equals(userId);

        if (isBuyer) {
            if (order.getDeliveryType() == DeliveryType.SHIPMENT) {
                throw new ApiException(ErrorCode.CHAT_INVALID_REQUEST); // 배송 선택 거래는 채팅 불가
            }
            if (order.getDeliveryType() == null) {
                order.chooseDeliveryType(DeliveryType.DIRECT);
            }
        }

        return chatRoomRepository.findByAuctionId(auctionId)
                .map(room -> {
                    boolean isMember = room.getBuyer().getId().equals(userId)
                            || room.getSeller().getId().equals(userId);
                    if (!isMember) {
                        throw new ApiException(ErrorCode.CHAT_ACCESS_DENIED);
                    }
                    return ChatRoomRes.from(room, userId);
                })
                .orElseGet(() -> {
                    if (!isBuyer) {
                        throw new ApiException(ErrorCode.CHAT_INVALID_REQUEST); // 판매자: 구매자가 아직 거래 방식 미선택
                    }
                    return createRoom(order);
                });
    }
}
