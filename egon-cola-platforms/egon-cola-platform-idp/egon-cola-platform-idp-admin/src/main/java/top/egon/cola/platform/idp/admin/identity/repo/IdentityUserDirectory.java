package top.egon.cola.platform.idp.admin.identity.repo;

import top.egon.cola.platform.idp.core.identity.IdentityUser;

import java.util.List;

/**
 * 提供身份用户管理视角下的用户目录查询。
 *
 * <p>Provides identity-user directory queries for administration use cases.</p>
 */
@FunctionalInterface
public interface IdentityUserDirectory {

    List<IdentityUser> list();
}
