package top.egon.cola.archetype.source.serviceopen.adapter.course.validators;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.ScheduleCourseRequest;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/**
 * Adapter-boundary course rules.
 *
 * <p>The Protobuf request carries no Jakarta constraints, so its mapped carrier is validated here
 * before the remaining relation hook runs. Absent timestamps stay collapsed into epoch zero by the
 * wire, so only the ordering relation is asserted at this boundary.</p>
 */
@Component("courseFacadeValidator")
@RequiredArgsConstructor
@Slf4j
public class CourseFacadeValidator extends BaseValidator {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public <T> T validateCarrier(T carrier) {
        return validateBean(carrier);
    }

    public void require(ScheduleCourseRequest request) {
        requireWindow(request.getStartsAt(), request.getEndsAt());
    }

    private static void requireWindow(Timestamp startsAt, Timestamp endsAt) {
        if (startsAt.getSeconds() > endsAt.getSeconds()
                || (startsAt.getSeconds() == endsAt.getSeconds()
                && startsAt.getNanos() >= endsAt.getNanos())) {
            throw new IllegalArgumentException("starts_at must be before ends_at");
        }
    }
}
