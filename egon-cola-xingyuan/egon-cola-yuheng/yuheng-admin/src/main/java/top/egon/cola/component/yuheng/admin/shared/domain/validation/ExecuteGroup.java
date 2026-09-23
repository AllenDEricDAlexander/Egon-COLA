package top.egon.cola.component.yuheng.admin.shared.domain.validation;

/**
 * 中文说明：{@code ExecuteGroup} 是校验分组标记接口，负责已存在对象的命令与内部命令执行时的校验边界，无字段与构造器。
 * English summary: {@code ExecuteGroup} is a validation group marker interface that owns the execution boundary of commands over existing objects and of internal commands, with no fields and no constructor.
 *
 * 用法 / Usage: 只作为 Jakarta Validation 分组在确切的执行边界使用；分组校验不继承 {@code Default}，同一字段上属于 Default 的约束必须与本分组显式并列声明；/ Use it only as a Bean Validation group at the exact execute boundary; group validation does not inherit {@code Default}, so default constraints must be listed beside this group on the field.
 */
public interface ExecuteGroup {
}
