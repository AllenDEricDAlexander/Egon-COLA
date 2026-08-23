#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.common.enums;

public enum YesNoEnum {
    YES,
    NO;

    public boolean isYes() {
        return this == YES;
    }
}
