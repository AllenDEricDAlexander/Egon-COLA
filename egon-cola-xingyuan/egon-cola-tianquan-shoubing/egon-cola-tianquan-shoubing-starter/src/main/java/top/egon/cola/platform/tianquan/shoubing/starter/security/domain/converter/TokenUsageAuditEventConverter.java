package top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.platform.tianquan.shoubing.core.audit.TokenUsageAuditEvent;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditContextBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;

/** Explicit one-way projection; raw JWT claims never enter the transport Event. */
@Mapper(componentModel = "spring")
public interface TokenUsageAuditEventConverter extends BaseForwardConverter<TokenAuditContextBO, TokenUsageAuditEvent> {
    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "schemaVersion", constant = "1")
    @Mapping(target = "issuer", expression = "java(source.getProof() == null ? source.getFallbackIssuer() : source.getProof().issuer())")
    @Mapping(target = "resource", expression = "java(source.getResource().toASCIIString())")
    @Mapping(target = "subjectRef", source = "proof.subjectRef")
    @Mapping(target = "clientRef", source = "proof.clientRef")
    @Mapping(target = "tokenRef", source = "proof.tokenRef")
    @Mapping(target = "result", source = "result")
    @Mapping(target = "reasonCode", source = "reasonCode")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "traceId", source = "traceId")
    TokenUsageAuditEvent toTarget(TokenAuditContextBO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "issuer", source = "trustedIssuer.issuer")
    @Mapping(target = "resource", expression = "java(resource)")
    @Mapping(target = "subjectRef", source = "subject", qualifiedByName = "auditHash")
    @Mapping(target = "clientRef", expression = "java(hash(verified.getJwt().getClaimAsString(\"client_id\")))")
    @Mapping(target = "tokenRef", expression = "java(references.reference(verified.getTrustedIssuer().issuer(), verified.getTokenId()))")
    TokenAuditProofBO toProof(VerifiedJwtBO verified, @Context URI resource, @Context TokenUsageAuditComponent references);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "issuer", source = "expectedIssuer")
    @Mapping(target = "resource", expression = "java(resource)")
    @Mapping(target = "subjectRef", expression = "java(hash(verifiedJwt.getSubject()))")
    @Mapping(target = "clientRef", expression = "java(hash(verifiedJwt.getClaimAsString(\"client_id\")))")
    @Mapping(target = "tokenRef", expression = "java(references.reference(expectedIssuer, verifiedJwt.getClaimAsString(\"jti\")))")
    TokenAuditProofBO toProof(org.springframework.security.oauth2.jwt.Jwt verifiedJwt, URI expectedIssuer,
            @Context URI resource, @Context TokenUsageAuditComponent references);

    @Named("auditHash")
    default String hash(String value) {
        if (value == null) { return null; }
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException("OAUTH_AUDIT_HASH_UNAVAILABLE", failure); }
    }
}
