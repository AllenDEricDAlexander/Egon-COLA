package ${package}.application.config;

import ${package}.application.assemblers.user.UserAssembler;
import ${package}.application.validators.user.UserApplicationValidator;
import ${package}.domain.service.teaching.SchoolClassDomainService;
import ${package}.domain.service.user.UserDomainService;
import ${package}.domain.service.user.impl.UserDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("domainServiceConfiguration")
public class DomainServiceConfiguration {

    @Bean("userDomainService")
    UserDomainService userDomainService() { return new UserDomainServiceImpl(); }

    @Bean
    UserApplicationValidator userApplicationValidator() { return new UserApplicationValidator(); }

    @Bean
    UserAssembler userAssembler() { return new UserAssembler(); }

    @Bean("schoolClassDomainService")
    SchoolClassDomainService schoolClassDomainService() { return new SchoolClassDomainService(); }
}
