package top.egon.cola.component.accessguard.key;

public interface KeyHasher {

    String hash(String normalizedKey, String secret);
}
