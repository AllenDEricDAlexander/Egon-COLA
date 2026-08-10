package top.egon.cola.platform.idp.admin.identity.service;

import top.egon.cola.platform.idp.admin.identity.domain.dto.CreateIdentityUserDTO;
import top.egon.cola.platform.idp.admin.identity.domain.dto.UpdateIdentityUserDTO;
import top.egon.cola.platform.idp.admin.identity.domain.vo.CreatedIdentityUserVO;
import top.egon.cola.platform.idp.admin.identity.domain.vo.IdentityUserVO;
import top.egon.cola.platform.idp.admin.identity.domain.vo.ResetPasswordVO;

import java.util.List;

/**
 * 统一身份用户的管理用例入口。
 *
 * <p>Application entry point for identity-user administration use cases.</p>
 */
public interface IdentityUserService {

    List<IdentityUserVO> list();

    CreatedIdentityUserVO create(CreateIdentityUserDTO command);

    IdentityUserVO update(String identitySub, UpdateIdentityUserDTO command);

    ResetPasswordVO resetPassword(String identitySub);

    IdentityUserVO revokeAll(String identitySub);
}
