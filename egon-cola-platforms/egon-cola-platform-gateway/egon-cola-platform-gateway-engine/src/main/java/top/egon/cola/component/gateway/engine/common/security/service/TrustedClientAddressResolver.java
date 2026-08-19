package top.egon.cola.component.gateway.engine.common.security.service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 中文说明：{@code TrustedClientAddressResolver} 是类型，位于当前 Gateway 模块的相关包中，负责Trusted客户端AddressResolver相关的职责与边界。
 * English summary: {@code TrustedClientAddressResolver} is a type in the current Gateway module; it owns the trusted client address resolver-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class TrustedClientAddressResolver {

    /**
     * 中文说明：保存 trustedProxies 对应的状态、依赖或配置值；字段类型为 {@code List<Cidr>}，由 {@code TrustedClientAddressResolver} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by trusted proxies; its type is {@code List<Cidr>}, and {@code TrustedClientAddressResolver} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code TrustedClientAddressResolver} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrustedClientAddressResolver}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final List<Cidr> trustedProxies;

    /**
     * 中文说明：创建 {@code TrustedClientAddressResolver} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code TrustedClientAddressResolver} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param trustedProxyCidrs 参数 trusted代理Cidrs；parameter trusted proxy cidrs。
     */
    public TrustedClientAddressResolver(List<String> trustedProxyCidrs) {
        if (trustedProxyCidrs == null) {
            trustedProxies = List.of();
            return;
        }
        trustedProxies = trustedProxyCidrs.stream()
                .map(Cidr::parse)
                .toList();
    }

    /**
     * 中文说明：执行 resolve 操作；该方法是 {@code TrustedClientAddressResolver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resolve operation; this method is the invocation entry point on {@code TrustedClientAddressResolver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedClientAddressResolver.resolve(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param peer 参数 peer；parameter peer。
     * @param headers 参数 headers；parameter headers。
     * @return 返回 resolve 的处理结果；returns the result of the operation.
     */
    public InetAddress resolve(
            InetSocketAddress peer,
            Map<String, List<String>> headers) {
        if (peer == null || peer.getAddress() == null) {
            throw new IllegalArgumentException(
                    "resolved peer address is required"
            );
        }
        InetAddress peerAddress = peer.getAddress();
        if (!trusted(peerAddress)) {
            return peerAddress;
        }
        Optional<InetAddress> forwarded = firstHeader(
                headers,
                "forwarded"
        ).flatMap(this::forwardedAddress);
        if (forwarded.isPresent()) {
            return forwarded.get();
        }
        return firstHeader(headers, "x-forwarded-for")
                .flatMap(this::forwardedForAddress)
                .orElse(peerAddress);
    }

    /**
     * 中文说明：执行 trusted 操作；该方法是 {@code TrustedClientAddressResolver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the trusted operation; this method is the invocation entry point on {@code TrustedClientAddressResolver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedClientAddressResolver.trusted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param address 参数 address；parameter address。
     * @return 返回 trusted 的处理结果；returns the result of the operation.
     */
    public boolean trusted(InetAddress address) {
        return trustedProxies.stream().anyMatch(cidr -> cidr.contains(address));
    }

    /**
     * 中文说明：执行 firstHeader 操作；该方法是 {@code TrustedClientAddressResolver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the first header operation; this method is the invocation entry point on {@code TrustedClientAddressResolver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedClientAddressResolver.firstHeader(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @param expected 参数 expected；parameter expected。
     * @return 返回 firstHeader 的处理结果；returns the result of the operation.
     */
    private Optional<String> firstHeader(
            Map<String, List<String>> headers,
            String expected) {
        if (headers == null) {
            return Optional.empty();
        }
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(expected))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst();
    }

    /**
     * 中文说明：执行 forwardedAddress 操作；该方法是 {@code TrustedClientAddressResolver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the forwarded address operation; this method is the invocation entry point on {@code TrustedClientAddressResolver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedClientAddressResolver.forwardedAddress(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 forwardedAddress 的处理结果；returns the result of the operation.
     */
    private Optional<InetAddress> forwardedAddress(String value) {
        if (value == null || value.length() > 2048) {
            return Optional.empty();
        }
        String first = value.split(",", 2)[0];
        for (String parameter : first.split(";")) {
            String[] pair = parameter.trim().split("=", 2);
            if (pair.length == 2
                    && "for".equals(pair[0].toLowerCase(Locale.ROOT))) {
                return literal(addressPart(unquote(pair[1].trim())));
            }
        }
        return Optional.empty();
    }

    /**
     * 中文说明：执行 forwardedForAddress 操作；该方法是 {@code TrustedClientAddressResolver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the forwarded for address operation; this method is the invocation entry point on {@code TrustedClientAddressResolver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedClientAddressResolver.forwardedForAddress(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 forwardedForAddress 的处理结果；returns the result of the operation.
     */
    private Optional<InetAddress> forwardedForAddress(String value) {
        if (value == null || value.length() > 2048) {
            return Optional.empty();
        }
        return literal(addressPart(value.split(",", 2)[0].trim()));
    }

    /**
     * 中文说明：执行 unquote 操作；该方法是 {@code TrustedClientAddressResolver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the unquote operation; this method is the invocation entry point on {@code TrustedClientAddressResolver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedClientAddressResolver.unquote(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 unquote 的处理结果；returns the result of the operation.
     */
    private String unquote(String value) {
        return value.length() >= 2
                && value.startsWith("\"")
                && value.endsWith("\"")
                ? value.substring(1, value.length() - 1)
                : value;
    }

    /**
     * 中文说明：执行 addressPart 操作；该方法是 {@code TrustedClientAddressResolver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the address part operation; this method is the invocation entry point on {@code TrustedClientAddressResolver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedClientAddressResolver.addressPart(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 addressPart 的处理结果；returns the result of the operation.
     */
    private String addressPart(String value) {
        if (value.startsWith("[")) {
            int close = value.indexOf(']');
            return close > 1 ? value.substring(1, close) : "";
        }
        int colon = value.indexOf(':');
        return colon > 0 && value.indexOf(':', colon + 1) < 0
                ? value.substring(0, colon)
                : value;
    }

    /**
     * 中文说明：执行 literal 操作；该方法是 {@code TrustedClientAddressResolver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the literal operation; this method is the invocation entry point on {@code TrustedClientAddressResolver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedClientAddressResolver.literal(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 literal 的处理结果；returns the result of the operation.
     */
    private Optional<InetAddress> literal(String value) {
        if (value == null
                || value.isBlank()
                || value.contains("%")
                || (!ipv4(value) && !value.contains(":"))) {
            return Optional.empty();
        }
        try {
            return Optional.of(InetAddress.getByName(value));
        } catch (UnknownHostException invalid) {
            return Optional.empty();
        }
    }

    /**
     * 中文说明：执行 ipv4 操作；该方法是 {@code TrustedClientAddressResolver} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the ipv4 operation; this method is the invocation entry point on {@code TrustedClientAddressResolver} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code TrustedClientAddressResolver.ipv4(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 ipv4 的处理结果；returns the result of the operation.
     */
    private boolean ipv4(String value) {
        String[] segments = value.split("\\.", -1);
        if (segments.length != 4) {
            return false;
        }
        for (String segment : segments) {
            if (!segment.matches("[0-9]{1,3}")
                    || Integer.parseInt(segment) > 255) {
                return false;
            }
        }
        return true;
    }

    /**
     * 中文说明：{@code Cidr} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Cidr相关的职责与边界。
     * English summary: {@code Cidr} is an immutable data carrier in the current Gateway module; it owns the cidr-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param network 参数 network；parameter network。
     * @param prefixLength 参数 prefixLength；parameter prefix length。
     */
    private record Cidr(
    /**
     * 中文说明：保存 network 对应的状态、依赖或配置值；字段类型为 {@code byte[]}，由 {@code TrustedClientAddressResolver.Cidr} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by network; its type is {@code byte[]}, and {@code TrustedClientAddressResolver.Cidr} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code TrustedClientAddressResolver.Cidr} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrustedClientAddressResolver.Cidr}; do not couple callers to its representation when the owning type exposes an API.
     */
    byte[] network,
    /**
     * 中文说明：保存 prefixLength 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code TrustedClientAddressResolver.Cidr} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by prefix length; its type is {@code int}, and {@code TrustedClientAddressResolver.Cidr} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code TrustedClientAddressResolver.Cidr} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code TrustedClientAddressResolver.Cidr}; do not couple callers to its representation when the owning type exposes an API.
     */
    int prefixLength) {

        /**
         * 中文说明：执行 parse 操作；该方法是 {@code TrustedClientAddressResolver.Cidr} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the parse operation; this method is the invocation entry point on {@code TrustedClientAddressResolver.Cidr} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code TrustedClientAddressResolver.Cidr.parse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param value 参数 值；parameter value。
         * @return 返回 parse 的处理结果；returns the result of the operation.
         */
        private static Cidr parse(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        "trusted proxy CIDR is required"
                );
            }
            String[] parts = value.trim().split("/", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "trusted proxy must be CIDR"
                );
            }
            try {
                InetAddress address = InetAddress.getByName(parts[0]);
                int prefix = Integer.parseInt(parts[1]);
                int maximum = address.getAddress().length * 8;
                if (prefix < 0 || prefix > maximum) {
                    throw new IllegalArgumentException(
                            "invalid trusted proxy prefix"
                    );
                }
                return new Cidr(address.getAddress(), prefix);
            } catch (UnknownHostException | NumberFormatException invalid) {
                throw new IllegalArgumentException(
                        "invalid trusted proxy CIDR",
                        invalid
                );
            }
        }

        /**
         * 中文说明：执行 contains 操作；该方法是 {@code TrustedClientAddressResolver.Cidr} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the contains operation; this method is the invocation entry point on {@code TrustedClientAddressResolver.Cidr} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code TrustedClientAddressResolver.Cidr.contains(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param address 参数 address；parameter address。
         * @return 返回 contains 的处理结果；returns the result of the operation.
         */
        private boolean contains(InetAddress address) {
            byte[] candidate = address.getAddress();
            if (candidate.length != network.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int index = 0; index < fullBytes; index++) {
                if (candidate[index] != network[index]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xff << (8 - remainingBits);
            return (candidate[fullBytes] & mask)
                    == (network[fullBytes] & mask);
        }
    }
}
