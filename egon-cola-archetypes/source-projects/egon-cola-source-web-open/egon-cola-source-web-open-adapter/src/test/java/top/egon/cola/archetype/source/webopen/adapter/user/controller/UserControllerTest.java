package top.egon.cola.archetype.source.webopen.adapter.user.controller;

import top.egon.cola.archetype.source.webopen.adapter.user.controller.UserController;
import top.egon.cola.archetype.source.webopen.adapter.user.converter.UserAdapterConverter;
import top.egon.cola.archetype.source.webopen.application.user.manage.UserManage;
import top.egon.cola.archetype.source.webopen.application.user.result.UserDetailResult;
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
import top.egon.cola.component.common.id.generator.LongIdGenerator;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserManage userManage;

    @Test
    void createsUserWithDirectVoAndLocation() throws Exception {
        when(userManage.createUser(any())).thenReturn(
            new UserDetailResult(1001L, "Mario", "mario@example.com", "ACTIVE", List.of()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new UserController(userManage, Mappers.getMapper(UserAdapterConverter.class),
                (LongIdGenerator) () -> 9001L)).build();

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Mario\",\"email\":\"mario@example.com\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/users/1001"))
            .andExpect(jsonPath("$.email").value("mario@example.com"));
    }
}
