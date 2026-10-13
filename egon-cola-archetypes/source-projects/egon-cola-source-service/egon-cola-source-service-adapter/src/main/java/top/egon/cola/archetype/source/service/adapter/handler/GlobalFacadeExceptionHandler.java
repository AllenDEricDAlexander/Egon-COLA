package top.egon.cola.archetype.source.service.adapter.handler;

import top.egon.cola.component.common.core.exception.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.service.adapter.pojo.dto.FacadeFailureDTO;
import top.egon.cola.archetype.source.service.common.exception.ApplicationException;

/** Normalizes use-case rejections into the string code/message pair the wire envelope carries. */
@Component("globalFacadeExceptionHandler")
@Slf4j
public class GlobalFacadeExceptionHandler {

    private static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private static final String INTERNAL_MESSAGE = "service request failed";

    public FacadeFailureDTO toFailure(RuntimeException failure) {
        ExceptionLogger.log(log, failure);
        if (failure instanceof ApplicationException applicationFailure) {
            return new FacadeFailureDTO(applicationFailure.getStatus(), applicationFailure.getMessage());
        }
        return new FacadeFailureDTO(INTERNAL_ERROR, INTERNAL_MESSAGE);
    }
}
