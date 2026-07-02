#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.handler;

import ${package}.common.exception.BizException;
import ${package}.common.response.Response;
import ${package}.common.response.SingleResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class ServiceExceptionHandler {

    private static final Log LOGGER = LogFactory.getLog(ServiceExceptionHandler.class);

    private static final String SYSTEM_ERROR = "SYSTEM_ERROR";

    public Response handle(BizException exception) {
        return Response.failure(exception.getCode(), exception.getMessage());
    }

    public Response handle(Exception exception) {
        LOGGER.error("unexpected service exception", exception);
        return Response.failure(SYSTEM_ERROR, "system error");
    }

    public <T> SingleResponse<T> handleSingle(BizException exception) {
        return SingleResponse.fail(exception.getCode(), exception.getMessage());
    }

    public <T> SingleResponse<T> handleSingle(Exception exception) {
        LOGGER.error("unexpected service exception", exception);
        return SingleResponse.fail(SYSTEM_ERROR, "system error");
    }
}
