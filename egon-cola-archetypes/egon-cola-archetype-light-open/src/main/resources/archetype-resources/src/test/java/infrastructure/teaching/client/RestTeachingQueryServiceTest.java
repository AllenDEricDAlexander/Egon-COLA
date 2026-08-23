package ${package}.infrastructure.teaching.client;

import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.ExternalCourse;
import ${package}.infrastructure.teaching.client.impl.RestTeachingQueryService;
import ${package}.infrastructure.teaching.validators.TeachingInfrastructureValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestTeachingQueryServiceTest {
    @Test
    void loads_and_validates_external_course() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://courses.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://courses.test/courses/COURSE-001"))
                .andRespond(withSuccess(
                        "{\"code\":\"COURSE-001\",\"name\":\"Mathematics\"}",
                        MediaType.APPLICATION_JSON));
        RestTeachingQueryService service = new RestTeachingQueryService(
                builder.build(), new TeachingInfrastructureValidator());

        ExternalCourse course = service.findExternalCourse(new CourseCode("COURSE-001")).orElseThrow();

        assertEquals("Mathematics", course.name());
        server.verify();
    }
}
