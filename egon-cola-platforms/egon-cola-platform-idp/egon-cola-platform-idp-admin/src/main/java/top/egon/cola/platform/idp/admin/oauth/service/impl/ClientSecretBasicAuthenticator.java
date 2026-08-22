package top.egon.cola.platform.idp.admin.oauth.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientSecretEntity;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientSecretRepository;
import top.egon.cola.platform.idp.core.oauth.ClientSecretAuthentication;
import top.egon.cola.platform.idp.core.oauth.OAuthException;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

/**
 * Authenticates OAuth Clients exclusively through one RFC 6749 Basic header.
 *
 * <p>所有失败都使用相同的 {@code invalid_client}，并在无法找到 Client/Secret 时执行
 * dummy hash，避免凭证存在性通过时序被区分。</p>
 */
@Service
public class ClientSecretBasicAuthenticator {

    private static final int MAX_HEADER_LENGTH = 4096;
    private static final int MAX_CREDENTIAL_LENGTH = 512;

    private final IdentityClientRepository clients;
    private final IdentityClientSecretRepository secrets;
    private final PasswordHashPort passwordHashes;

    public ClientSecretBasicAuthenticator(
            IdentityClientRepository clients,
            IdentityClientSecretRepository secrets,
            PasswordHashPort passwordHashes
    ) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.secrets = Objects.requireNonNull(secrets, "secrets");
        this.passwordHashes = Objects.requireNonNull(
                passwordHashes,
                "passwordHashes"
        );
    }

    /**
     * 验证请求中的唯一 Basic 凭证。
     *
     * <p>Authenticates the single Basic credential in the request.</p>
     *
     * @param request 当前 HTTP 请求；current HTTP request
     * @return 可信 Client/credential identity；trusted Client/credential identity
     */
    public ClientSecretAuthentication authenticate(HttpServletRequest request) {
        Objects.requireNonNull(request, "request");
        byte[] decoded = null;
        char[] rawSecret = null;
        try {
            String encoded = singleAuthorizationHeader(request);
            decoded = decode(encoded);
            int separator = indexOf(decoded, (byte) ':');
            if (separator < 1 || separator == decoded.length - 1) {
                throw invalidClient();
            }
            String encodedClientId = utf8(decoded, 0, separator);
            String encodedSecret = utf8(
                    decoded,
                    separator + 1,
                    decoded.length - separator - 1
            );
            String clientId = formDecode(encodedClientId);
            String secret = formDecode(encodedSecret);
            if (clientId.length() > MAX_CREDENTIAL_LENGTH
                    || secret.length() > MAX_CREDENTIAL_LENGTH) {
                throw invalidClient();
            }
            rawSecret = secret.toCharArray();
            IdentityClientEntity client = clients.findById(clientId)
                    .orElse(null);
            IdentityClientSecretEntity credential = client == null
                    ? null
                    : secrets.findActiveByClientId(clientId).orElse(null);
            String encodedHash = credential == null
                    ? passwordHashes.dummyHash()
                    : credential.getSecretHash();
            boolean matches = passwordHashes.matches(rawSecret, encodedHash);
            if (!matches
                    || client == null
                    || client.getClientType()
                    != IdentityClientEntity.ClientType.CONFIDENTIAL
                    || client.getStatus() != IdentityClientEntity.Status.ACTIVE
                    || credential == null) {
                throw invalidClient();
            }
            return new ClientSecretAuthentication(
                    client.getClientId(),
                    credential.getId()
            );
        } catch (OAuthException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidClient();
        } finally {
            if (decoded != null) {
                java.util.Arrays.fill(decoded, (byte) 0);
            }
            if (rawSecret != null) {
                java.util.Arrays.fill(rawSecret, '\0');
            }
        }
    }

    private static String singleAuthorizationHeader(HttpServletRequest request) {
        Enumeration<String> values = request.getHeaders("Authorization");
        List<String> headers = values == null
                ? List.of()
                : java.util.Collections.list(values);
        if (headers.size() != 1) {
            throw invalidClient();
        }
        String header = headers.getFirst();
        if (header == null || header.length() > MAX_HEADER_LENGTH
                || !header.regionMatches(true, 0, "Basic", 0, 5)
                || header.length() <= 6
                || header.charAt(5) != ' ') {
            throw invalidClient();
        }
        String encoded = header.substring(6).trim();
        if (encoded.isEmpty()) {
            throw invalidClient();
        }
        return encoded;
    }

    private static byte[] decode(String encoded) {
        try {
            byte[] value = Base64.getDecoder().decode(encoded);
            if (value.length == 0 || value.length > MAX_HEADER_LENGTH) {
                throw invalidClient();
            }
            return value;
        } catch (IllegalArgumentException exception) {
            throw invalidClient();
        }
    }

    private static int indexOf(byte[] value, byte target) {
        for (int index = 0; index < value.length; index++) {
            if (value[index] == target) {
                return index;
            }
        }
        return -1;
    }

    private static String utf8(byte[] value, int offset, int length) {
        try {
            CharBuffer chars = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(value, offset, length));
            return chars.toString();
        } catch (CharacterCodingException exception) {
            throw invalidClient();
        }
    }

    private static String formDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw invalidClient();
        }
    }

    private static OAuthException invalidClient() {
        return new OAuthException("invalid_client", "invalid_client");
    }
}
