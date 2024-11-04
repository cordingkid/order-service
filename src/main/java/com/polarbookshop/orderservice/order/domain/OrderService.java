package com.polarbookshop.orderservice.order.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public Flux<Order> getAllOrders(){
        return orderRepository.findAll();
    }

    /**
     * 주문 생성
     * @param isbn
     * @param quantity
     * @return
     */
    public Mono<Order> submitOrder(String isbn, int quantity) {
        return Mono.just(buildRejectedOrder(isbn, quantity))
                .flatMap(orderRepository::save);
    }

    // 주문 거부
    public static Order buildRejectedOrder(String bookIsbn, int quantity) {
        return Order.of(bookIsbn, null, null, quantity, OrderStatus.REJECTED);
    }
}
