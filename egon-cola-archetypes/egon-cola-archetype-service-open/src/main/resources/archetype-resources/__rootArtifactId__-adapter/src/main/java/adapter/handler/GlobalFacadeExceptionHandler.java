#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.handler;
import ${package}.application.exceptions.ApplicationException;
import top.egon.cola.evaluation.facade.dto.SingleResponse;
import top.egon.cola.evaluation.facade.exceptions.EvaluationFacadeException;
import org.springframework.stereotype.Component;
@Component
public class GlobalFacadeExceptionHandler {
    public <T> SingleResponse<T> toFailure(ApplicationException failure) {
        return SingleResponse.fail(failure.code().name(), failure.getMessage());
    }
    public <T> SingleResponse<T> toFailure(RuntimeException failure) {
        if (failure instanceof ApplicationException applicationFailure) {
            return toFailure(applicationFailure);
        }
        if (failure instanceof EvaluationFacadeException facadeFailure) {
            return SingleResponse.fail(facadeFailure.code().name(), facadeFailure.getMessage());
        }
        return SingleResponse.fail("INTERNAL_ERROR", "service request failed");
    }
}
