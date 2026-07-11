#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exceptions;

public enum ApplicationErrorCode {
    COURSE_NOT_FOUND,
    COURSE_CODE_DUPLICATED,
    EXAM_NOT_FOUND,
    EXAM_PAPER_NOT_FOUND,
    SCORE_NOT_FOUND,
    VALIDATION_FAILED,
    BUSINESS_REJECTED,
    INFRASTRUCTURE_FAILURE
}
