package top.egon.cola.component.common.mybatis.model;

import jakarta.validation.groups.Default;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;

import java.util.Objects;

/**
 * Repository-specific validation facade for EgonModel operations.
 */
public final class EgonColaModelValidationUtils {

    private final ValidationUtils validationUtils;
    private final EgonColaTenantIdProvider tenantIdProvider;

    public EgonColaModelValidationUtils(ValidationUtils validationUtils,
                                        EgonColaTenantIdProvider tenantIdProvider) {
        this.validationUtils = Objects.requireNonNull(validationUtils, "validationUtils must not be null");
        this.tenantIdProvider = Objects.requireNonNull(tenantIdProvider, "tenantIdProvider must not be null");
    }

    public <M extends EgonModel<M>> M validateBusiness(
            M model, EgonColaModelValidationGroups.Operation operation) {
        M checkedModel = requireModel(model);
        EgonColaModelValidationGroups.Operation checkedOperation = requireOperation(operation);
        validationUtils.validate(checkedModel, Default.class, checkedOperation.group());
        return checkedModel;
    }

    public <M extends EgonModel<M>> M validate(
            M model, EgonColaModelValidationGroups.Operation operation) {
        M checkedModel = requireModel(model);
        EgonColaModelValidationGroups.Operation checkedOperation = requireOperation(operation);
        if (checkedOperation == EgonColaModelValidationGroups.Operation.LOADED) {
            validationUtils.validate(checkedModel, Default.class,
                    EgonColaModelValidationGroups.Persisted.class);
        } else {
            validationUtils.validate(checkedModel, Default.class, checkedOperation.group(),
                    EgonColaModelValidationGroups.Persisted.class);
        }
        Long currentTenantId = tenantIdProvider.currentTenantId();
        if (currentTenantId == null || !currentTenantId.equals(checkedModel.getTenantId())) {
            throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
        }
        return checkedModel;
    }

    private static <M extends EgonModel<M>> M requireModel(M model) {
        if (model == null) {
            throw new IllegalArgumentException("model must not be null");
        }
        return model;
    }

    private static EgonColaModelValidationGroups.Operation requireOperation(
            EgonColaModelValidationGroups.Operation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        return operation;
    }
}
