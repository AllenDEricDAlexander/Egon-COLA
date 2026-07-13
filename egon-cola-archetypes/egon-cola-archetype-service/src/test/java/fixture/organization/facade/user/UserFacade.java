package fixture.organization.facade.user;

import fixture.organization.facade.user.dto.UserDetailDTO;

public interface UserFacade {

    UserDetailDTO getUser(String userId);
}
