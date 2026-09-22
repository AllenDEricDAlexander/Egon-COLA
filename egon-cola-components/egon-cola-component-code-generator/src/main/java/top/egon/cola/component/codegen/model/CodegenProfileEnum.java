package top.egon.cola.component.codegen.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Frozen native generator profiles. Values are configuration codes, not database codes.
 */
public enum CodegenProfileEnum {

    LIGHT("light"),
    WEB("web"),
    SERVICE("service");

    private final String code;

    CodegenProfileEnum(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    /**
     * Accepts only the documented lower-case codes. Agent, open and traditional profiles are rejected.
     *
     * @param value external project type
     * @return matching profile
     */
    @JsonCreator
    public static CodegenProfileEnum parse(String value) {
        if (value == null) {
            return null;
        }
        for (CodegenProfileEnum profile : values()) {
            if (profile.code.equals(value)) {
                return profile;
            }
        }
        throw new IllegalArgumentException("unsupported projectType: " + value);
    }

    public boolean allowsController() {
        return this == LIGHT || this == WEB;
    }
}
