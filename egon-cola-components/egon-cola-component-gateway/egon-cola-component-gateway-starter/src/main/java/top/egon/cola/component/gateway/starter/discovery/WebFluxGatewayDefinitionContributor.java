package top.egon.cola.component.gateway.starter.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class WebFluxGatewayDefinitionContributor
        implements GatewayDefinitionContributor {

    private final RequestMappingHandlerMapping mappings;

    private final GatewayHttpOperationMapper mapper;

    public WebFluxGatewayDefinitionContributor(
            RequestMappingHandlerMapping mappings,
            GatewayReportingProperties properties,
            ObjectMapper objectMapper) {
        this.mappings = mappings;
        mapper = new GatewayHttpOperationMapper(properties, objectMapper);
    }

    @Override
    public List<DiscoveredInterfaceGroup> discover() {
        Map<Class<?>, List<GatewayHttpOperationMapper.Mapping>> byType =
                new LinkedHashMap<>();
        mappings.getHandlerMethods().forEach((mapping, handler) -> {
            if (excluded(handler.getBeanType())) {
                return;
            }
            byType.computeIfAbsent(
                    handler.getBeanType(),
                    ignored -> new ArrayList<>()
            ).add(new GatewayHttpOperationMapper.Mapping(
                    handler,
                    paths(mapping),
                    methods(mapping),
                    media(mapping.getConsumesCondition()
                            .getConsumableMediaTypes()),
                    media(mapping.getProducesCondition()
                            .getProducibleMediaTypes())
            ));
        });
        return byType.entrySet().stream()
                .map(entry -> mapper.group(
                        entry.getKey(),
                        entry.getValue()
                ))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private Set<String> paths(RequestMappingInfo mapping) {
        Set<String> values = mapping.getPatternsCondition()
                .getPatterns()
                .stream()
                .map(Object::toString)
                .collect(Collectors.toUnmodifiableSet());
        return values.isEmpty() ? Set.of("/") : values;
    }

    private Set<String> methods(RequestMappingInfo mapping) {
        Set<String> methods = mapping.getMethodsCondition()
                .getMethods()
                .stream()
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
        return methods.isEmpty() ? Set.of("ANY") : methods;
    }

    private Set<String> media(
            Set<org.springframework.http.MediaType> values) {
        return values.stream()
                .map(Object::toString)
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean excluded(Class<?> type) {
        String name = type.getName();
        return name.startsWith("org.springframework.")
                || name.startsWith(
                "top.egon.cola.component.gateway.starter.");
    }
}
