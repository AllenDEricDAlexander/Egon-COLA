package top.egon.cola.platform.idp.admin.token.service;

import top.egon.cola.platform.idp.admin.token.domain.dto.PublishSigningKeyDTO;
import top.egon.cola.platform.idp.admin.token.domain.vo.SigningKeyVO;

import java.util.List;

/**
 * 身份令牌签名密钥的管理用例入口。
 *
 * <p>Application entry point for identity-token signing-key administration.</p>
 */
public interface SigningKeyService {

    List<SigningKeyVO> list();

    SigningKeyVO publish(PublishSigningKeyDTO command);

    SigningKeyVO activate(String kid, long expectedVersion);

    SigningKeyVO retire(String kid, long expectedVersion);
}
