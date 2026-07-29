package top.egon.cola.component.accessguard.blacklist;

public record BlacklistStatus(boolean blacklisted, long rejectCount, long expiresAtMillis, String reason) {

    public static BlacklistStatus none() {
        return new BlacklistStatus(false, 0L, 0L, "not blacklisted");
    }

    public static BlacklistStatus rejected(long rejectCount) {
        return new BlacklistStatus(false, rejectCount, 0L, "reject count incremented");
    }

    public static BlacklistStatus hit(long rejectCount, long expiresAtMillis) {
        return new BlacklistStatus(true, rejectCount, expiresAtMillis, "blacklisted");
    }

    public static BlacklistStatus rejected(String reason) {
        return new BlacklistStatus(false, 0L, 0L, reason);
    }
}
