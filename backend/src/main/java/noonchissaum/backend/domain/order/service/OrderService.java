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
}
