local current = redis.call('GET', KEYS[1])
if current then
    local currentValue = cjson.decode(current)
    if currentValue['serviceKeyCanonical'] ~= ARGV[3] then
        return {2, 0, 0}
    end
end

local catalogAdded = redis.call('SADD', KEYS[4], ARGV[3])
local serviceRevision = redis.call('INCR', KEYS[3])
local catalogRevision = tonumber(redis.call('GET', KEYS[5]) or '0')
if catalogAdded == 1 then
    catalogRevision = redis.call('INCR', KEYS[5])
end

local instance = cjson.decode(ARGV[4])
instance['revision'] = serviceRevision
redis.call('SET', KEYS[1], cjson.encode(instance), 'EX', ARGV[5])
redis.call('ZADD', KEYS[2], ARGV[6], ARGV[1])

local event = {
    serviceKey = cjson.decode(ARGV[7]),
    serviceRevision = serviceRevision,
    catalogRevision = catalogRevision
}
redis.call('PUBLISH', KEYS[6], cjson.encode(event))
return {1, serviceRevision, catalogRevision}
