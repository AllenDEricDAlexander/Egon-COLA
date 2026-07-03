package top.egon.fable.adapter.handler;

import top.egon.fable.common.exception.BizException;
import top.egon.fable.facade.dto.Response;
import top.egon.fable.facade.dto.SingleResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component("serviceExceptionHandler")
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
