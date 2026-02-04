package noonchissaum.backend.domain.order.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.order.entity.Order;
import noonchissaum.backend.domain.order.entity.OrderStatus;
import noonchissaum.backend.domain.order.repositroy.OrderRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    @Transactional
    public void createOrder(Auction auction, User buyer) {
        Order order = Order.builder()
                .auction(auction)
                .item(auction.getItem())
                .buyer(buyer)
                .seller(auction.getSeller())
                .status(OrderStatus.CREATED)
                .build();
        orderRepository.save(order);
    }

    /**
     * 관리자 통계용 - 날짜별 전체 거래 수
     */
    public long countByDate(LocalDate date) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getCreatedAt().toLocalDate().equals(date))
                .count();
    }

    /**
     * 관리자 통계용 - 날짜별 완료 거래 수
     */
    public long countCompletedByDate(LocalDate date) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .filter(o -> o.getCreatedAt().toLocalDate().equals(date))
                .count();
    }

    /**
     * 관리자 통계용 - 날짜별 취소 거래 수
     */
    public long countCanceledByDate(LocalDate date) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.CANCELED)
                .filter(o -> o.getCreatedAt().toLocalDate().equals(date))
                .count();
    }

}
