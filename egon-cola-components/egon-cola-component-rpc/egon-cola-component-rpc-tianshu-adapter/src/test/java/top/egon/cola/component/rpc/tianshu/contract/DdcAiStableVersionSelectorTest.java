package top.egon.cola.component.rpc.tianshu.contract;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.model.ai.DdcAiResourceQuery;
import top.egon.cola.component.tianshu.model.ai.DdcAiValidationGroups;
import top.egon.cola.component.tianshu.model.ai.validation.ConsistentAiVersionSelector;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAiStableVersionSelectorTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void versionNumberRequiresTheResourceIdentity() {
        DdcAiResourceQuery query = new DdcAiResourceQuery("dev-space", "review-order", 3L, null);
        assertThat(validator.validate(query, DdcAiValidationGroups.Read.class))
                .anyMatch(violation -> violation.getConstraintDescriptor().getAnnotation()
                        .annotationType().equals(ConsistentAiVersionSelector.class));
    }

    @Test
    void supportsCurrentLookupAndFixedRootCurrentOrVersionedLookup() {
        DdcAiResourceQuery current = new DdcAiResourceQuery("dev-space", "review-order", null, null);
        DdcAiResourceQuery rootCurrent = new DdcAiResourceQuery("dev-space", "review-order", null, 41L);
        DdcAiResourceQuery fixedVersion = new DdcAiResourceQuery("dev-space", "review-order", 3L, 41L);

        assertThat(validator.validate(current, DdcAiValidationGroups.Read.class)).isEmpty();
        assertThat(validator.validate(rootCurrent, DdcAiValidationGroups.Read.class)).isEmpty();
        assertThat(validator.validate(fixedVersion, DdcAiValidationGroups.Read.class)).isEmpty();
        assertThat(fixedVersion.resourceId()).isEqualTo(41L);
        assertThat(fixedVersion.versionNo()).isEqualTo(3L);
    }

    @Test
    void rejectsNonPositiveIdentityAndVersionValues() {
        DdcAiResourceQuery query = new DdcAiResourceQuery("dev-space", "review-order", 0L, 0L);
        assertThat(validator.validate(query, DdcAiValidationGroups.Read.class)).isNotEmpty();
    }
}
