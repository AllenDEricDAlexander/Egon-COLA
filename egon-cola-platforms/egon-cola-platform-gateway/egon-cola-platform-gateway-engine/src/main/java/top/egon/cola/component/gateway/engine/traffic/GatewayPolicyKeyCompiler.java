package top.egon.cola.component.gateway.engine.traffic;

import top.egon.cola.component.gateway.engine.rule.GatewayRuleJsonCodec;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GatewayPolicyKeyCompiler {

    private static final Pattern FIELD =
            Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9_.-]{0,63})}");

    public CompiledTrafficKey compile(String expression) {
        if (expression == null
                || expression.isBlank()
                || expression.length() > 512) {
            throw new IllegalArgumentException(
                    "traffic key expression is required and bounded"
            );
        }
        Matcher matcher = FIELD.matcher(expression);
        List<Part> parts = new ArrayList<>();
        int offset = 0;
        while (matcher.find()) {
            literal(parts, expression.substring(offset, matcher.start()));
            parts.add(new FieldPart(matcher.group(1)));
            offset = matcher.end();
        }
        literal(parts, expression.substring(offset));
        if (parts.stream().noneMatch(FieldPart.class::isInstance)) {
            throw new IllegalArgumentException(
                    "traffic key expression requires at least one field"
            );
        }
        return context -> {
            StringBuilder raw = new StringBuilder();
            for (Part part : parts) {
                part.append(context, raw);
            }
            if (raw.length() > 2048) {
                throw new IllegalArgumentException(
                        "traffic key material exceeds maximum length"
                );
            }
            return GatewayRuleJsonCodec.sha256(
                    raw.toString().getBytes(StandardCharsets.UTF_8)
            );
        };
    }

    private void literal(List<Part> parts, String literal) {
        if (!literal.matches("[A-Za-z0-9._:/-]*")) {
            throw new IllegalArgumentException(
                    "traffic key expression contains unsafe literal"
            );
        }
        if (!literal.isEmpty()) {
            parts.add((context, target) -> target.append(literal));
        }
    }

    @FunctionalInterface
    public interface CompiledTrafficKey {

        String hash(GatewayTrafficContext context);
    }

    private interface Part {

        void append(GatewayTrafficContext context, StringBuilder target);
    }

    private record FieldPart(String field) implements Part {

        @Override
        public void append(
                GatewayTrafficContext context,
                StringBuilder target) {
            String value = context.value(field);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        "traffic key field is missing: " + field
                );
            }
            target.append(value);
        }
    }
}
