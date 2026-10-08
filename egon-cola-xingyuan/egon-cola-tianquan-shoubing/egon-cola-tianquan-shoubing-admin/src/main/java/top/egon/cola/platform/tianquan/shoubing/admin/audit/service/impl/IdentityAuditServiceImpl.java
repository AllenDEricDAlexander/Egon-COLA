package top.egon.cola.platform.tianquan.shoubing.admin.audit.service.impl;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.converter.IdentityAuditViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.dto.IdentityAuditQueryDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.vo.IdentityAuditPageVO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.service.IdentityAuditService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;

@Service("identityAuditServiceImpl")
@RequiredArgsConstructor
@Slf4j
public class IdentityAuditServiceImpl implements IdentityAuditService {

    private static final int MAXIMUM_PAGE_SIZE = 200;

    @Qualifier("oAuthAuditRepository")
    private final OAuthAuditRepository audits;
    @Qualifier("identityAuditViewConverterImpl")
    private final IdentityAuditViewConverter views;
    @Qualifier("oauthManagementRealmResolver")
    private final OAuthManagementRealmResolver realmResolver;

    @Override
    @Transactional(readOnly = true)
    public IdentityAuditPageVO list(IdentityAuditQueryDTO query) {
        IdentityAuditQueryDTO input = Objects.requireNonNull(query, "query");
        if (input.page() < 0 || input.size() < 1 || input.size() > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException("invalid audit page request");
        }
        TrustedRealmBO platformRealm = realmResolver.platformRealm();
        long total = audits.countIdentityPage(platformRealm, input);
        var rows = audits.findIdentityPage(platformRealm, input);
        int totalPages = total == 0L ? 0 : Math.toIntExact((total + input.size() - 1L) / input.size());
        return new IdentityAuditPageVO(
                views.toTargetList(rows), input.page(), input.size(), total, totalPages);
    }
}
