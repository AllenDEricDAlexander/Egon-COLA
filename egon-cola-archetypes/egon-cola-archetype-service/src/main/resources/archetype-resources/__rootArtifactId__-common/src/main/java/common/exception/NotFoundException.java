#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.common.exception;

public class NotFoundException extends BizException {

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
