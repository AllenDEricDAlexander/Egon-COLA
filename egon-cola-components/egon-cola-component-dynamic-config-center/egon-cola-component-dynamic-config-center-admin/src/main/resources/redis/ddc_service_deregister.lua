local current = redis.call('GET', KEYS[1])
if not current then
    return {2, 0, tonumber(redis.call('GET', KEYS[5]) or '0')}
end

local instance = cjson.decode(current)
if instance['instanceId'] ~= ARGV[1]
        or instance['leaseId'] ~= ARGV[2]
        or instance['serviceKeyCanonical'] ~= ARGV[3] then
    return {3, 0, tonumber(redis.call('GET', KEYS[5]) or '0')}
end

redis.call('DEL', KEYS[1])
redis.call('ZREM', KEYS[2], ARGV[1])
local serviceRevision = redis.call('INCR', KEYS[3])
local catalogRevision = tonumber(redis.call('GET', KEYS[5]) or '0')
if redis.call('ZCARD', KEYS[2]) == 0 and redis.call('SREM', KEYS[4], ARGV[3]) == 1 then
    catalogRevision = redis.call('INCR', KEYS[5])
end

local event = {
    serviceKey = cjson.decode(ARGV[4]),
    serviceRevision = serviceRevision,
    catalogRevision = catalogRevision
}
redis.call('PUBLISH', KEYS[6], cjson.encode(event))
redis.call('PUBLISH', ARGV[6], cjson.encode(event))
return {1, serviceRevision, catalogRevision}
