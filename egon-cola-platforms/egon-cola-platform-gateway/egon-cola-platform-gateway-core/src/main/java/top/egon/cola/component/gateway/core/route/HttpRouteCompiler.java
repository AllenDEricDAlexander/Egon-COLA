package top.egon.cola.component.gateway.core.route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HttpRouteCompiler {

    public CompiledHttpRouteIndex compile(List<RuntimeHttpRoute> routes) {
        Objects.requireNonNull(routes, "routes");
        Map<String, Map<String, CompiledHttpRouteIndex.PathNode>> exact =
                new HashMap<>();
        Map<String, Map<String, CompiledHttpRouteIndex.PathNode>> wildcard =
                new HashMap<>();
        Map<String, CompiledHttpRouteIndex.PathNode> any = new HashMap<>();
        for (RuntimeHttpRoute route : routes) {
            List<CompiledHttpRouteIndex.PathSegment> segments =
                    parse(route.pathPattern());
            for (String method : route.methods()) {
                Map<String, CompiledHttpRouteIndex.PathNode> methodIndex;
                if (route.hostPattern().equals("*")) {
                    methodIndex = any;
                } else if (route.hostPattern().startsWith("*.")) {
                    methodIndex = wildcard.computeIfAbsent(
                            route.hostPattern().substring(2),
                            ignored -> new HashMap<>()
                    );
                } else {
                    methodIndex = exact.computeIfAbsent(
                            route.hostPattern(),
                            ignored -> new HashMap<>()
                    );
                }
                methodIndex.computeIfAbsent(
                        method,
                        ignored -> new CompiledHttpRouteIndex.PathNode()
                ).add(segments, 0, route);
            }
        }
        return new CompiledHttpRouteIndex(exact, wildcard, any);
    }

    private List<CompiledHttpRouteIndex.PathSegment> parse(String pattern) {
        if (pattern.equals("/")) {
            return List.of();
        }
        String[] rawSegments = pattern.substring(1).split("/", -1);
        List<CompiledHttpRouteIndex.PathSegment> segments = new ArrayList<>();
        for (String raw : rawSegments) {
            if (raw.isEmpty()) {
                throw new IllegalArgumentException(
                        "empty path pattern segment is not allowed"
                );
            }
            if (raw.equals("**")) {
                segments.add(new CompiledHttpRouteIndex.PathSegment(
                        "**",
                        false,
                        true
                ));
            } else if (raw.startsWith("{") && raw.endsWith("}")) {
                String name = raw.substring(1, raw.length() - 1);
                if (name.isBlank() || name.contains("{") || name.contains("}")) {
                    throw new IllegalArgumentException("invalid path variable");
                }
                segments.add(new CompiledHttpRouteIndex.PathSegment(
                        name,
                        true,
                        false
                ));
            } else {
                if (raw.contains("*") || raw.contains("{") || raw.contains("}")) {
                    throw new IllegalArgumentException(
                            "unsupported path pattern segment"
                    );
                }
                segments.add(new CompiledHttpRouteIndex.PathSegment(
                        raw,
                        false,
                        false
                ));
            }
        }
        return List.copyOf(segments);
    }
}
