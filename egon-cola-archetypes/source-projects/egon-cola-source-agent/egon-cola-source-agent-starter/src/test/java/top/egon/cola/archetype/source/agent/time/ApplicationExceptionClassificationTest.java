package top.egon.cola.archetype.source.agent.time;

import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.agent.common.exception.DeepResearchApplicationException;
import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Classification of the existing public application vocabulary, without starting a runtime. */
class ApplicationExceptionClassificationTest {
    @Test
    void expectedRejectionsAndOperationalFailuresKeepSeparateAlertSemantics() {
        assertFalse(new DeepResearchApplicationException(ResearchErrorCodeEnum.RESEARCH_VALIDATION_ERROR, "trace").isAlertRequired());
        assertTrue(new DeepResearchApplicationException(ResearchErrorCodeEnum.RESEARCH_DEPENDENCY_UNAVAILABLE, "trace").isAlertRequired());
    }
}
