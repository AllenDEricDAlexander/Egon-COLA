package top.egon.cola.component.gateway.contract.schema;

import com.google.protobuf.Descriptors;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.schema.proto.GatewaySchemaFieldOption;
import top.egon.cola.component.gateway.contract.schema.proto.SchemaOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GatewaySchemaOptionsContractTest {

    @Test
    void gatewaySchemaExtensionTargetsFieldOptions() {
        assertEquals(51001, SchemaOptions.gatewaySchema.getNumber());
        Descriptors.FieldDescriptor descriptor =
                SchemaOptions.gatewaySchema.getDescriptor();
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
