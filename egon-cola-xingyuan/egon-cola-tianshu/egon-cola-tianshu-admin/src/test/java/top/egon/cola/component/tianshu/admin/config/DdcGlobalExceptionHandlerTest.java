package top.egon.cola.component.tianshu.admin.config;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;
import top.egon.cola.component.tianshu.admin.security.registration.DdcRegistrationAuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.assertj.core.api.Assertions.assertThat;

class DdcGlobalExceptionHandlerTest {

    @Test
    void admissionUnavailableHttp503PreservesResultRecord() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new AdmissionFixtureController()).setControllerAdvice(handler).build();
        mvc.perform(get("/fixture/admission/unavailable")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(56028))
                .andExpect(jsonPath("$.status").value("TIANSHU_RESOURCE_ADMISSION_UNAVAILABLE"));
        mvc.perform(get("/fixture/admission/invalid")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(56025)).andExpect(jsonPath("$.status").value("TIANSHU_RESOURCE_ADMISSION_INVALID"));
        mvc.perform(get("/fixture/admission/expired")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(56026)).andExpect(jsonPath("$.status").value("TIANSHU_RESOURCE_ADMISSION_EXPIRED"));
    }

    @RestController("admissionStatusFixtureController")
    static class AdmissionFixtureController {
        @GetMapping("/fixture/admission/unavailable") void unavailable() { throw new DdcRegistrationAuthenticationException(DdcErrorStatus.RESOURCE_ADMISSION_UNAVAILABLE); }
        @GetMapping("/fixture/admission/invalid") void invalid() { throw new DdcRegistrationAuthenticationException(DdcErrorStatus.RESOURCE_ADMISSION_INVALID); }
        @GetMapping("/fixture/admission/expired") void expired() { throw new DdcRegistrationAuthenticationException(DdcErrorStatus.RESOURCE_ADMISSION_EXPIRED); }
    }

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
