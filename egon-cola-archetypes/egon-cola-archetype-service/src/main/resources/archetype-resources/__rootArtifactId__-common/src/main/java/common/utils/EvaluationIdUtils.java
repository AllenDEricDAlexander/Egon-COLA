#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.common.utils;

import java.util.UUID;

public final class EvaluationIdUtils {

    private EvaluationIdUtils() {
    }

    public static String nextId() {
        return UUID.randomUUID().toString();
    }
}
