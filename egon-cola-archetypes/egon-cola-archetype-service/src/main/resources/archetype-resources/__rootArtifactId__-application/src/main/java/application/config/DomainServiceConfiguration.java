#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.config;

import ${package}.domain.course.service.CourseDomainService;
import ${package}.domain.course.service.impl.CourseDomainServiceImpl;
import ${package}.domain.exam.service.ExamDomainService;
import ${package}.domain.exam.service.ScoreDomainService;
import ${package}.domain.exam.service.impl.ExamDomainServiceImpl;
import ${package}.domain.exam.service.impl.ScoreDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("domainServiceConfiguration")
public class DomainServiceConfiguration {

    @Bean("courseDomainService")
    public CourseDomainService courseDomainService() {
        return new CourseDomainServiceImpl();
    }

    @Bean("examDomainService")
    public ExamDomainService examDomainService() {
        return new ExamDomainServiceImpl();
    }

    @Bean("scoreDomainService")
    public ScoreDomainService scoreDomainService() {
        return new ScoreDomainServiceImpl();
    }
}
