package top.egon.cola.archetype.source.web.time;

import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.web.common.exception.OrganizationApplicationException;
import top.egon.cola.archetype.source.web.common.enums.OrganizationFailureType;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Classification of the existing public application vocabulary, without starting a runtime. */
class ApplicationExceptionClassificationTest {
    @Test
    void expectedRejectionsAndOperationalFailuresKeepSeparateAlertSemantics() {
        assertFalse(new OrganizationApplicationException(OrganizationFailureType.VALIDATION, "VALIDATION", "invalid").isAlertRequired());
        assertTrue(new OrganizationApplicationException(OrganizationFailureType.DEPENDENCY_UNAVAILABLE, "DEPENDENCY", "unavailable").isAlertRequired());
    }
}
