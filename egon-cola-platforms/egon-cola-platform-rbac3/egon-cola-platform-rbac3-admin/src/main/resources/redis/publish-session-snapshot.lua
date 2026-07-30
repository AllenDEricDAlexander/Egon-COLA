local requestedAuthVersion = tonumber(ARGV[2])
local requestedPolicyVersion = tonumber(ARGV[3])
local requestedSessionVersion = tonumber(ARGV[5])

local currentAuthVersion = redis.call('get', KEYS[2])
if currentAuthVersion and tonumber(currentAuthVersion) > requestedAuthVersion then
    return -1
end

local currentPolicyVersion = redis.call('get', KEYS[3])
if currentPolicyVersion and tonumber(currentPolicyVersion) > requestedPolicyVersion then
    return -1
end

local currentSessionJson = redis.call('get', KEYS[1])
if currentSessionJson then
    local currentSession = cjson.decode(currentSessionJson)
    if tonumber(currentSession.sessionVersion) > requestedSessionVersion then
        return -1
    end
    if tonumber(currentSession.sessionVersion) == requestedSessionVersion
            and redis.call('exists', KEYS[4]) == 1 then
        redis.call('del', KEYS[5])
        return 0
    end
end

redis.call('set', KEYS[1], ARGV[1])
redis.call('set', KEYS[2], ARGV[2])
redis.call('set', KEYS[3], ARGV[3])
redis.call('set', KEYS[4], ARGV[4])
redis.call('pexpireat', KEYS[1], ARGV[6])
redis.call('pexpireat', KEYS[4], ARGV[6])
redis.call('del', KEYS[5])
return 1
