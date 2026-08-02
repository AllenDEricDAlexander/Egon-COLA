package top.egon.cola.component.gateway.mcp.tool;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class McpArgumentBinder {

    private static final Set<String> FORBIDDEN_TARGETS = Set.of(
            "operationid",
            "providerurl",
            "routeid",
            "servicename",
            "authorization",
            "tlsprofile"
    );

    public Map<String, Object> bind(
            McpRuntimeTool tool,
            Map<String, Object> input) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        tool.argumentBindings().forEach((source, target) -> {
            if (input.containsKey(source) && allowed(target)) {
                result.put(target, input.get(source));
            }
        });
        return java.util.Collections.unmodifiableMap(result);
    }

    private boolean allowed(String target) {
        return target != null
                && !target.isBlank()
                && !FORBIDDEN_TARGETS.contains(
                target.replace("_", "")
                        .replace("-", "")
                        .toLowerCase(Locale.ROOT)
        );
    }
}
