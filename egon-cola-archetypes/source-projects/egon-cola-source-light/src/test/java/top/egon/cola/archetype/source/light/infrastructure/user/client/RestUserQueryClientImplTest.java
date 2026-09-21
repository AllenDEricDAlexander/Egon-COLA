package top.egon.cola.archetype.source.light.infrastructure.user.client;

import jakarta.validation.Validation;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.archetype.source.light.domain.user.vos.ExternalUser;
import top.egon.cola.archetype.source.light.infrastructure.user.client.impl.RestUserQueryClientImpl;
import top.egon.cola.archetype.source.light.infrastructure.user.validators.UserInfrastructureValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestUserQueryClientImplTest {
    @Test
    void loads_and_validates_external_user() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://users.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users.test/users/ext-1"))
                .andRespond(withSuccess(
                        "{\"externalId\":\"ext-1\",\"name\":\"Mario\"}",
                        MediaType.APPLICATION_JSON));
        RestUserQueryClientImpl service = new RestUserQueryClientImpl(
                builder.build(), new UserInfrastructureValidator(new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator())));

        ExternalUser user = service.findExternalUser("ext-1").orElseThrow();

        assertEquals("Mario", user.name());
        server.verify();
    }
}
