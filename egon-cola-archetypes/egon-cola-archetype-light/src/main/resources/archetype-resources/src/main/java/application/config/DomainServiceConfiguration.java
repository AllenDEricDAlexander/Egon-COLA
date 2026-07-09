package ${package}.application.config;

import ${package}.domain.student.service.StudentDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("domainServiceConfiguration")
@RequiredArgsConstructor
public class DomainServiceConfiguration {

    @Bean("studentDomainService")
    public StudentDomainService studentDomainService() {
        return new StudentDomainService();
    }
}
