package top.egon.cola.component.gateway.engine.rpc.domain;

import io.grpc.MethodDescriptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 中文说明：{@code RawByteMarshaller} 是枚举类型，位于当前 Gateway 模块的相关包中，负责RawByteMarshaller相关的职责与边界。
 * English summary: {@code RawByteMarshaller} is an enumeration in the current Gateway module; it owns the raw byte marshaller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public enum RawByteMarshaller
        implements MethodDescriptor.Marshaller<byte[]> {

    /**
     * 中文说明：表示 INSTANCE 这一固定值；它属于 {@code RawByteMarshaller} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value instance; it is a state, type, or protocol value of {@code RawByteMarshaller} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code RawByteMarshaller} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RawByteMarshaller}; do not couple callers to its representation when the owning type exposes an API.
     */
    INSTANCE;

    /**
     * 中文说明：执行 stream 操作；该方法是 {@code RawByteMarshaller} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the stream operation; this method is the invocation entry point on {@code RawByteMarshaller} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RawByteMarshaller.stream(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 stream 的处理结果；returns the result of the operation.
     */
    @Override
    public InputStream stream(byte[] value) {
        return new ByteArrayInputStream(value.clone());
    }

    /**
     * 中文说明：执行 parse 操作；该方法是 {@code RawByteMarshaller} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the parse operation; this method is the invocation entry point on {@code RawByteMarshaller} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RawByteMarshaller.parse(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param stream 参数 stream；parameter stream。
     * @return 返回 parse 的处理结果；returns the result of the operation.
     */
    @Override
    public byte[] parse(InputStream stream) {
        try {
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "failed to read protobuf message bytes",
                    exception
            );
        }
    }

    /**
     * 中文说明：执行 descriptor 操作；该方法是 {@code RawByteMarshaller} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the descriptor operation; this method is the invocation entry point on {@code RawByteMarshaller} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RawByteMarshaller.descriptor(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param fullMethodName 参数 full方法Name；parameter full method name。
     * @return 返回 descriptor 的处理结果；returns the result of the operation.
     */
    public MethodDescriptor<byte[], byte[]> descriptor(String fullMethodName) {
        return MethodDescriptor.<byte[], byte[]>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(fullMethodName)
                .setRequestMarshaller(this)
                .setResponseMarshaller(this)
                .build();
    }
}
