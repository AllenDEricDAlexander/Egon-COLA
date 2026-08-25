package ${package}.application.config;

import ${package}.application.user.assemblers.UserAssembler;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.application.teaching.validators.TeachingApplicationValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Application-layer collaborators that do not own persistence implementations. */
@Configuration("applicationConfiguration")
public class ApplicationConfiguration {

    @Bean
    UserApplicationValidator userApplicationValidator() {
        return new UserApplicationValidator();
    }

    @Bean
    TeachingApplicationValidator teachingApplicationValidator() {
        return new TeachingApplicationValidator();
    }

    @Bean
    UserAssembler userAssembler() {
        return new UserAssembler();
    }
}
