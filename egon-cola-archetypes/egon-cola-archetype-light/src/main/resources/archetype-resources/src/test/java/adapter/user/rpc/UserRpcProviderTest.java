package ${package}.adapter.user.rpc;

import ${package}.facade.user.UserFacade;
import ${package}.facade.user.dto.CreateUserDTO;
import ${package}.facade.user.dto.UserDetailDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRpcProviderTest {
    @Test
    void delegates_to_facade_contract() {
        UserFacade delegate = mock(UserFacade.class);
        CreateUserDTO request = new CreateUserDTO("ext-1", "Mario", "mario@example.com", "operator-1", "request-1");
        UserDetailDTO response = new UserDetailDTO("user-1", "Mario", "mario@example.com", "ACTIVE");
        when(delegate.createUser(request)).thenReturn(response);

        UserRpcProvider provider = new UserRpcProvider(delegate);
        assertThat(provider.createUser(request)).isSameAs(response);
        verify(delegate).createUser(request);
    }
}
