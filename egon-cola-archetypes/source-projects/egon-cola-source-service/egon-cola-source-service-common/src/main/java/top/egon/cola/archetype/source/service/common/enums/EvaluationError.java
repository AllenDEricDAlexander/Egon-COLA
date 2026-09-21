package top.egon.cola.archetype.source.service.common.enums;

import top.egon.cola.component.common.core.enums.ErrorStatus;

/** Error code contract shared by the evaluation business failures. */
public interface EvaluationError extends ErrorStatus {

    /** The stable String code published on the wire; never derived from the declaration order. */
    String code();

    @Override
    default String getStatus() {
        return code();
    }
}
