local current = redis.call('GET', KEYS[1])
if not current then
    return {2, 0}
end

local instance = cjson.decode(current)
if instance['instanceId'] ~= ARGV[1]
        or instance['leaseId'] ~= ARGV[2]
        or instance['serviceKeyCanonical'] ~= ARGV[3] then
    return {3, 0}
end

local leaseExpireAt = tonumber(ARGV[4]) + tonumber(instance['leaseSeconds']) * 1000
instance['lastHeartbeatAtEpochMillis'] = tonumber(ARGV[4])
instance['leaseExpireAtEpochMillis'] = leaseExpireAt
redis.call('SET', KEYS[1], cjson.encode(instance), 'EX', instance['leaseSeconds'])
redis.call('ZADD', KEYS[2], leaseExpireAt, ARGV[1])
return {1, leaseExpireAt}
