#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.validators;
import ${package}.facade.enums.EvaluationFacadeErrorCode;
import ${package}.facade.exceptions.EvaluationFacadeException;
import org.springframework.stereotype.Component;
@Component
public class ScoreFacadeValidator {
    public void require(Object request) {
        if (request == null) throw new EvaluationFacadeException(
                EvaluationFacadeErrorCode.VALIDATION_FAILED, "request must not be null");
    }
}
