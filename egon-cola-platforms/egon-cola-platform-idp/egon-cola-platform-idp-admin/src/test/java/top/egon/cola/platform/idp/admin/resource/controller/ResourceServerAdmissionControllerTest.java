package top.egon.cola.platform.idp.admin.resource.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.platform.idp.admin.resource.service.impl.ResourceServerAdmissionServiceImpl;
import top.egon.cola.platform.idp.core.oauth.OAuthException;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceServerAdmissionControllerTest {

    @Test
    void acceptsFormEncodedPrivateKeyJwtAndReturnsOnlyTicketSchedule()
            throws Exception {
        ResourceServerAdmissionServiceImpl service = mock(
                ResourceServerAdmissionServiceImpl.class);
        when(service.issue(
                eq("urn:ietf:params:oauth:client-assertion-type:jwt-bearer"),
                eq("idp-service"),
                eq("signed-assertion"),
                any()
        )).thenReturn(new ResourceServerAdmissionServiceImpl
                .IssuedAdmissionTicket(
                "signed-admission-ticket",
                Instant.parse("2026-08-10T08:05:00Z")
        ));
        MockMvc mvc = mvc(service);

        mvc.perform(post("/oauth2/resource-server-admission")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("client_id", "idp-service")
                        .param("client_assertion_type",
                                "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                        .param("client_assertion", "signed-assertion")
                        .param("resource_server_id", "rs-idp-prod")
                        .param("resource", "https://api.example/idp")
                        .param("biz", "platform")
                        .param("app", "idp")
                        .param("env", "prod")
                        .param("instance_id", "idp-10.0.0.8-8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket")
                        .value("signed-admission-ticket"))
                .andExpect(jsonPath("$.expiresAt")
                        .value("2026-08-10T08:05:00Z"))
                .andExpect(jsonPath("$.clientAssertion").doesNotExist());
    }

    @Test
    void mapsInvalidClientWithoutLeakingAssertionDetails() throws Exception {
        ResourceServerAdmissionServiceImpl service = mock(
                ResourceServerAdmissionServiceImpl.class);
        when(service.issue(any(), any(), any(), any()))
                .thenThrow(new OAuthException(
                        "invalid_client",
                        "signature failure for key material"
                ));
        MockMvc mvc = mvc(service);

        mvc.perform(post("/oauth2/resource-server-admission")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("client_id", "idp-service")
                        .param("client_assertion_type", "invalid")
                        .param("client_assertion", "secret-assertion")
                        .param("resource_server_id", "rs-idp-prod")
                        .param("resource", "https://api.example/idp")
                        .param("biz", "platform")
                        .param("app", "idp")
                        .param("env", "prod")
                        .param("instance_id", "idp-10.0.0.8-8080"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_client"))
                .andExpect(jsonPath("$.message").value("request is invalid"));
    }

    private static MockMvc mvc(ResourceServerAdmissionServiceImpl service) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return MockMvcBuilders.standaloneSetup(
                        new ResourceServerAdmissionController(service))
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }
}
