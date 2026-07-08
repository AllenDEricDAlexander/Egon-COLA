package top.egon.cola.component.accessguard.whitelist;

public interface WhiteListRepository {

    boolean contains(String ruleName, String accessKeyHash);
}
