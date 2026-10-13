package top.egon.cola.archetype.source.service.time;

import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.service.common.exception.ApplicationException;
import top.egon.cola.archetype.source.service.common.enums.ApplicationErrorCode;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Classification of the existing public application vocabulary, without starting a runtime. */
class ApplicationExceptionClassificationTest {
    @Test
    void expectedRejectionsAndOperationalFailuresKeepSeparateAlertSemantics() {
        assertFalse(new ApplicationException(ApplicationErrorCode.VALIDATION_FAILED, "invalid").isAlertRequired());
        assertTrue(new ApplicationException(ApplicationErrorCode.INFRASTRUCTURE_FAILURE, "unavailable").isAlertRequired());
    }
}
