package top.egon.cola.component.accessguard.whitelist;

import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.accessguard.support.AccessGuardRedisKeys;

public class RedissonWhiteListRepository implements WhiteListRepository {

    private final RedissonClient redissonClient;

    private final AccessGuardRedisKeys redisKeys;

    public RedissonWhiteListRepository(RedissonClient redissonClient, AccessGuardRedisKeys redisKeys) {
        this.redissonClient = redissonClient;
        this.redisKeys = redisKeys;
    }

    @Override
    public boolean contains(String ruleName, String accessKeyHash) {
        RSet<String> whiteList = redissonClient.getSet(redisKeys.whiteList(ruleName, "all"));
        return whiteList.contains(accessKeyHash);
    }
}
