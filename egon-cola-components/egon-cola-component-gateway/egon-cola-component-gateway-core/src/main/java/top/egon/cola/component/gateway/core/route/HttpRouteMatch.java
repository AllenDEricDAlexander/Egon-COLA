package top.egon.cola.component.gateway.core.route;

import java.util.Map;
import java.util.Objects;

public record HttpRouteMatch(
        RuntimeHttpRoute route,
        Map<String, String> pathVariables
) {

    public HttpRouteMatch {
        route = Objects.requireNonNull(route, "route");
        pathVariables = Map.copyOf(
                Objects.requireNonNull(pathVariables, "pathVariables")
        );
    }
}
