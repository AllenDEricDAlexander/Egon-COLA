package top.egon.cola.component.gateway.engine.rpc;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code ProtobufDescriptorRegistry} 是类型，位于当前 Gateway 模块的相关包中，负责ProtobufDescriptor注册表相关的职责与边界。
 * English summary: {@code ProtobufDescriptorRegistry} is a type in the current Gateway module; it owns the protobuf descriptor registry-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class ProtobufDescriptorRegistry {

    /**
     * 中文说明：保存 messages 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Descriptors.Descriptor>}，由 {@code ProtobufDescriptorRegistry} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by messages; its type is {@code Map<String, Descriptors.Descriptor>}, and {@code ProtobufDescriptorRegistry} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProtobufDescriptorRegistry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProtobufDescriptorRegistry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, Descriptors.Descriptor> messages;

    /**
     * 中文说明：创建 {@code ProtobufDescriptorRegistry} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ProtobufDescriptorRegistry} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param descriptorSetBytes 参数 descriptorSetBytes；parameter descriptor set bytes。
     */
    public ProtobufDescriptorRegistry(byte[] descriptorSetBytes) {
        try {
            DescriptorProtos.FileDescriptorSet set =
                    DescriptorProtos.FileDescriptorSet.parseFrom(
                            descriptorSetBytes
                    );
            Map<String, DescriptorProtos.FileDescriptorProto> pending =
                    new LinkedHashMap<>();
            set.getFileList().forEach(file -> pending.put(file.getName(), file));
            Map<String, Descriptors.FileDescriptor> built =
                    new LinkedHashMap<>();
            while (!pending.isEmpty()) {
                boolean progressed = false;
                for (String name : List.copyOf(pending.keySet())) {
                    DescriptorProtos.FileDescriptorProto file =
                            pending.get(name);
                    if (!built.keySet().containsAll(
                            file.getDependencyList()
                    )) {
                        continue;
                    }
                    Descriptors.FileDescriptor[] dependencies =
                            file.getDependencyList().stream()
                                    .map(built::get)
                                    .toArray(Descriptors.FileDescriptor[]::new);
                    built.put(
                            name,
                            Descriptors.FileDescriptor.buildFrom(
                                    file,
                                    dependencies
                            )
                    );
                    pending.remove(name);
                    progressed = true;
                }
                if (!progressed) {
                    throw new IllegalArgumentException(
                            "descriptor set has missing or cyclic dependencies"
                    );
                }
            }
            Map<String, Descriptors.Descriptor> indexed =
                    new LinkedHashMap<>();
            built.values().forEach(file -> file.getMessageTypes()
                    .forEach(message -> index(message, indexed)));
            messages = Map.copyOf(indexed);
        } catch (com.google.protobuf.InvalidProtocolBufferException
                 | Descriptors.DescriptorValidationException failure) {
            throw new IllegalArgumentException(
                    "invalid protobuf descriptor set",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 消息 操作；该方法是 {@code ProtobufDescriptorRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the message operation; this method is the invocation entry point on {@code ProtobufDescriptorRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProtobufDescriptorRegistry.message(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param fullName 参数 fullName；parameter full name。
     * @return 返回 消息 的处理结果；returns the result of the operation.
     */
    public Descriptors.Descriptor message(String fullName) {
        Descriptors.Descriptor descriptor = messages.get(fullName);
        if (descriptor == null) {
            throw new IllegalArgumentException(
                    "unknown protobuf message: " + fullName
            );
        }
        return descriptor;
    }

    /**
     * 中文说明：执行 索引 操作；该方法是 {@code ProtobufDescriptorRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the index operation; this method is the invocation entry point on {@code ProtobufDescriptorRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProtobufDescriptorRegistry.index(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param descriptor 参数 descriptor；parameter descriptor。
     * @param target 参数 target；parameter target。
     */
    private void index(
            Descriptors.Descriptor descriptor,
            Map<String, Descriptors.Descriptor> target) {
        target.put(descriptor.getFullName(), descriptor);
        descriptor.getNestedTypes().forEach(nested -> index(nested, target));
    }
}
