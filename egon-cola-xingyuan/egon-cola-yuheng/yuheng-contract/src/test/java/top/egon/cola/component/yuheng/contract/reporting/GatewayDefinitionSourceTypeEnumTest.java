package top.egon.cola.component.yuheng.contract.reporting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class GatewayDefinitionSourceTypeEnumTest {

    @Test
    void exposesOnlyTheApprovedDefinitionSources() {
        assertArrayEquals(
                new String[]{"MANUAL", "RPC_DESCRIPTOR", "OPENAPI31"},
                java.util.Arrays.stream(GatewayDefinitionSourceTypeEnum.values())
                        .map(Enum::name)
                        .toArray(String[]::new)
        );
    }
}
