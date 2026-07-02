package top.egon.fable-web.adapter.convertor;

import top.egon.fable-web.application.manage.user.UserView;
import top.egon.fable-web.facade.dto.user.UserDTO;

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
