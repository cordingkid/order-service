package com.polarbookshop.orderservice.order.event;

import com.polarbookshop.orderservice.order.domain.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class OrderFunctions {

    @Bean
    public Consumer<Flux<OrderDispatchedMessage>> dispatchOrder(OrderService orderService) {
        return flux -> orderService.consumeOrderDispatchedEvent(flux)                           // 각 발송된 메세지에 대해 해당 주문 업데이트
                .doOnNext(order -> log.info("The order with id {} is dispatched", order.id()))  // DB에 업데이트된 orderId 로깅
                .subscribe();                                                                   // 리액티브 스트림 활성화 하기위해 구독
    }
}
