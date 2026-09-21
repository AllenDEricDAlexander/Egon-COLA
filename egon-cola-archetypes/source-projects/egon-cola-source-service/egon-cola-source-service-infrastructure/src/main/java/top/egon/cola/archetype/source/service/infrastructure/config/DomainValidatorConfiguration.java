package top.egon.cola.archetype.source.service.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.archetype.source.service.domain.course.validators.CourseDomainValidator;
import top.egon.cola.archetype.source.service.domain.exam.validators.ExamDomainValidator;
import top.egon.cola.archetype.source.service.domain.exam.validators.ScoreDomainValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/**
 * Domain validators stay framework-free, so the infrastructure layer supplies their single
 * technical collaborator and publishes them under the names the service implementations qualify on.
 */
@Configuration(proxyBeanMethods = false)
public class DomainValidatorConfiguration {

    @Bean("courseDomainValidator")
    CourseDomainValidator courseDomainValidator(
            @Qualifier("egonColaValidationUtils") ValidationUtils validationUtils) {
        return new CourseDomainValidator(validationUtils);
    }

    @Bean("examDomainValidator")
    ExamDomainValidator examDomainValidator(
            @Qualifier("egonColaValidationUtils") ValidationUtils validationUtils) {
        return new ExamDomainValidator(validationUtils);
    }

    @Bean("scoreDomainValidator")
    ScoreDomainValidator scoreDomainValidator(
            @Qualifier("egonColaValidationUtils") ValidationUtils validationUtils) {
        return new ScoreDomainValidator(validationUtils);
    }
}
