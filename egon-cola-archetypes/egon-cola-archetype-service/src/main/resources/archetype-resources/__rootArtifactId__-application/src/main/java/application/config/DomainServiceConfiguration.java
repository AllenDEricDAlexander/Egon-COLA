#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.config;

import ${package}.domain.client.course.CourseClient;
import ${package}.domain.service.examing.ExamDomainService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("domainServiceConfiguration")
public class DomainServiceConfiguration {

    @Bean("examDomainService")
    public ExamDomainService examDomainService(
            @Qualifier("courseClientImpl") CourseClient courseClient) {
        return new ExamDomainService(courseClient);
    }
}
