package ${package}.infrastructure.teaching.client.impl;

import ${package}.domain.teaching.gateway.TeachingQueryGateway;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.ExternalCourse;
import ${package}.infrastructure.teaching.validators.TeachingInfrastructureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component("teachingQueryGateway")
@ConditionalOnProperty(name = "app.integrations.external-http.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RestTeachingQueryService implements TeachingQueryGateway {
    @Qualifier("teachingRestClient")
    private final RestClient restClient;
    @Qualifier("teachingInfrastructureValidator")
    private final TeachingInfrastructureValidator validator;

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
