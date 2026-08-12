package top.egon.cola.component.gateway.engine.traffic;

import top.egon.cola.component.gateway.engine.rule.GatewayRuleJsonCodec;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中文说明：{@code GatewayPolicyKeyCompiler} 是编译器，位于当前 Gateway 模块的相关包中，负责网关策略键Compiler相关的职责与边界。
 * English summary: {@code GatewayPolicyKeyCompiler} is a gateway policy key compiler compiler in the current Gateway module; it owns the gateway policy key compiler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayPolicyKeyCompiler {

    /**
     * 中文说明：表示 FIELD 这一固定值；它属于 {@code GatewayPolicyKeyCompiler} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value field; it is a state, type, or protocol value of {@code GatewayPolicyKeyCompiler} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayPolicyKeyCompiler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayPolicyKeyCompiler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Pattern FIELD =
            Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9_.-]{0,63})}");

    /**
     * 中文说明：执行 compile 操作；该方法是 {@code GatewayPolicyKeyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile operation; this method is the invocation entry point on {@code GatewayPolicyKeyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayPolicyKeyCompiler.compile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param expression 参数 expression；parameter expression。
     * @return 返回 compile 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 literal 操作；该方法是 {@code GatewayPolicyKeyCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the literal operation; this method is the invocation entry point on {@code GatewayPolicyKeyCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayPolicyKeyCompiler.literal(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param parts 参数 parts；parameter parts。
     * @param literal 参数 literal；parameter literal。
     */
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

    /**
     * 中文说明：{@code CompiledTrafficKey} 是接口契约，位于当前 Gateway 模块的相关包中，负责Compiled流量键相关的职责与边界。
     * English summary: {@code CompiledTrafficKey} is an interface contract in the current Gateway module; it owns the compiled traffic key-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    @FunctionalInterface
    public interface CompiledTrafficKey {

        /**
         * 中文说明：执行 hash 操作；该方法是 {@code GatewayPolicyKeyCompiler.CompiledTrafficKey} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the hash operation; this method is the invocation entry point on {@code GatewayPolicyKeyCompiler.CompiledTrafficKey} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayPolicyKeyCompiler.CompiledTrafficKey.hash(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param context 参数 context；parameter context。
         * @return 返回 hash 的处理结果；returns the result of the operation.
         */
        String hash(GatewayTrafficContext context);
    }

    /**
     * 中文说明：{@code Part} 是接口契约，位于当前 Gateway 模块的相关包中，负责Part相关的职责与边界。
     * English summary: {@code Part} is an interface contract in the current Gateway module; it owns the part-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private interface Part {

        /**
         * 中文说明：执行 append 操作；该方法是 {@code GatewayPolicyKeyCompiler.Part} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the append operation; this method is the invocation entry point on {@code GatewayPolicyKeyCompiler.Part} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayPolicyKeyCompiler.Part.append(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param context 参数 context；parameter context。
         * @param target 参数 target；parameter target。
         */
        void append(GatewayTrafficContext context, StringBuilder target);
    }

    /**
     * 中文说明：{@code FieldPart} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责FieldPart相关的职责与边界。
     * English summary: {@code FieldPart} is an immutable data carrier in the current Gateway module; it owns the field part-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param field 参数 field；parameter field。
     */
    private record FieldPart(
    /**
     * 中文说明：保存 field 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayPolicyKeyCompiler.FieldPart} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by field; its type is {@code String}, and {@code GatewayPolicyKeyCompiler.FieldPart} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayPolicyKeyCompiler.FieldPart} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayPolicyKeyCompiler.FieldPart}; do not couple callers to its representation when the owning type exposes an API.
     */
    String field) implements Part {

        /**
         * 中文说明：执行 append 操作；该方法是 {@code GatewayPolicyKeyCompiler.FieldPart} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the append operation; this method is the invocation entry point on {@code GatewayPolicyKeyCompiler.FieldPart} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayPolicyKeyCompiler.FieldPart.append(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param context 参数 context；parameter context。
         * @param target 参数 target；parameter target。
         */
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
