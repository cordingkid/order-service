package com.polarbookshop.orderservice.book;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class BookClientTests {
    private MockWebServer mockWebServer;
    private BookClient bookClient;

    @BeforeEach
    void setUp() throws Exception {
        // 모의 서버 실행
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // 모의 서버 URL을 웹 클라이언트의 베이스 URL로 설정
        var webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").uri().toString())
                .build();

        bookClient = new BookClient(webClient);
    }

    @AfterEach
    void clean() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void whenBookExistsThenReturnBook() throws Exception {
        var bookIsbn = "1234567890";

        var mockResponse = new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                            "isbn": %s,
                            "title": "Title",
                            "author": "Author",
                            "price": 9.90,
                            "publisher": "Polarsophia"
                        }
                        """.formatted(bookIsbn));
        // 모의 서버가 처리하는 큐에 모의 응답을 추가
        mockWebServer.enqueue(mockResponse);

        Mono<Book> book = bookClient.getBookByIsbn(bookIsbn);

        StepVerifier.create(book) // BookClient가 반환하는 객체로 StepVerifier 객체 초기화
                .expectNextMatches(b ->
                        b.isbn().equals(bookIsbn)) // 반환된 책의 isbn 비교
                .verifyComplete(); // 리액티브 스트림이 성공적으로 완료됐는지 확인
    }

}