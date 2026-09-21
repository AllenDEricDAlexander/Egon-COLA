package top.egon.cola.archetype.source.webopen.application.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.archetype.source.webopen.application.teaching.validators.GradeApplicationValidator;
import top.egon.cola.archetype.source.webopen.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.webopen.application.user.validators.PermissionApplicationValidator;
import top.egon.cola.archetype.source.webopen.application.user.validators.UserApplicationValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Application-layer collaborators that do not own persistence implementations. */
@Configuration("applicationConfiguration")
public class ApplicationConfiguration {

    @Bean("userApplicationValidator")
    UserApplicationValidator userApplicationValidator(
            @Qualifier("egonColaValidationUtils") ValidationUtils validationUtils) {
        return new UserApplicationValidator(validationUtils);
    }

    @Bean("teachingApplicationValidator")
    TeachingApplicationValidator teachingApplicationValidator(
            @Qualifier("egonColaValidationUtils") ValidationUtils validationUtils) {
        return new TeachingApplicationValidator(validationUtils);
    }

    @Bean("gradeApplicationValidator")
    GradeApplicationValidator gradeApplicationValidator(
            @Qualifier("egonColaValidationUtils") ValidationUtils validationUtils) {
        return new GradeApplicationValidator(validationUtils);
    }

    @Bean("permissionApplicationValidator")
    PermissionApplicationValidator permissionApplicationValidator(
            @Qualifier("egonColaValidationUtils") ValidationUtils validationUtils) {
        return new PermissionApplicationValidator(validationUtils);
    }
}
