package top.egon.cola.platform.idp.admin.resource.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.platform.idp.admin.resource.service.impl.ResourceServerAdmissionServiceImpl;
import top.egon.cola.platform.idp.core.oauth.OAuthException;
import top.egon.cola.platform.idp.core.resource.AdmissionRequest;
import top.egon.cola.platform.idp.core.resource.ResourceAuthorizationException;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Resource Server 使用端点绑定 {@code private_key_jwt} 取得短期准入票据的协议接口。
 *
 * <p>Protocol endpoint through which a Resource Server obtains a short-lived admission ticket
 * with endpoint-bound {@code private_key_jwt} authentication.</p>
 */
@RestController
@RequestMapping("/oauth2/resource-server-admission")
public class ResourceServerAdmissionController {

    /** 允许的表单参数，拒绝未知或重复参数；allowed form fields, rejecting unknown or repeated values. */
    private static final Set<String> FIELDS = Set.of(
            "client_id",
            "client_assertion_type",
            "client_assertion",
            "resource_server_id",
            "resource",
            "biz",
            "app",
            "env",
            "instance_id"
    );

    /** Resource Server 准入签发服务；Resource Server admission issuance service. */
    private final ResourceServerAdmissionServiceImpl admissions;

    /**
     * 创建 Resource Server 准入协议接口。
     *
     * <p>Creates the Resource Server admission protocol endpoint.</p>
     *
     * @param admissions 准入签发服务；admission issuance service
     */
    public ResourceServerAdmissionController(
            ResourceServerAdmissionServiceImpl admissions
    ) {
        this.admissions = Objects.requireNonNull(admissions, "admissions");
    }

    /**
     * 验证单值表单和 Client Assertion，返回 JWT 与本地续签时间。
     *
     * <p>Validates the single-valued form and Client Assertion, returning the JWT and local
     * renewal time.</p>
     *
     * @param form {@code application/x-www-form-urlencoded} 准入表单；admission form
     * @return 最小 Admission Ticket 结果；minimal Admission Ticket result
     */
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResourceServerAdmissionServiceImpl.IssuedAdmissionTicket admit(
            @RequestParam MultiValueMap<String, String> form
    ) {
        if (!FIELDS.equals(form.keySet())) {
            throw new IllegalArgumentException("admission form is invalid");
        }
        AdmissionRequest request = new AdmissionRequest(
                one(form, "resource_server_id"),
                URI.create(one(form, "resource")),
                one(form, "biz"),
                one(form, "app"),
                one(form, "env"),
                one(form, "instance_id")
        );
        return admissions.issue(
                one(form, "client_assertion_type"),
                one(form, "client_id"),
                one(form, "client_assertion"),
                request
        );
    }

    /**
     * 将 Client 认证失败映射为不泄露细节的 401 响应。
     *
     * <p>Maps Client authentication failures to a detail-free 401 response.</p>
     *
     * @param exception OAuth Client 认证异常；OAuth Client authentication exception
     * @return 安全错误响应；safe error response
     */
    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<AdmissionError> invalidClient(
            OAuthException exception
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new AdmissionError("invalid_client", "request is invalid")
        );
    }

    /**
     * 将 Resource 准入策略拒绝映射为稳定且安全的 403 响应。
     *
     * <p>Maps Resource admission-policy denials to a stable and safe 403 response.</p>
     *
     * @param exception Resource 准入异常；Resource admission exception
     * @return 安全错误响应；safe error response
     */
    @ExceptionHandler(ResourceAuthorizationException.class)
    public ResponseEntity<AdmissionError> denied(
            ResourceAuthorizationException exception
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new AdmissionError(exception.code(), "request is not authorized")
        );
    }

    /**
     * 读取一个且仅一个非空表单值。
     *
     * <p>Reads exactly one non-blank form value.</p>
     *
     * @param form 请求表单；request form
     * @param name 参数名；parameter name
     * @return 精确表单值；exact form value
     */
    private static String one(
            MultiValueMap<String, String> form,
            String name
    ) {
        List<String> values = form.get(name);
        if (values == null
                || values.size() != 1
                || values.getFirst() == null
                || values.getFirst().isBlank()
                || !values.getFirst().equals(values.getFirst().trim())) {
            throw new IllegalArgumentException(name + " is invalid");
        }
        return values.getFirst();
    }

    /**
     * Admission Endpoint 对外稳定错误体。
     *
     * <p>Stable public error body of the Admission Endpoint.</p>
     *
     * @param code 稳定错误码；stable error code
     * @param message 不包含敏感信息的错误描述；non-sensitive error description
     */
    public record AdmissionError(String code, String message) {

        /**
         * 校验安全错误体。
         *
         * <p>Validates the safe error body.</p>
         */
        public AdmissionError {
            code = required(code, "code");
            message = required(message, "message");
        }

        /**
         * 校验错误体必填文本。
         *
         * <p>Validates required error-body text.</p>
         *
         * @param value 待校验值；value to validate
         * @param field 字段名；field name
         * @return 已校验文本；validated text
         */
        private static String required(String value, String field) {
            if (value == null
                    || value.isBlank()
                    || !value.equals(value.trim())) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value;
        }
    }
}
