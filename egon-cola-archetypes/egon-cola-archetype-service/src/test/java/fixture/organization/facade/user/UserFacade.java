package fixture.organization.facade.user;

import fixture.organization.facade.dto.user.UserDetailDTO;

public interface UserFacade {

    UserDetailDTO getUser(String userId);
}
