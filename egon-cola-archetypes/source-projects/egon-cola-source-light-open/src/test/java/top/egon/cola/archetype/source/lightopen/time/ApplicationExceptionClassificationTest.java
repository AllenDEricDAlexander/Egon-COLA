package top.egon.cola.archetype.source.lightopen.time;

import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.lightopen.common.exception.BaseBusinessException;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Classification of the existing public application vocabulary, without starting a runtime. */
class ApplicationExceptionClassificationTest {
    @Test
    void expectedRejectionsAndOperationalFailuresKeepSeparateAlertSemantics() {
        assertFalse(new BaseBusinessException("VALIDATION", "invalid").isAlertRequired());
        assertTrue(new BaseBusinessException("DEPENDENCY", "unavailable", new CommonException(ResultCode.REMOTE_CALL_ERROR)).isAlertRequired());
    }
}
