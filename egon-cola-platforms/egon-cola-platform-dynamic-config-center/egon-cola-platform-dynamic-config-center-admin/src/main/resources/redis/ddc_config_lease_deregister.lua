local value = redis.call('GET', KEYS[1])
if not value then
    return 2
end

local lease = cjson.decode(value)
if lease.instanceId ~= ARGV[1] or lease.leaseId ~= ARGV[2] then
    return 3
end

redis.call('DEL', KEYS[1])
redis.call('SREM', KEYS[2], lease.instanceId)
return 1
