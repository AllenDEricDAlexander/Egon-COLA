package top.egon.cola.component.gateway.test.webflux;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class RealtimeWebSocketConfiguration {

    @Bean
    RealtimeWebSocketProbe realtimeWebSocketProbe() {
        return new RealtimeWebSocketProbe();
    }

    @Bean
    RealtimeWebSocketHandler realtimeWebSocketHandler(
            RealtimeWebSocketProbe probe) {
        return new RealtimeWebSocketHandler(probe);
    }

    @Bean
    HandlerMapping realtimeWebSocketMapping(
            RealtimeWebSocketHandler handler) {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(-1);
        mapping.setUrlMap(Map.of(
                "/test/transport/realtime",
                handler
        ));
        return mapping;
    }

    @Bean
    WebSocketHandlerAdapter realtimeWebSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
