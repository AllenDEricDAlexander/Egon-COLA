package top.egon.cola.component.accessguard.blacklist;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;
import top.egon.cola.component.accessguard.support.AccessGuardRedisKeys;

public class RedissonBlacklistService implements BlacklistService {

    private static final String ALL_HASH = "all";

    private final RedissonClient redissonClient;

    private final AccessGuardRedisKeys redisKeys;

    public RedissonBlacklistService(RedissonClient redissonClient, AccessGuardRedisKeys redisKeys) {
        this.redissonClient = redissonClient;
        this.redisKeys = redisKeys;
    }

    @Override
    public BlacklistStatus status(AccessGuardRule rule, AccessGuardContext context) {
        if (!rule.blacklistEnabled()) {
            return BlacklistStatus.none();
        }
        RBucket<BlacklistStatus> bucket = redissonClient.getBucket(redisKeys.blacklist(rule.name(), context.accessKeyHash()));
        BlacklistStatus status = bucket.get();
        return status == null ? BlacklistStatus.none() : status;
    }

    @Override
    public BlacklistStatus incrementRejectAndMaybeBlacklist(AccessGuardRule rule, AccessGuardContext context) {
        if (!rule.blacklistEnabled() || rule.blacklistCount() <= 0) {
            return BlacklistStatus.none();
        }
        if (ALL_HASH.equals(context.accessKeyHash()) && !rule.enableBlacklistForAllKey()) {
            return BlacklistStatus.rejected("blacklist for all key disabled");
        }

        RAtomicLong rejectCounter = redissonClient.getAtomicLong(redisKeys.rejectCount(rule.name(), context.accessKeyHash()));
        long rejectCount = rejectCounter.incrementAndGet();
        rejectCounter.expire(rule.blacklistTimeout());
        if (rejectCount < rule.blacklistCount()) {
            return BlacklistStatus.rejected(rejectCount);
        }

        long expiresAtMillis = System.currentTimeMillis() + rule.blacklistTimeout().toMillis();
        BlacklistStatus status = BlacklistStatus.hit(rejectCount, expiresAtMillis);
        RBucket<BlacklistStatus> bucket = redissonClient.getBucket(redisKeys.blacklist(rule.name(), context.accessKeyHash()));
        bucket.set(status, rule.blacklistTimeout());
        return status;
    }

    @Override
    public void remove(String ruleName, String accessKeyHash) {
        redissonClient.getBucket(redisKeys.blacklist(ruleName, accessKeyHash)).delete();
        redissonClient.getAtomicLong(redisKeys.rejectCount(ruleName, accessKeyHash)).delete();
    }
}
