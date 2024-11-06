package com.polarbookshop.orderservice.book;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class BookClient {
    private static final String BOOKS_ROOT_API = "/books/";
    private final WebClient webClient;

    public Mono<Book> getBookByIsbn(String isbn) {
        return webClient
                .get()
                .uri(BOOKS_ROOT_API + isbn)
                .retrieve() // 요청을 보내고 응답 받음
                .bodyToMono(Book.class) // 받은 객체를 Mono<Book>으로 반환
                .timeout(
                        Duration.ofSeconds(3), // GET요청에 대해 3초의 타임아웃 설정
                        Mono.empty() // 타임아웃이 초과햇을경우 예외를 반환하는대신 빈 Mono객체 반환
                )
                .onErrorResume(WebClientResponseException.NotFound.class,
                        exception -> Mono.empty()) // 404 응답시 빈 객체 반환
                .retryWhen(
                        // 지수 백오프 전략 사용
                        // 100밀리초의 초기 백오프로 총 3회까지 시도한다.
                        Retry.backoff(3, Duration.ofMillis(100))
                )
                .onErrorResume(Exception.class,
                        exception -> Mono.empty()); // 3회 재시도 동안 오류 발생시 빈객체 반환
    }
}
