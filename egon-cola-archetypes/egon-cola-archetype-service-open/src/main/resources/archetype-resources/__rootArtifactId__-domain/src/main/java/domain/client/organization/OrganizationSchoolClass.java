#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.client.organization;

import java.util.List;

public record OrganizationSchoolClass(
        long id,
        String name,
        String gradeCode,
        String status,
        List<Long> userIds) {

    public OrganizationSchoolClass {
        userIds = List.copyOf(userIds);
    }
}
