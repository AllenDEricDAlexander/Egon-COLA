#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.client.organization;

public interface OrganizationDirectoryPort {

    OrganizationUser getUser(String userId);

    OrganizationSchoolClass getSchoolClass(String gradeId, String schoolClassId);
}
