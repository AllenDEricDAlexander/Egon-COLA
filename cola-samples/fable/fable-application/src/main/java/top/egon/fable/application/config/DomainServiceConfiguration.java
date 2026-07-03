package top.egon.fable.application.config;

import top.egon.fable.domain.repos.course.CourseRepository;
import top.egon.fable.domain.service.course.CourseDomainService;
import top.egon.fable.domain.service.examing.ExamDomainService;
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
