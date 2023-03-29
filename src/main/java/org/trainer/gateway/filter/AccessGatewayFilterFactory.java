package org.trainer.gateway.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * @author rogin
 */
@Component
@Slf4j
public class AccessGatewayFilterFactory implements GatewayFilterFactory<AccessGatewayFilterFactory.Config> {

    @Override
    public GatewayFilter apply(AccessGatewayFilterFactory.Config config) {
        GatewayFilter filter = (exchange, chain) -> Mono.deferContextual(Mono::just)
                .cast(Context.class)
                .filter(c -> c.hasKey("USER"))
                .map(c -> c.get("USER"))
                .switchIfEmpty(Mono.error(new RuntimeException("没有通过认证，不允许访问")))
                .cast(String.class)
                .flatMap(u -> {
                    log.info("current login user is: {}", u);
                    return chain.filter(exchange);
                })
                .onErrorResume(Exception.class, e -> this.generalErrorHandle(exchange, e));
        return new OrderedGatewayFilter(filter, 5000);

    }

    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.singletonList("auth");
    }


    private Mono<Void> generalErrorHandle(ServerWebExchange exchange, Exception e) {
        final String errorMessage = e.getMessage();
        return Mono.defer(() -> Mono.just(exchange.getResponse())).flatMap(response -> {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            HttpHeaders headers = response.getHeaders();
            headers.setContentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8));
            DataBufferFactory dataBufferFactory = response.bufferFactory();
            DataBuffer buffer = dataBufferFactory.wrap(errorMessage.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer)).doOnError(error -> DataBufferUtils.release(buffer));
        });
    }

    @Getter
    @Setter
    public static class Config {
        String auth;

    }
}
