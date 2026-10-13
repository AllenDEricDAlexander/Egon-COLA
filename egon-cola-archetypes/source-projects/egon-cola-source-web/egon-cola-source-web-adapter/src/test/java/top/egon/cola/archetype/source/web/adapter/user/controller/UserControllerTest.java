package top.egon.cola.archetype.source.web.adapter.user.controller;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.archetype.source.web.adapter.graphql.NativeGraphQlSecurityContractTest.SecurityFixture;
import top.egon.cola.archetype.source.web.adapter.user.controller.UserController;
import top.egon.cola.archetype.source.web.adapter.user.pojo.convertor.UserAdapterConverter;
import top.egon.cola.archetype.source.web.application.user.manage.UserManage;
import top.egon.cola.archetype.source.web.application.user.pojo.result.UserDetailResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @TempDir Path temporary;
    private SecurityFixture security;

    @BeforeEach
    void bindVerifiedUser() throws Exception {
        security = new SecurityFixture(temporary);
        SecurityContextHolder.getContext().setAuthentication(security.authenticatedUser("1001", Set.copyOf(security.permissions)));
    }

    @AfterEach
    void closeVerifiedUser() { security.close(); }


    @Mock
    private UserManage userManage;

    @Test
    void createsUserWithDirectVoAndLocation() throws Exception {
        when(userManage.createUser(any())).thenReturn(
            new UserDetailResult(1001L, "Mario", "mario@example.com", "ACTIVE", List.of()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new UserController(security.fields, security.properties, userManage, Mappers.getMapper(UserAdapterConverter.class))).build();

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Mario\",\"email\":\"mario@example.com\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/users/1001"))
            .andExpect(jsonPath("$.email").value("mario@example.com"));
    }
}
