#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.config;

import ${package}.domain.service.course.CourseDomainService;
import ${package}.domain.service.course.impl.CourseDomainServiceImpl;
import ${package}.domain.service.exam.ExamDomainService;
import ${package}.domain.service.exam.ScoreDomainService;
import ${package}.domain.service.exam.impl.ExamDomainServiceImpl;
import ${package}.domain.service.exam.impl.ScoreDomainServiceImpl;
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
