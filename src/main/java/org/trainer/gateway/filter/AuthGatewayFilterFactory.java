package org.trainer.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.util.context.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author rogin
 */
@Component
public class AuthGatewayFilterFactory implements GatewayFilterFactory<AuthGatewayFilterFactory.Config> {

    @Override
    public GatewayFilter apply(Config config) {

        GatewayFilter filter = (exchange, chain) -> {
            if (config.getAllow()) {
                return chain.filter(exchange).contextWrite(Context.of("USER", "张三"));
            }
            return chain.filter(exchange);
        };
        return new OrderedGatewayFilter(filter, 1000);
    }

    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.singletonList("allow");
    }

    public static class Config {
        Boolean allow;

        public Boolean getAllow() {
            return this.allow;
        }

        public void setAllow(Boolean allow) {
            this.allow = allow;
        }
    }
}


