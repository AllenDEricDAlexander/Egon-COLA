package top.egon.cola.component.tianshu.model.ai.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import top.egon.cola.component.tianshu.model.ai.DdcAiResourceQuery;

public class AiVersionSelectorValidator
        implements ConstraintValidator<ConsistentAiVersionSelector, DdcAiResourceQuery> {

    @Override
    public boolean isValid(DdcAiResourceQuery query, ConstraintValidatorContext context) {
        return query == null || query.versionNo() == null || query.resourceId() != null;
    }
}
