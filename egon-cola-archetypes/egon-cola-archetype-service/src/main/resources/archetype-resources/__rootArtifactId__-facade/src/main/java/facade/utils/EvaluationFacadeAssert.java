#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.utils;

import ${package}.facade.enums.EvaluationFacadeErrorCode;
import ${package}.facade.exceptions.EvaluationFacadeException;

public final class EvaluationFacadeAssert {

    private EvaluationFacadeAssert() {
    }

    public static void notBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new EvaluationFacadeException(
                    EvaluationFacadeErrorCode.VALIDATION_FAILED,
                    field + " must not be blank");
        }
    }
}
