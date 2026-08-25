#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.client.organization;

public interface OrganizationDirectoryPort {

    OrganizationUser getUser(Long userId);

    OrganizationSchoolClass getSchoolClass(Long gradeId, Long schoolClassId);
}
