package top.egon.cola.component.gateway.core.route;

import top.egon.cola.component.gateway.contract.protocol.AccessZone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CompiledHttpRouteIndex {

    private final Map<String, Map<String, PathNode>> exactHosts;

    private final Map<String, Map<String, PathNode>> wildcardHosts;

    private final Map<String, PathNode> anyHost;

    CompiledHttpRouteIndex(
            Map<String, Map<String, PathNode>> exactHosts,
            Map<String, Map<String, PathNode>> wildcardHosts,
            Map<String, PathNode> anyHost) {
        this.exactHosts = immutableHostIndex(exactHosts);
        this.wildcardHosts = immutableHostIndex(wildcardHosts);
        this.anyHost = Map.copyOf(anyHost);
    }

    public Optional<HttpRouteMatch> match(
            String host,
            String method,
            String normalizedPath,
            AccessZone accessZone) {
        Objects.requireNonNull(accessZone, "accessZone");
        Optional<HttpRouteMatch> exact = match(
                exactHosts.get(host),
                method,
                normalizedPath,
                accessZone
        );
        if (exact.isPresent()) {
            return exact;
        }
        int dot = host.indexOf('.');
        while (dot >= 0) {
            Optional<HttpRouteMatch> wildcard = match(
                    wildcardHosts.get(host.substring(dot + 1)),
                    method,
                    normalizedPath,
                    accessZone
            );
            if (wildcard.isPresent()) {
                return wildcard;
            }
            dot = host.indexOf('.', dot + 1);
        }
        return match(anyHost, method, normalizedPath, accessZone);
    }

    private Optional<HttpRouteMatch> match(
            Map<String, PathNode> methods,
            String method,
            String path,
            AccessZone accessZone) {
        if (methods == null) {
            return Optional.empty();
        }
        PathNode root = methods.get(method);
        return root == null
                ? Optional.empty()
                : root.match(path, accessZone);
    }

    private static Map<String, Map<String, PathNode>> immutableHostIndex(
            Map<String, Map<String, PathNode>> source) {
        Map<String, Map<String, PathNode>> copy = new LinkedHashMap<>();
        source.forEach((host, methods) -> copy.put(host, Map.copyOf(methods)));
        return Map.copyOf(copy);
    }

    static final class PathNode {

        private final Map<String, PathNode> literals = new HashMap<>();

        private PathNode variable;

        private String variableName;

        private PathNode catchAll;

        private final List<RuntimeHttpRoute> routes = new ArrayList<>();

        void add(List<PathSegment> segments, int offset, RuntimeHttpRoute route) {
            if (offset == segments.size()) {
                routes.add(route);
                routes.sort((left, right) ->
                        Integer.compare(right.priority(), left.priority()));
                if (routes.size() > 1
                        && routes.get(0).priority() == routes.get(1).priority()) {
                    throw new IllegalArgumentException(
                            "ambiguous route pattern for "
                                    + route.hostPattern()
                                    + " "
                                    + route.pathPattern()
                    );
                }
                return;
            }
            PathSegment segment = segments.get(offset);
            if (segment.catchAll()) {
                if (offset != segments.size() - 1) {
                    throw new IllegalArgumentException(
                            "catch-all must be the final path segment"
                    );
                }
                if (catchAll == null) {
                    catchAll = new PathNode();
                }
                catchAll.add(segments, offset + 1, route);
                return;
            }
            if (segment.variable()) {
                if (variable == null) {
                    variable = new PathNode();
                    variableName = segment.value();
                }
                variable.add(segments, offset + 1, route);
                return;
            }
            literals.computeIfAbsent(segment.value(), ignored -> new PathNode())
                    .add(segments, offset + 1, route);
        }

        Optional<HttpRouteMatch> match(String path, AccessZone zone) {
            String withoutLeading = path.length() == 1
                    ? ""
                    : path.substring(1);
            List<String> segments = withoutLeading.isEmpty()
                    ? List.of()
                    : List.of(withoutLeading.split("/", -1));
            return match(segments, 0, zone, new LinkedHashMap<>());
        }

        private Optional<HttpRouteMatch> match(
                List<String> segments,
                int offset,
                AccessZone zone,
                Map<String, String> variables) {
            if (offset == segments.size()) {
                Optional<RuntimeHttpRoute> terminal = routes.stream()
                        .filter(route -> route.accessZones().contains(zone))
                        .findFirst();
                if (terminal.isPresent()) {
                    return Optional.of(new HttpRouteMatch(
                            terminal.get(),
                            variables
                    ));
                }
                if (catchAll != null) {
                    return catchAll.match(
                            segments,
                            offset,
                            zone,
                            variables
                    );
                }
                return Optional.empty();
            }
            String current = segments.get(offset);
            PathNode literal = literals.get(current);
            if (literal != null) {
                Optional<HttpRouteMatch> result = literal.match(
                        segments,
                        offset + 1,
                        zone,
                        variables
                );
                if (result.isPresent()) {
                    return result;
                }
            }
            if (variable != null) {
                Map<String, String> withVariable = new LinkedHashMap<>(variables);
                withVariable.put(variableName, current);
                Optional<HttpRouteMatch> result = variable.match(
                        segments,
                        offset + 1,
                        zone,
                        withVariable
                );
                if (result.isPresent()) {
                    return result;
                }
            }
            if (catchAll != null) {
                Map<String, String> withCatchAll = new LinkedHashMap<>(variables);
                withCatchAll.put("**", String.join(
                        "/",
                        segments.subList(offset, segments.size())
                ));
                return catchAll.match(
                        segments,
                        segments.size(),
                        zone,
                        withCatchAll
                );
            }
            return Optional.empty();
        }
    }

    record PathSegment(String value, boolean variable, boolean catchAll) {
    }
}
