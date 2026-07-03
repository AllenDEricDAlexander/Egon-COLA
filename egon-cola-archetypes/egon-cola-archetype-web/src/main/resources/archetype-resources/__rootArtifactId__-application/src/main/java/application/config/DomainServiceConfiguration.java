package ${package}.application.config;

import ${package}.domain.service.teaching.SchoolClassDomainService;
import ${package}.domain.service.user.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("domainServiceConfiguration")
@RequiredArgsConstructor
public class DomainServiceConfiguration {

    @Bean("userDomainService")
    public UserDomainService userDomainService() {
        return new UserDomainService();
    }

    @Bean("schoolClassDomainService")
    public SchoolClassDomainService schoolClassDomainService() {
        return new SchoolClassDomainService();
    }
}
