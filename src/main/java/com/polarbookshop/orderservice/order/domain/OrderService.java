package com.polarbookshop.orderservice.order.domain;

import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import com.polarbookshop.orderservice.order.event.OrderAcceptedMessage;
import com.polarbookshop.orderservice.order.event.OrderDispatchedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookClient bookClient;
    private final StreamBridge streamBridge;

    public Flux<Order> getAllOrders(String userId){
        return orderRepository.findAllByCreatedBy(userId);
    }

    /**
     * 주문 생성
     */
    @Transactional
    public Mono<Order> submitOrder(String isbn, int quantity) {
        return bookClient.getBookByIsbn(isbn)
                .map(book -> buildAcceptedOrder(book, quantity))
                .defaultIfEmpty(buildRejectedOrder(isbn, quantity))         // book 없을경우 주문 거부
                .flatMap(orderRepository::save)
                .doOnNext(this::publishOrderAcceptedEvent);                 // 주문이 접수되면 이벤트 발행
    }

    /**
     * 주문 성공
     */
    public static Order buildAcceptedOrder(Book book, int quantity) {
        return Order.of(
                book.isbn(),
                book.title() + " - " + book.author(),
                book.price(),
                quantity,
                OrderStatus.ACCEPTED
        );
    }

    /**
     * 주문 거부
     */
    public static Order buildRejectedOrder(String bookIsbn, int quantity) {
        return Order.of(bookIsbn, null, null, quantity, OrderStatus.REJECTED);
    }

    /**
     * 주문 발송후 주문상태 업데이트
     */
    public Flux<Order> consumeOrderDispatchedEvent(Flux<OrderDispatchedMessage> flux) {
        return flux.flatMap(message -> orderRepository.findById(message.orderId()))
                .map(this::buildDispatchedOrder)
                .flatMap(orderRepository::save);
    }

    /**
     * 배송된 상태를 갖는 주문 반환
     */
    private Order buildDispatchedOrder(Order existingOrder) {
        return new Order(
                existingOrder.id(),
                existingOrder.bookIsbn(),
                existingOrder.bookName(),
                existingOrder.bookPrice(),
                existingOrder.quantity(),
                OrderStatus.DISPATCHED,
                existingOrder.createdDate(),
                existingOrder.lastModifiedDate(),
                existingOrder.createdBy(),
                existingOrder.lastModifiedBy(),
                existingOrder.version()
        );
    }

    private void publishOrderAcceptedEvent(Order order) {
        if (!order.status().equals(OrderStatus.ACCEPTED)) {
            return;
        }
        var orderAcceptedMessage = new OrderAcceptedMessage(order.id());
        log.info("Sending order accepted event with id: {}", order.id());
        // 메세지를 acceptOrder-out-0 바인딩에 명시적으로 보냄
        var result = streamBridge.send("acceptOrder-out-0", orderAcceptedMessage);
        log.info("Result of sending data for order with id {}: {}", order.id(), result);
    }
}
