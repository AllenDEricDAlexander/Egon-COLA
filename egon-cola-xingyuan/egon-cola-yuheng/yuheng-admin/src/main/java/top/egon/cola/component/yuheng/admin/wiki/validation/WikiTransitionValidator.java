package top.egon.cola.component.yuheng.admin.wiki.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiTransitionCommandDTO;

import java.util.Objects;

/**
 * 中文说明：{@code WikiTransitionValidator} 是 {@code ValidWikiTransition} 的 ConstraintValidator 实现，负责校验迁移事件与两个 CAS version 字段必须同时给出。
 * English summary: {@code WikiTransitionValidator} is the ConstraintValidator of {@code ValidWikiTransition} and checks that the selected event and both compare-and-set versions are present together.
 *
 * 用法 / Usage: 由容器按约束注解调用，不查库、不发外部调用、不决定权限；哪条迁移合法、审核 adapter 是否注册、租户与 KB 成员资格以及 version 的 CAS 结果由所属 Service 在事务内判定；本命令没有 publicationStatus、reviewStatus、reviewer 或 decision 字段，调用方因此无法改写审查状态，状态与策略快照始终服务端派生。/ Invoked by the container through the constraint annotation without database, network or permission access; the legal migration, the registered review adapter, tenant and KB membership and the CAS outcome are decided by the owning Service inside its transaction; this carrier exposes no publicationStatus, reviewStatus, reviewer or decision field, so a caller cannot override review state and status stays server-derived.
 */
@Slf4j
@Component("wikiTransitionValidator")
@RequiredArgsConstructor
public class WikiTransitionValidator
        implements ConstraintValidator<ValidWikiTransition, WikiTransitionCommandDTO> {

    private static final String EVENT_PROPERTY = "event";

    private static final String PAGE_REVISION_PROPERTY = "expectedPageRevision";

    private static final String PUBLICATION_VERSION_PROPERTY =
            "expectedPublicationVersion";

    /**
     * Checks one transition command for the event and both CAS versions.
     *
     * @param command wiki transition command, may be absent
     * @param context constraint context used for property-node violations
     * @return {@code true} when every required carrier is present
     */
    @Override
    public boolean isValid(
            WikiTransitionCommandDTO command,
            ConstraintValidatorContext context) {
        if (command == null) {
            // Absent input belongs to the carrier's own presence constraints.
            return true;
        }
        boolean eventPresent = command.getEvent() != null;
        boolean pageRevisionPresent =
                Objects.nonNull(command.getExpectedPageRevision());
        boolean publicationVersionPresent =
                Objects.nonNull(command.getExpectedPublicationVersion());
        if (eventPresent && pageRevisionPresent && publicationVersionPresent) {
            return true;
        }
        log.debug(
                "Rejected incomplete wiki transition command pageId={}",
                command.getPageId()
        );
        context.disableDefaultConstraintViolation();
        if (!eventPresent) {
            rejectProperty(context, EVENT_PROPERTY);
        }
        if (!pageRevisionPresent) {
            rejectProperty(context, PAGE_REVISION_PROPERTY);
        }
        if (!publicationVersionPresent) {
            rejectProperty(context, PUBLICATION_VERSION_PROPERTY);
        }
        return false;
    }

    private static void rejectProperty(
            ConstraintValidatorContext context,
            String property) {
        context.buildConstraintViolationWithTemplate(
                        context.getDefaultConstraintMessageTemplate()
                )
                .addPropertyNode(property)
                .addConstraintViolation();
    }
}
