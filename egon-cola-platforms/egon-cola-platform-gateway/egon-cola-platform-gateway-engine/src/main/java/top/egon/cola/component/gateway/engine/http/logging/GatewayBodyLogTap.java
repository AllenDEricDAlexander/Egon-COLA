package top.egon.cola.component.gateway.engine.http.logging;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Optional body-log decorator that never owns or consumes a DataBuffer.
 * 补充说明 / Supplementary summary: {@code GatewayBodyLogTap} 是类型，位于当前 Gateway 模块的相关包中，负责网关BodyLogTap相关的职责与边界。
 * English supplement: {@code GatewayBodyLogTap} is a type in the current Gateway module; it owns the gateway body log tap-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayBodyLogTap {

    /**
     * 中文说明：表示 DEFAULTSAMPLEBYTES 这一固定值；它属于 {@code GatewayBodyLogTap} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value default sample bytes; it is a state, type, or protocol value of {@code GatewayBodyLogTap} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayBodyLogTap} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayBodyLogTap}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final int DEFAULT_SAMPLE_BYTES = 8 * 1024;

    /**
     * 中文说明：表示 MAXSAMPLEBYTES 这一固定值；它属于 {@code GatewayBodyLogTap} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value max sample bytes; it is a state, type, or protocol value of {@code GatewayBodyLogTap} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayBodyLogTap} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayBodyLogTap}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final int MAX_SAMPLE_BYTES = 64 * 1024;

    /**
     * 中文说明：创建 {@code GatewayBodyLogTap} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayBodyLogTap} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    private GatewayBodyLogTap() {
    }

    /**
     * 中文说明：执行 tap 操作；该方法是 {@code GatewayBodyLogTap} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the tap operation; this method is the invocation entry point on {@code GatewayBodyLogTap} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBodyLogTap.tap(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param enabled 参数 enabled；parameter enabled。
     * @param contentType 参数 contentType；parameter content type。
     * @param direction 参数 direction；parameter direction。
     * @param requestedSampleBytes 参数 requestedSampleBytes；parameter requested sample bytes。
     * @param observer 参数 observer；parameter observer。
     * @return 返回 tap 的处理结果；returns the result of the operation.
     */
    public static Flux<DataBuffer> tap(
            Flux<DataBuffer> source,
            boolean enabled,
            String contentType,
            GatewayBodyLogDirection direction,
            int requestedSampleBytes,
            Consumer<GatewayBodyLogEvent> observer) {
        Objects.requireNonNull(source, "source");
        if (!enabled) {
            return source;
        }
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(observer, "observer");
        int sampleLimit = Math.min(
                requirePositive(requestedSampleBytes),
                MAX_SAMPLE_BYTES
        );
        String normalizedContentType = normalize(contentType);
        boolean metadataOnly = direction
                == GatewayBodyLogDirection.WEBSOCKET
                || metadataOnly(normalizedContentType);
        return Flux.defer(() -> {
            AtomicLong totalBytes = new AtomicLong();
            ByteArrayOutputStream sample = new ByteArrayOutputStream(
                    metadataOnly ? 0 : Math.min(sampleLimit, 1024)
            );
            return source.doOnNext(buffer -> {
                totalBytes.addAndGet(buffer.readableByteCount());
                if (!metadataOnly && sample.size() < sampleLimit) {
                    copySample(buffer, sample, sampleLimit);
                }
            }).doFinally(ignored -> notifyObserver(
                    observer,
                    new GatewayBodyLogEvent(
                            direction,
                            normalizedContentType,
                            totalBytes.get(),
                            metadataOnly,
                            sample.toByteArray()
                    )
            ));
        });
    }

    /**
     * 中文说明：执行 safeHeaderNames 操作；该方法是 {@code GatewayBodyLogTap} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe header names operation; this method is the invocation entry point on {@code GatewayBodyLogTap} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBodyLogTap.safeHeaderNames(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headerNames 参数 headerNames；parameter header names。
     * @return 返回 safeHeaderNames 的处理结果；returns the result of the operation.
     */
    public static List<String> safeHeaderNames(
            Iterable<String> headerNames) {
        Objects.requireNonNull(headerNames, "headerNames");
        List<String> safe = new ArrayList<>();
        for (String name : headerNames) {
            if (name != null && !credentialHeader(name)) {
                safe.add(name);
            }
        }
        return List.copyOf(safe);
    }

    /**
     * 中文说明：执行 copySample 操作；该方法是 {@code GatewayBodyLogTap} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the copy sample operation; this method is the invocation entry point on {@code GatewayBodyLogTap} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBodyLogTap.copySample(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param buffer 参数 缓冲区；parameter buffer。
     * @param sample 参数 sample；parameter sample。
     * @param sampleLimit 参数 sampleLimit；parameter sample limit。
     */
    private static void copySample(
            DataBuffer buffer,
            ByteArrayOutputStream sample,
            int sampleLimit) {
        int remaining = sampleLimit - sample.size();
        try (DataBuffer.ByteBufferIterator buffers =
                     buffer.readableByteBuffers()) {
            while (buffers.hasNext() && remaining > 0) {
                ByteBuffer bytes = buffers.next().duplicate();
                int count = Math.min(remaining, bytes.remaining());
                byte[] chunk = new byte[count];
                bytes.get(chunk);
                sample.writeBytes(chunk);
                remaining -= count;
            }
        }
    }

    /**
     * 中文说明：执行 notifyObserver 操作；该方法是 {@code GatewayBodyLogTap} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the notify observer operation; this method is the invocation entry point on {@code GatewayBodyLogTap} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBodyLogTap.notifyObserver(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param observer 参数 observer；parameter observer。
     * @param event 参数 事件；parameter event。
     */
    private static void notifyObserver(
            Consumer<GatewayBodyLogEvent> observer,
            GatewayBodyLogEvent event) {
        try {
            observer.accept(event);
        } catch (RuntimeException ignored) {
            // Logging is passive and must not change transport behavior.
        }
    }

    /**
     * 中文说明：执行 元数据Only 操作；该方法是 {@code GatewayBodyLogTap} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the metadata only operation; this method is the invocation entry point on {@code GatewayBodyLogTap} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBodyLogTap.metadataOnly(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param contentType 参数 contentType；parameter content type。
     * @return 返回 元数据Only 的处理结果；returns the result of the operation.
     */
    private static boolean metadataOnly(String contentType) {
        return contentType.startsWith("multipart/")
                || contentType.startsWith("image/")
                || contentType.startsWith("audio/")
                || "application/octet-stream".equals(contentType);
    }

    /**
     * 中文说明：执行 凭证Header 操作；该方法是 {@code GatewayBodyLogTap} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the credential header operation; this method is the invocation entry point on {@code GatewayBodyLogTap} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBodyLogTap.credentialHeader(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param name 参数 name；parameter name。
     * @return 返回 凭证Header 的处理结果；returns the result of the operation.
     */
    private static boolean credentialHeader(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return "authorization".equals(normalized)
                || "proxy-authorization".equals(normalized)
                || "cookie".equals(normalized)
                || "set-cookie".equals(normalized)
                || normalized.contains("api-key");
    }

    /**
     * 中文说明：执行 normalize 操作；该方法是 {@code GatewayBodyLogTap} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the normalize operation; this method is the invocation entry point on {@code GatewayBodyLogTap} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBodyLogTap.normalize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param contentType 参数 contentType；parameter content type。
     * @return 返回 normalize 的处理结果；returns the result of the operation.
     */
    private static String normalize(String contentType) {
        if (contentType == null) {
            return "";
        }
        int parameters = contentType.indexOf(';');
        String mediaType = parameters < 0
                ? contentType
                : contentType.substring(0, parameters);
        return mediaType.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 中文说明：执行 requirePositive 操作；该方法是 {@code GatewayBodyLogTap} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require positive operation; this method is the invocation entry point on {@code GatewayBodyLogTap} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBodyLogTap.requirePositive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 requirePositive 的处理结果；returns the result of the operation.
     */
    private static int requirePositive(int value) {
        if (value < 1) {
            throw new IllegalArgumentException(
                    "requestedSampleBytes must be positive"
            );
        }
        return value;
    }
}
