if ARGV[1] == '' then
    if redis.call('ZCARD', KEYS[2]) ~= 0 then
        return {0, tonumber(redis.call('GET', KEYS[3]) or '0'), tonumber(redis.call('GET', KEYS[5]) or '0')}
    end
else
    local score = redis.call('ZSCORE', KEYS[2], ARGV[1])
    if not score or tonumber(score) > tonumber(ARGV[3]) then
        return {0, tonumber(redis.call('GET', KEYS[3]) or '0'), tonumber(redis.call('GET', KEYS[5]) or '0')}
    end

    local current = redis.call('GET', KEYS[1])
    if current then
        local instance = cjson.decode(current)
        if instance['serviceKeyCanonical'] == ARGV[2]
                and tonumber(instance['leaseExpireAtEpochMillis']) > tonumber(ARGV[3]) then
            redis.call('ZADD', KEYS[2], instance['leaseExpireAtEpochMillis'], ARGV[1])
            return {0, tonumber(redis.call('GET', KEYS[3]) or '0'), tonumber(redis.call('GET', KEYS[5]) or '0')}
        end
        if instance['serviceKeyCanonical'] == ARGV[2] then
            redis.call('DEL', KEYS[1])
        end
    end
    redis.call('ZREM', KEYS[2], ARGV[1])
end

local serviceRevision = tonumber(redis.call('GET', KEYS[3]) or '0')
local catalogRevision = tonumber(redis.call('GET', KEYS[5]) or '0')
local changed = 0
if ARGV[1] ~= '' then
    serviceRevision = redis.call('INCR', KEYS[3])
    changed = 1
end
if redis.call('ZCARD', KEYS[2]) == 0 and redis.call('SREM', KEYS[4], ARGV[2]) == 1 then
    catalogRevision = redis.call('INCR', KEYS[5])
    changed = 1
end
if changed == 1 then
    local event = {
        serviceKey = cjson.decode(ARGV[4]),
        serviceRevision = serviceRevision,
        catalogRevision = catalogRevision
    }
    redis.call('PUBLISH', KEYS[6], cjson.encode(event))
end
return {changed, serviceRevision, catalogRevision}
