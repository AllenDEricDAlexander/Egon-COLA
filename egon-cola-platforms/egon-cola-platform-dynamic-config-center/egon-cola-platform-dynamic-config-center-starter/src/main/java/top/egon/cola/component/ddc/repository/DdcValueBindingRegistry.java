package top.egon.cola.component.ddc.repository;

import top.egon.cola.component.ddc.model.vo.DdcFieldBinding;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 在线程安全的进程内注册表中保存可刷新的 {@code @DdcValue} 字段。
 * Stores refreshable {@code @DdcValue} fields in a thread-safe in-process registry.
 */
public class DdcValueBindingRegistry {

    /**
     * 保存当前活动字段绑定。 Stores the currently active field bindings.
     */
    private final CopyOnWriteArrayList<DdcFieldBinding> bindings =
            new CopyOnWriteArrayList<>();

    /**
     * 登记一个字段绑定。 Registers a field binding.
     *
     * @param binding 待登记绑定; binding to register
     */
    public void register(DdcFieldBinding binding) {
        bindings.add(binding);
    }

    /**
     * 移除属于指定 Bean 实例的全部绑定。 Removes all bindings owned by the specified bean instance.
     *
     * @param bean 待解绑 Bean; bean to unregister
     */
    public void unregister(Object bean) {
        bindings.removeIf(binding -> binding.getBean() == bean);
    }

    /**
     * 返回当前字段绑定的不可变快照。 Returns an immutable snapshot of current field bindings.
     *
     * @return 字段绑定快照; field-binding snapshot
     */
    public List<DdcFieldBinding> bindings() {
        return List.copyOf(bindings);
    }
}
