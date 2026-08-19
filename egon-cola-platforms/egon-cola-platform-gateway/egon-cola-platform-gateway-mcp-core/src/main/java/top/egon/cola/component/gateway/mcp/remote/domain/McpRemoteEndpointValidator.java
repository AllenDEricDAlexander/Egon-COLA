package top.egon.cola.component.gateway.mcp.remote.domain;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

/**
 * Shared control-plane and data-plane policy for remote MCP endpoints.
 * 补充说明 / Supplementary summary: {@code McpRemoteEndpointValidator} 是校验器，位于当前 Gateway 模块的相关包中，负责MCP远程Endpoint校验器相关的职责与边界。
 * English supplement: {@code McpRemoteEndpointValidator} is a mcp remote endpoint validator validator in the current Gateway module; it owns the mcp remote endpoint validator-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpRemoteEndpointValidator {

    /**
     * 中文说明：创建 {@code McpRemoteEndpointValidator} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpRemoteEndpointValidator} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    private McpRemoteEndpointValidator() {
    }

    /**
     * 中文说明：执行 requireSafe 操作；该方法是 {@code McpRemoteEndpointValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require safe operation; this method is the invocation entry point on {@code McpRemoteEndpointValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteEndpointValidator.requireSafe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param endpointReference 参数 endpointReference；parameter endpoint reference。
     * @return 返回 requireSafe 的处理结果；returns the result of the operation.
     */
    public static URI requireSafe(String endpointReference) {
        URI endpoint;
        try {
            endpoint = URI.create(endpointReference);
        } catch (RuntimeException failure) {
            throw invalidEndpoint(failure);
        }
        if (!endpoint.isAbsolute()
                || endpoint.getUserInfo() != null
                || endpoint.getHost() == null
                || endpoint.getQuery() != null
                || endpoint.getFragment() != null
                || !("http".equalsIgnoreCase(endpoint.getScheme())
                || "https".equalsIgnoreCase(endpoint.getScheme()))) {
            throw invalidEndpoint(null);
        }
        validatePath(endpoint.getRawPath());
        validateHost(endpoint.getHost());
        return endpoint;
    }

    /**
     * 中文说明：执行 validatePath 操作；该方法是 {@code McpRemoteEndpointValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate path operation; this method is the invocation entry point on {@code McpRemoteEndpointValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteEndpointValidator.validatePath(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param rawPath 参数 rawPath；parameter raw path。
     */
    private static void validatePath(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return;
        }
        String normalized = rawPath.toLowerCase(Locale.ROOT);
        if (normalized.contains("..")
                || normalized.contains("\\")
                || normalized.contains("%2e")
                || normalized.contains("%2f")
                || normalized.contains("%5c")) {
            throw invalidEndpoint(null);
        }
    }

    /**
     * 中文说明：执行 validateHost 操作；该方法是 {@code McpRemoteEndpointValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate host operation; this method is the invocation entry point on {@code McpRemoteEndpointValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteEndpointValidator.validateHost(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param host 参数 host；parameter host。
     */
    private static void validateHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.equals("metadata")
                || normalized.startsWith("metadata.")) {
            throw invalidEndpoint(null);
        }
        int[] ipv4 = parseIpv4(normalized);
        if (ipv4 != null) {
            if (ipv4[0] == 0
                    || ipv4[0] >= 224
                    || (ipv4[0] == 169 && ipv4[1] == 254)
                    || (ipv4[0] == 255 && ipv4[1] == 255
                    && ipv4[2] == 255 && ipv4[3] == 255)) {
                throw invalidEndpoint(null);
            }
            return;
        }
        if (normalized.indexOf(':') >= 0) {
            validateIpv6(normalized);
            return;
        }
        if (normalized.chars().allMatch(Character::isDigit)) {
            throw invalidEndpoint(null);
        }
    }

    /**
     * 中文说明：执行 parseIpv4 操作；该方法是 {@code McpRemoteEndpointValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the parse ipv4 operation; this method is the invocation entry point on {@code McpRemoteEndpointValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteEndpointValidator.parseIpv4(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param host 参数 host；parameter host。
     * @return 返回 parseIpv4 的处理结果；returns the result of the operation.
     */
    private static int[] parseIpv4(String host) {
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        int[] result = new int[4];
        for (int index = 0; index < parts.length; index++) {
            String part = parts[index];
            if (part.isEmpty()
                    || part.length() > 3
                    || (part.length() > 1 && part.charAt(0) == '0')
                    || !part.chars().allMatch(Character::isDigit)) {
                throw invalidEndpoint(null);
            }
            int value = Integer.parseInt(part);
            if (value > 255) {
                throw invalidEndpoint(null);
            }
            result[index] = value;
        }
        return result;
    }

    /**
     * 中文说明：执行 validateIpv6 操作；该方法是 {@code McpRemoteEndpointValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate ipv6 operation; this method is the invocation entry point on {@code McpRemoteEndpointValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteEndpointValidator.validateIpv6(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param host 参数 host；parameter host。
     */
    private static void validateIpv6(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            if (!(address instanceof Inet6Address)
                    || address.isAnyLocalAddress()
                    || address.isLinkLocalAddress()
                    || address.isMulticastAddress()) {
                throw invalidEndpoint(null);
            }
        } catch (IllegalArgumentException failure) {
            throw failure;
        } catch (Exception failure) {
            throw invalidEndpoint(failure);
        }
    }

    /**
     * 中文说明：执行 invalidEndpoint 操作；该方法是 {@code McpRemoteEndpointValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invalid endpoint operation; this method is the invocation entry point on {@code McpRemoteEndpointValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteEndpointValidator.invalidEndpoint(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param cause 参数 cause；parameter cause。
     * @return 返回 invalidEndpoint 的处理结果；returns the result of the operation.
     */
    private static IllegalArgumentException invalidEndpoint(
            Throwable cause) {
        return new IllegalArgumentException(
                "remote MCP endpoint must be a path-safe HTTP(S) URI "
                        + "without embedded credentials, query, fragment, "
                        + "or link-local/metadata address",
                cause
        );
    }
}
