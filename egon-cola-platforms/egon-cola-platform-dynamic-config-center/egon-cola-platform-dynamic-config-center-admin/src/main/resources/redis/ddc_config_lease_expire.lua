local value = redis.call('GET', KEYS[1])
if value then
    local lease = cjson.decode(value)
    if lease.instanceId ~= ARGV[1] or lease.leaseId ~= ARGV[2] then
        return 0
    end
    if tonumber(lease.leaseExpireAt) > tonumber(ARGV[3]) then
        return 0
    end
    redis.call('DEL', KEYS[1])
end

redis.call('SREM', KEYS[2], ARGV[1])
return 1
