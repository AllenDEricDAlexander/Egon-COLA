package top.egon.cola.component.gateway.starter.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Discovers Gateway interface definitions from Spring MVC request mappings.
 */
public final class MvcGatewayDefinitionContributor
        implements GatewayDefinitionContributor {

    /** Spring MVC request mapping registry to inspect. */
    private final RequestMappingHandlerMapping mappings;

    /** Mapper that converts normalized HTTP mappings into report entries. */
    private final GatewayHttpOperationMapper mapper;

    /**
     * Creates an MVC Gateway definition contributor.
     *
     * @param mappings     the MVC request mapping registry
     * @param properties   the Gateway reporting properties
     * @param objectMapper the object mapper used for schema generation
     */
    public MvcGatewayDefinitionContributor(
            RequestMappingHandlerMapping mappings,
            GatewayReportingProperties properties,
            ObjectMapper objectMapper) {
        this.mappings = mappings;
        mapper = new GatewayHttpOperationMapper(properties, objectMapper);
    }

    /**
     * Discovers annotated MVC handler types and maps their request mappings to
     * Gateway interface groups.
     *
     * @return the discovered interface groups
     * @throws IllegalArgumentException if an operation declaration cannot be
     *                                  represented by the Gateway report
     */
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

    /**
     * Extracts the paths declared by an MVC request mapping.
     *
     * @param mapping the request mapping
     * @return the declared paths, or {@code /} when no path is declared
     */
    private Set<String> paths(RequestMappingInfo mapping) {
        Set<String> values = mapping.getPatternValues();
        return values.isEmpty() ? Set.of("/") : values;
    }

    /**
     * Extracts the HTTP methods declared by an MVC request mapping.
     *
     * @param mapping the request mapping
     * @return the declared method names, or {@code ANY} when unrestricted
     */
    private Set<String> methods(RequestMappingInfo mapping) {
        Set<String> methods = mapping.getMethodsCondition()
                .getMethods()
                .stream()
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
        return methods.isEmpty() ? Set.of("ANY") : methods;
    }

    /**
     * Converts media types to their reportable string representations.
     *
     * @param values the media types to convert
     * @return an unmodifiable set of media type strings
     */
    private Set<String> media(
            Set<org.springframework.http.MediaType> values) {
        return values.stream()
                .map(Object::toString)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Determines whether a handler type belongs to framework, error, or
     * Gateway infrastructure and must be omitted from discovery.
     *
     * @param type the handler bean type
     * @return {@code true} when the handler must be excluded
     */
    private boolean excluded(Class<?> type) {
        String name = type.getName();
        return name.startsWith("org.springframework.")
                || name.contains("BasicErrorController")
                || name.startsWith(
                "top.egon.cola.component.gateway.starter.");
    }
}
