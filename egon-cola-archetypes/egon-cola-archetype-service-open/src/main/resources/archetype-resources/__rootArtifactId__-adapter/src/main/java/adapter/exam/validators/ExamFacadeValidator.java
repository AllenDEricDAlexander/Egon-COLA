#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.validators;
import top.egon.cola.evaluation.facade.enums.EvaluationFacadeErrorCode;
import top.egon.cola.evaluation.facade.exceptions.EvaluationFacadeException;
import org.springframework.stereotype.Component;
@Component
public class ExamFacadeValidator {
    public void require(Object request) {
        if (request == null) throw new EvaluationFacadeException(
                EvaluationFacadeErrorCode.VALIDATION_FAILED, "request must not be null");
    }
}
