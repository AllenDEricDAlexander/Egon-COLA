package top.egon.cola.component.accessguard.policy;

public interface GuardPolicy<C extends PolicyConfig> {

    String id();

    PolicyResult evaluate(GuardContext context, C config);
}
