package top.egon.cola.component.gateway.admin.infrastructure.security;

import top.egon.cola.component.gateway.admin.application.credential.GatewaySecretProtector;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 中文说明：{@code AesGcmGatewaySecretProtector} 是类型，位于当前 Gateway 模块的相关包中，负责AesGcm网关SecretProtector相关的职责与边界。
 * English summary: {@code AesGcmGatewaySecretProtector} is a type in the current Gateway module; it owns the aes gcm gateway secret protector-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class AesGcmGatewaySecretProtector
        implements GatewaySecretProtector {

    /**
     * 中文说明：表示 IVBYTES 这一固定值；它属于 {@code AesGcmGatewaySecretProtector} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value iv bytes; it is a state, type, or protocol value of {@code AesGcmGatewaySecretProtector} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code AesGcmGatewaySecretProtector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AesGcmGatewaySecretProtector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final int IV_BYTES = 12;

    /**
     * 中文说明：表示 TAGBITS 这一固定值；它属于 {@code AesGcmGatewaySecretProtector} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value tag bits; it is a state, type, or protocol value of {@code AesGcmGatewaySecretProtector} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code AesGcmGatewaySecretProtector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AesGcmGatewaySecretProtector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final int TAG_BITS = 128;

    /**
     * 中文说明：保存 键 对应的状态、依赖或配置值；字段类型为 {@code SecretKeySpec}，由 {@code AesGcmGatewaySecretProtector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by key; its type is {@code SecretKeySpec}, and {@code AesGcmGatewaySecretProtector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code AesGcmGatewaySecretProtector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AesGcmGatewaySecretProtector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final SecretKeySpec key;

    /**
     * 中文说明：保存 键Version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code AesGcmGatewaySecretProtector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by key version; its type is {@code String}, and {@code AesGcmGatewaySecretProtector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code AesGcmGatewaySecretProtector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AesGcmGatewaySecretProtector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String keyVersion;

    /**
     * 中文说明：保存 random 对应的状态、依赖或配置值；字段类型为 {@code SecureRandom}，由 {@code AesGcmGatewaySecretProtector} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by random; its type is {@code SecureRandom}, and {@code AesGcmGatewaySecretProtector} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code AesGcmGatewaySecretProtector} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AesGcmGatewaySecretProtector}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final SecureRandom random;

    /**
     * 中文说明：创建 {@code AesGcmGatewaySecretProtector} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code AesGcmGatewaySecretProtector} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param key 参数 键；parameter key。
     * @param keyVersion 参数 键Version；parameter key version。
     */
    public AesGcmGatewaySecretProtector(
            byte[] key,
            String keyVersion) {
        this(key, keyVersion, new SecureRandom());
    }

    /**
     * 中文说明：创建 {@code AesGcmGatewaySecretProtector} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code AesGcmGatewaySecretProtector} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param key 参数 键；parameter key。
     * @param keyVersion 参数 键Version；parameter key version。
     * @param random 参数 random；parameter random。
     */
    AesGcmGatewaySecretProtector(
            byte[] key,
            String keyVersion,
            SecureRandom random) {
        if (key == null || key.length != 32) {
            throw new IllegalArgumentException(
                    "AES-256-GCM key must contain 32 bytes"
            );
        }
        if (keyVersion == null || keyVersion.isBlank()) {
            throw new IllegalArgumentException("keyVersion is required");
        }
        this.key = new SecretKeySpec(key.clone(), "AES");
        this.keyVersion = keyVersion.trim();
        this.random = random;
    }

    /**
     * 中文说明：执行 protect 操作；该方法是 {@code AesGcmGatewaySecretProtector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the protect operation; this method is the invocation entry point on {@code AesGcmGatewaySecretProtector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AesGcmGatewaySecretProtector.protect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param plaintext 参数 plaintext；parameter plaintext。
     * @param associatedData 参数 associatedData；parameter associated data。
     * @return 返回 protect 的处理结果；returns the result of the operation.
     */
    @Override
    public ProtectedSecret protect(
            String plaintext,
            String associatedData) {
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_BITS, iv)
            );
            cipher.updateAAD(aad(associatedData));
            byte[] encrypted = cipher.doFinal(
                    plaintext.getBytes(StandardCharsets.UTF_8)
            );
            return new ProtectedSecret(
                    Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(iv)
                            + "."
                            + Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(encrypted),
                    keyVersion
            );
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException(
                    "gateway secret encryption failed",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 unprotect 操作；该方法是 {@code AesGcmGatewaySecretProtector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the unprotect operation; this method is the invocation entry point on {@code AesGcmGatewaySecretProtector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AesGcmGatewaySecretProtector.unprotect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param secret 参数 secret；parameter secret。
     * @param associatedData 参数 associatedData；parameter associated data。
     * @return 返回 unprotect 的处理结果；returns the result of the operation.
     */
    @Override
    public String unprotect(
            ProtectedSecret secret,
            String associatedData) {
        if (!keyVersion.equals(secret.keyVersion())) {
            throw new IllegalArgumentException(
                    "gateway secret key version is not available"
            );
        }
        String[] parts = secret.ciphertext().split("\\.", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "gateway secret ciphertext is malformed"
            );
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(
                            TAG_BITS,
                            Base64.getUrlDecoder().decode(parts[0])
                    )
            );
            cipher.updateAAD(aad(associatedData));
            return new String(
                    cipher.doFinal(
                            Base64.getUrlDecoder().decode(parts[1])
                    ),
                    StandardCharsets.UTF_8
            );
        } catch (GeneralSecurityException | IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                    "gateway secret cannot be decrypted",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 aad 操作；该方法是 {@code AesGcmGatewaySecretProtector} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the aad operation; this method is the invocation entry point on {@code AesGcmGatewaySecretProtector} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AesGcmGatewaySecretProtector.aad(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param associatedData 参数 associatedData；parameter associated data。
     * @return 返回 aad 的处理结果；returns the result of the operation.
     */
    private byte[] aad(String associatedData) {
        if (associatedData == null || associatedData.isBlank()) {
            throw new IllegalArgumentException(
                    "associatedData is required"
            );
        }
        return associatedData.getBytes(StandardCharsets.UTF_8);
    }
}
