package ${package}.infrastructure.teaching.client.impl;

import ${package}.domain.teaching.service.TeachingQueryService;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.ExternalCourse;
import ${package}.infrastructure.teaching.validators.TeachingInfrastructureValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component("teachingQueryService")
@ConditionalOnProperty(name = "app.integrations.external-http.enabled", havingValue = "true")
public class RestTeachingQueryService implements TeachingQueryService {
    private final RestClient restClient;
    private final TeachingInfrastructureValidator validator;

    public RestTeachingQueryService(
            @Qualifier("teachingRestClient") RestClient restClient,
            TeachingInfrastructureValidator validator) {
        this.restClient = restClient;
        this.validator = validator;
    }

    @Override
    public Optional<ExternalCourse> findExternalCourse(CourseCode code) {
        try {
            CourseResponse response = restClient.get()
                    .uri("/courses/{courseCode}", code.value())
                    .retrieve()
                    .body(CourseResponse.class);
            ExternalCourse course = response == null
                    ? null
                    : new ExternalCourse(new CourseCode(response.code()), response.name());
            validator.validateExternalCourse(course, code);
            return Optional.of(course);
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        }
    }

    private record CourseResponse(String code, String name) {
    }
}
