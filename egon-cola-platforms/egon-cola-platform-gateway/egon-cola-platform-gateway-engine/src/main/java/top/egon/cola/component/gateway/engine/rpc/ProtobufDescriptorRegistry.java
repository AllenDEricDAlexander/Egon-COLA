package top.egon.cola.component.gateway.engine.rpc;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProtobufDescriptorRegistry {

    private final Map<String, Descriptors.Descriptor> messages;

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

    public Descriptors.Descriptor message(String fullName) {
        Descriptors.Descriptor descriptor = messages.get(fullName);
        if (descriptor == null) {
            throw new IllegalArgumentException(
                    "unknown protobuf message: " + fullName
            );
        }
        return descriptor;
    }

    private void index(
            Descriptors.Descriptor descriptor,
            Map<String, Descriptors.Descriptor> target) {
        target.put(descriptor.getFullName(), descriptor);
        descriptor.getNestedTypes().forEach(nested -> index(nested, target));
    }
}
