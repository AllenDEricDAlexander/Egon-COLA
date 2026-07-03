#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.handler;

import ${package}.common.exception.BizException;
import ${package}.facade.dto.Response;
import ${package}.facade.dto.SingleResponse;
import jakarta.validation.ValidationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component("serviceExceptionHandler")
public class ServiceExceptionHandler {

    private static final Log LOGGER = LogFactory.getLog(ServiceExceptionHandler.class);

    private static final String SYSTEM_ERROR = "SYSTEM_ERROR";
    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    public Response handle(BizException exception) {
        return Response.failure(exception.getCode(), exception.getMessage());
    }

    public Response handle(ValidationException exception) {
        return Response.failure(VALIDATION_ERROR, exception.getMessage());
    }

    public Response handle(Exception exception) {
        LOGGER.error("unexpected service exception", exception);
        return Response.failure(SYSTEM_ERROR, "system error");
    }

    public <T> SingleResponse<T> handleSingle(BizException exception) {
        return SingleResponse.fail(exception.getCode(), exception.getMessage());
    }

    public <T> SingleResponse<T> handleSingle(ValidationException exception) {
        return SingleResponse.fail(VALIDATION_ERROR, exception.getMessage());
    }

    public <T> SingleResponse<T> handleSingle(Exception exception) {
        LOGGER.error("unexpected service exception", exception);
        return SingleResponse.fail(SYSTEM_ERROR, "system error");
    }
}
