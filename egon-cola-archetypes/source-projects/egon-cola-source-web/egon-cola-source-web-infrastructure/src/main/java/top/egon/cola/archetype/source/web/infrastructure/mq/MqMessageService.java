package top.egon.cola.archetype.source.web.infrastructure.mq;

import jakarta.validation.constraints.NotNull;

/** Single outbound message boundary; the broker profile is decided by the bound implementation. */
public interface MqMessageService {
    void publish(@NotNull MqRouteEnum route, @NotNull Object payload);
}
