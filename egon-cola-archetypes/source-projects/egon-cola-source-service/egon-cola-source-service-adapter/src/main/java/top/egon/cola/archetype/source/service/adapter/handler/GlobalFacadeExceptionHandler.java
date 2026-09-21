package top.egon.cola.archetype.source.service.adapter.handler;

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
        if (failure instanceof ApplicationException applicationFailure) {
            return new FacadeFailureDTO(applicationFailure.getStatus(), applicationFailure.getMessage());
        }
        log.warn("unexpected native facade failure masked as {}", INTERNAL_ERROR, failure);
        return new FacadeFailureDTO(INTERNAL_ERROR, INTERNAL_MESSAGE);
    }
}
