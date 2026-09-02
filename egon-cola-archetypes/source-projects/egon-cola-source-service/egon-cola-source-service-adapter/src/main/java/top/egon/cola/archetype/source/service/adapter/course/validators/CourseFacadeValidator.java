package top.egon.cola.archetype.source.service.adapter.course.validators;
import top.egon.cola.evaluation.facade.enums.EvaluationFacadeErrorCode;
import top.egon.cola.evaluation.facade.exceptions.EvaluationFacadeException;
import org.springframework.stereotype.Component;
@Component
public class CourseFacadeValidator {
    public void require(Object request) {
        if (request == null) throw new EvaluationFacadeException(
                EvaluationFacadeErrorCode.VALIDATION_FAILED, "request must not be null");
    }
}
