package top.egon.cola.component.yuheng.edge.service.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Terminal session transitions are owned by the lifecycle lease. */
@Getter
@RequiredArgsConstructor
public enum EdgeSessionBindingStateEnum {
    NEW("NEW"),
    ACTIVE("ACTIVE"),
    CLOSING("CLOSING"),
    CLOSED("CLOSED");

    @JsonValue private final String code;
}
