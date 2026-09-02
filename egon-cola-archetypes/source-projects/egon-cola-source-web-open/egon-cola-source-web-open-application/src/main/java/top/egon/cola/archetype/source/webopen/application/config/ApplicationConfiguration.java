package top.egon.cola.archetype.source.webopen.application.config;

import top.egon.cola.archetype.source.webopen.application.user.assemblers.UserAssembler;
import top.egon.cola.archetype.source.webopen.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.webopen.application.teaching.validators.TeachingApplicationValidator;
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
