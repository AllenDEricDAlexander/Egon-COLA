package ${package}.adapter.convertor;

import ${package}.application.manage.user.UserView;
import ${package}.facade.dto.user.UserDTO;

public final class UserAdapterConverter {
    private UserAdapterConverter() {
    }

    public static UserDTO toDto(UserView userView) {
        return new UserDTO(
                userView.id(),
                userView.name(),
                userView.email(),
                userView.status(),
                userView.schoolClassIds());
    }
}
