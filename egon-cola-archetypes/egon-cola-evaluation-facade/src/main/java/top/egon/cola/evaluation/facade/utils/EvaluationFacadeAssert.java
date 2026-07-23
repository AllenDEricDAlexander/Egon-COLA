package top.egon.cola.evaluation.facade.utils;

import top.egon.cola.evaluation.facade.enums.EvaluationFacadeErrorCode;
import top.egon.cola.evaluation.facade.exceptions.EvaluationFacadeException;

public final class EvaluationFacadeAssert {

    private EvaluationFacadeAssert() {
    }

    public static void notBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new EvaluationFacadeException(
                    EvaluationFacadeErrorCode.VALIDATION_FAILED,
                    field + " must not be blank");
        }
    }
}
