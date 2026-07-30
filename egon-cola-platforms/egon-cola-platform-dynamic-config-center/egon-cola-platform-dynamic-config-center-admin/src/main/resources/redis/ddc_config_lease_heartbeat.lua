local value = redis.call('GET', KEYS[1])
if not value then
    return {2, 0}
end

local lease = cjson.decode(value)
if lease.instanceId ~= ARGV[1] or lease.leaseId ~= ARGV[2] then
    return {3, 0}
end

local heartbeatAt = tonumber(ARGV[3])
local leaseExpireAt = heartbeatAt + lease.leaseSeconds * 1000
lease.lastHeartbeatAt = heartbeatAt
lease.leaseExpireAt = leaseExpireAt
lease.status = 'ONLINE'

redis.call('SET', KEYS[1], cjson.encode(lease), 'EX', lease.leaseSeconds)
redis.call('SADD', KEYS[2], lease.instanceId)
return {1, leaseExpireAt}
