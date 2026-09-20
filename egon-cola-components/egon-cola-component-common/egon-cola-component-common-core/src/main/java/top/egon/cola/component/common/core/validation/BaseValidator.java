package top.egon.cola.component.common.core.validation;

/**
 * Shared guard for every governed validator.
 *
 * <p>Subclasses contribute only the relation or protocol rules that native constraints cannot
 * express; the bean validation itself always runs through the common {@link ValidationUtils}.</p>
 */
public abstract class BaseValidator {

    protected abstract ValidationUtils getValidationUtils();

    protected final <T> T validateBean(T target, Class<?>... groups) {
        return getValidationUtils().validate(target, groups);
    }
}
