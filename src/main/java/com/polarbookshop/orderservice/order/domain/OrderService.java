package com.polarbookshop.orderservice.order.domain;

import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookClient bookClient;

    public Flux<Order> getAllOrders(){
        return orderRepository.findAll();
    }

    /**
     * 주문 생성
     */
    public Mono<Order> submitOrder(String isbn, int quantity) {
        return bookClient.getBookByIsbn(isbn)
                .map(book -> buildAcceptedOrder(book, quantity))
                .defaultIfEmpty(
                        // book 없을경우 주문 거부
                        buildRejectedOrder(isbn, quantity)
                )
                .flatMap(orderRepository::save);
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
}
