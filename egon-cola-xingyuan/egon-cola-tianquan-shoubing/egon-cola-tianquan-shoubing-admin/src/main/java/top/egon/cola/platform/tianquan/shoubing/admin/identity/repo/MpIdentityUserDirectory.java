package top.egon.cola.platform.tianquan.shoubing.admin.identity.repo;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repository.IdentityUserRepository;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUser;

import java.util.List;

@Repository("mpIdentityUserDirectory")
@RequiredArgsConstructor
public class MpIdentityUserDirectory implements IdentityUserDirectory {

    @Qualifier("identityUserRepository")
    private final IdentityUserRepository users;

    @Override
    public List<IdentityUser> list() {
        return users.listOrderedByNormalizedUsernameAndSubject();
    }
}
