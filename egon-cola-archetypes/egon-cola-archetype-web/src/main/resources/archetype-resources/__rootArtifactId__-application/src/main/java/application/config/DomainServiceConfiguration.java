package ${package}.application.config;

import ${package}.application.user.assemblers.UserAssembler;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.domain.teaching.service.SchoolClassDomainService;
import ${package}.domain.teaching.service.GradeDomainService;
import ${package}.domain.teaching.service.impl.GradeDomainServiceImpl;
import ${package}.application.teaching.validators.TeachingApplicationValidator;
import ${package}.domain.user.service.PermissionDomainService;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.user.service.impl.PermissionDomainServiceImpl;
import ${package}.domain.user.service.impl.UserDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("domainServiceConfiguration")
public class DomainServiceConfiguration {

    @Bean("userDomainService")
    UserDomainService userDomainService() { return new UserDomainServiceImpl(); }

    @Bean("permissionDomainService")
    PermissionDomainService permissionDomainService() { return new PermissionDomainServiceImpl(); }

    @Bean
    UserApplicationValidator userApplicationValidator() { return new UserApplicationValidator(); }

    @Bean
    UserAssembler userAssembler() { return new UserAssembler(); }

    @Bean("schoolClassDomainService")
    SchoolClassDomainService schoolClassDomainService() { return new SchoolClassDomainService(); }

    @Bean("gradeDomainService")
    GradeDomainService gradeDomainService() { return new GradeDomainServiceImpl(); }

    @Bean
    TeachingApplicationValidator teachingApplicationValidator() { return new TeachingApplicationValidator(); }
}
