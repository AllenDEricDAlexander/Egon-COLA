package top.egon.cola.component.common.mybatis.contract;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusContractValidator;

import java.sql.PreparedStatement;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EgonColaEnumContractTest {

    @Test
    void persistedEnumsRequireOneUniqueNonnullCodeAndSharedJsonUsesThatCode() throws Exception {
        validate(ExternalStatusEnum.class, true);
        ObjectMapper mapper = new ObjectMapper();
        assertThat(mapper.writeValueAsString(ExternalStatusEnum.READY)).isEqualTo("10");
        assertThat(mapper.readValue("10", ExternalStatusEnum.class)).isEqualTo(ExternalStatusEnum.READY);
        assertThatThrownBy(() -> mapper.readValue("999", ExternalStatusEnum.class)).isInstanceOf(com.fasterxml.jackson.databind.exc.InvalidFormatException.class);
        assertThatThrownBy(() -> mapper.readValue("0", ExternalStatusEnum.class)).isInstanceOf(com.fasterxml.jackson.databind.exc.InvalidFormatException.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        new MybatisEnumTypeHandler<>(ExternalStatusEnum.class).setParameter(statement, 1, ExternalStatusEnum.READY, JdbcType.INTEGER);
        verify(statement).setObject(1, 10, JdbcType.INTEGER.TYPE_CODE);
    }

    @Test
    void storageOnlyEnumsDoNotNeedJsonAnnotationsButExternalOnesDo() {
        validate(StorageStatusEnum.class, false);
        assertThatThrownBy(() -> validate(StorageStatusEnum.class, true)).hasMessageContaining("ENUM_JSON_VALUE_REQUIRED");
        assertThatThrownBy(() -> validate(UnmappedStatusEnum.class, false)).hasMessageContaining("ENUM_VALUE_REQUIRED");
        assertThatThrownBy(() -> validate(DuplicateStatusEnum.class, false)).hasMessageContaining("ENUM_CODE_INVALID");
    }

    private static void validate(Class<?> type, boolean external) {
        ReflectionTestUtils.invokeMethod(EgonColaMybatisPlusContractValidator.class, "validateEnumContract", type, external);
    }

    enum ExternalStatusEnum {
        READY(10), DONE(20);
        @EnumValue @JsonValue
        private final int code;
        ExternalStatusEnum(int code) { this.code = code; }
    }

    enum StorageStatusEnum {
        READY;
        @EnumValue
        private final String code = "ready";
    }

    enum UnmappedStatusEnum { READY }

    enum DuplicateStatusEnum {
        A, B;
        @EnumValue
        private final int code = 1;
    }
}
