package top.egon.cola.archetype.source.web.adapter.user.pojo.dto;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record CreateUserMessage(String requestId, String name, String email) implements BasePojo {
}
