#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.config;

import ${package}.domain.repos.course.CourseRepository;
import ${package}.domain.service.course.CourseDomainService;
import ${package}.domain.service.examing.ExamDomainService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("domainServiceConfiguration")
public class DomainServiceConfiguration {

    @Bean("courseDomainService")
    public CourseDomainService courseDomainService(
            @Qualifier("courseRepositoryImpl") CourseRepository courseRepository) {
        return new CourseDomainService(courseRepository);
    }

    @Bean("examDomainService")
    public ExamDomainService examDomainService(
            @Qualifier("courseRepositoryImpl") CourseRepository courseRepository) {
        return new ExamDomainService(courseRepository);
    }
}
