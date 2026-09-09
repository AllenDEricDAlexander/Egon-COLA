local action = ARGV[1]
local separator = string.char(31)

if action == 'CREATE' then
    if redis.call('EXISTS', KEYS[1]) == 1 then
        return 'EXISTS'
    end
    redis.call('HSET', KEYS[1],
            'identitySub', ARGV[2],
            'tenantId', ARGV[3],
            'issuedAt', ARGV[4],
            'expiresAt', ARGV[5],
            'status', 'ACTIVE')
    redis.call('PEXPIREAT', KEYS[1], ARGV[5])
    redis.call('SADD', KEYS[2], KEYS[1])
    local indexTtl = redis.call('PTTL', KEYS[2])
    local requestedTtl = tonumber(ARGV[5]) - tonumber(ARGV[4])
    if indexTtl < requestedTtl then
        redis.call('PEXPIREAT', KEYS[2], ARGV[5])
    end
    return 'CREATED'
end

if action == 'FIND' then
    if redis.call('EXISTS', KEYS[1]) == 0 then
        return ''
    end
    local status = redis.call('HGET', KEYS[1], 'status')
    local expiresAt = tonumber(redis.call('HGET', KEYS[1], 'expiresAt'))
    if status ~= 'ACTIVE' or expiresAt == nil or expiresAt <= tonumber(ARGV[2]) then
        if status == 'ACTIVE' then
            redis.call('HSET', KEYS[1], 'status', 'EXPIRED')
        end
        return ''
    end
    return table.concat({
            redis.call('HGET', KEYS[1], 'identitySub'),
            redis.call('HGET', KEYS[1], 'tenantId'),
            redis.call('HGET', KEYS[1], 'issuedAt'),
            redis.call('HGET', KEYS[1], 'expiresAt'),
            status}, separator)
end

if action == 'REVOKE_TOKEN' then
    if redis.call('EXISTS', KEYS[1]) == 1 then
        redis.call('HSET', KEYS[1],
                'status', 'REVOKED',
                'revocationReason', ARGV[2],
                'updatedAt', ARGV[3])
        return 'REVOKED'
    end
    return 'MISSING'
end

if action == 'REVOKE_SUBJECT' then
    local tokenKeys = redis.call('SMEMBERS', KEYS[1])
    for _, tokenKey in ipairs(tokenKeys) do
        if redis.call('EXISTS', tokenKey) == 1 then
            redis.call('HSET', tokenKey,
                    'status', 'REVOKED',
                    'revocationReason', ARGV[2],
                    'updatedAt', ARGV[3])
        end
        redis.call('SREM', KEYS[1], tokenKey)
    end
    redis.call('DEL', KEYS[1])
    return 'REVOKED'
end

if action == 'EXPIRE' then
    if redis.call('EXISTS', KEYS[1]) == 1
            and tonumber(redis.call('HGET', KEYS[1], 'expiresAt')) <= tonumber(ARGV[2]) then
        redis.call('HSET', KEYS[1], 'status', 'EXPIRED')
        return 'EXPIRED'
    end
    return 'NOOP'
end

return 'MISSING'
