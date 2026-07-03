package top.egon.light.application.config;

import top.egon.light.domain.student.service.StudentDomainService;
import top.egon.light.domain.teaching.service.CourseDomainService;
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

    @Bean("courseDomainService")
    public CourseDomainService courseDomainService() {
        return new CourseDomainService();
    }
}
