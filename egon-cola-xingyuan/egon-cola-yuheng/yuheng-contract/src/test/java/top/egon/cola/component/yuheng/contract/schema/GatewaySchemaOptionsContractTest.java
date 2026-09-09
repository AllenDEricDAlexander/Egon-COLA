package top.egon.cola.component.yuheng.contract.schema;

import com.google.protobuf.Descriptors;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.contract.schema.proto.GatewaySchemaFieldOption;
import top.egon.cola.component.yuheng.contract.schema.proto.SchemaOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GatewaySchemaOptionsContractTest {

    @Test
    void yuhengSchemaExtensionTargetsFieldOptions() {
        assertEquals(51001, SchemaOptions.yuhengSchema.getNumber());
        Descriptors.FieldDescriptor descriptor =
                SchemaOptions.yuhengSchema.getDescriptor();
        assertEquals(
                "google.protobuf.FieldOptions",
                descriptor.getContainingType().getFullName()
        );
        assertEquals(
                GatewaySchemaFieldOption.getDescriptor(),
                descriptor.getMessageType()
        );
    }

    @Test
    void optionMessageExposesAllBusinessMetadataFields() {
        assertNotNull(GatewaySchemaFieldOption.getDescriptor()
                .findFieldByName("description"));
        assertNotNull(GatewaySchemaFieldOption.getDescriptor()
                .findFieldByName("format"));
        assertNotNull(GatewaySchemaFieldOption.getDescriptor()
                .findFieldByName("required"));
        assertNotNull(GatewaySchemaFieldOption.getDescriptor()
                .findFieldByName("example"));
    }
}
