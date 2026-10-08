package top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthSigningKeyPO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OAuthSigningKeyConverter extends BaseConverter<OAuthSigningKeyPO, OAuthSigningKeyBO> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "kid", source = "kid")
    @Mapping(target = "algorithm", source = "algorithm")
    @Mapping(target = "publicJwk", source = "publicJwk")
    @Mapping(target = "privateKeyCiphertext", source = "privateKeyCiphertext")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "publishedAt", source = "publishedAt")
    @Mapping(target = "activatedAt", source = "activatedAt")
    @Mapping(target = "lastIssuedAt", source = "lastIssuedAt")
    @Mapping(target = "verifyUntil", source = "verifyUntil")
    @Mapping(target = "retiredAt", source = "retiredAt")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    OAuthSigningKeyBO toTarget(OAuthSigningKeyPO source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "kid", source = "kid")
    @Mapping(target = "algorithm", source = "algorithm")
    @Mapping(target = "publicJwk", source = "publicJwk")
    @Mapping(target = "privateKeyCiphertext", source = "privateKeyCiphertext")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "publishedAt", source = "publishedAt")
    @Mapping(target = "activatedAt", source = "activatedAt")
    @Mapping(target = "lastIssuedAt", source = "lastIssuedAt")
    @Mapping(target = "verifyUntil", source = "verifyUntil")
    @Mapping(target = "retiredAt", source = "retiredAt")
    OAuthSigningKeyPO toSource(OAuthSigningKeyBO target);
}
