package com.polarbookshop.orderservice.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "polar")
public record ClientProperties(

        /**
         * 카탈로그 서비스의 URI를 지정하는 속성
         */
        @NotNull
        URI catalogServiceUri
) {
}
