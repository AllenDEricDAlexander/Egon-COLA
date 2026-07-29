package top.egon.cola.component.ddc.admin.config;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.common.DdcErrorStatus;

import static org.assertj.core.api.Assertions.assertThat;

class DdcGlobalExceptionHandlerTest {

    private final DdcGlobalExceptionHandler handler =
            new DdcGlobalExceptionHandler();

    @Test
    void preservesAdminExceptionStatus() {
        ResultRecord<Void> result = handler.handleCommon(
                new DdcAdminException("service registration is invalid")
        );

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo(
                DdcErrorStatus.INVALID_REQUEST.getCode()
        );
        assertThat(result.status()).isEqualTo(
                DdcErrorStatus.INVALID_REQUEST.getStatus()
        );
        assertThat(result.message()).isEqualTo(
                "service registration is invalid"
        );
    }
}
