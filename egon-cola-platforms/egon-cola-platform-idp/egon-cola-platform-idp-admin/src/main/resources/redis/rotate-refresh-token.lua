local action = ARGV[1]

if action == 'CREATE' then
    if redis.call('EXISTS', KEYS[1]) == 1
            or redis.call('EXISTS', KEYS[2]) == 1 then
        return 'EXISTS'
    end
    redis.call('HSET', KEYS[1],
            'familyId', ARGV[2],
            'identitySub', ARGV[3],
            'tenantId', ARGV[4],
            'sessionId', ARGV[5],
            'clientId', ARGV[6],
            'tokenVersion', ARGV[7],
            'generation', ARGV[8],
            'currentDigest', ARGV[9],
            'status', ARGV[10],
            'createdAt', ARGV[11],
            'updatedAt', ARGV[12],
            'expiresAt', ARGV[13])
    redis.call('PEXPIREAT', KEYS[1], ARGV[13])
    redis.call('SET', KEYS[2], ARGV[2], 'PXAT', ARGV[13])
    redis.call('SADD', KEYS[3], KEYS[1])
    local indexTtl = redis.call('PTTL', KEYS[3])
    local requestedTtl = tonumber(ARGV[13]) - tonumber(ARGV[12])
    if indexTtl < requestedTtl then
        redis.call('PEXPIREAT', KEYS[3], ARGV[13])
    end
    return 'CREATED'
end

if action == 'ROTATE' then
    if redis.call('EXISTS', KEYS[1]) == 0 then
        return 'MISSING'
    end
    if redis.call('HGET', KEYS[1], 'status') ~= 'ACTIVE' then
        return 'REVOKED'
    end
    if redis.call('HGET', KEYS[1], 'identitySub') ~= ARGV[6]
            or redis.call('HGET', KEYS[1], 'tokenVersion') ~= ARGV[7]
            or redis.call('HGET', KEYS[1], 'expiresAt') ~= ARGV[8] then
        return 'REVOKED'
    end
    local currentOwner = redis.call('GET', KEYS[2])
    if currentOwner == 'USED:' .. ARGV[2] then
        redis.call('HSET', KEYS[1],
                'status', 'COMPROMISED',
                'updatedAt', ARGV[9])
        return 'REPLAY'
    end
    if currentOwner ~= ARGV[2]
            or redis.call('HGET', KEYS[1], 'currentDigest') ~= ARGV[3] then
        return 'MISSING'
    end
    local expectedGeneration = tonumber(
            redis.call('HGET', KEYS[1], 'generation')) + 1
    if tonumber(ARGV[5]) ~= expectedGeneration then
        return 'REVOKED'
    end
    redis.call('SET', KEYS[2], 'USED:' .. ARGV[2], 'PXAT', ARGV[8])
    redis.call('SET', KEYS[3], ARGV[2], 'PXAT', ARGV[8])
    redis.call('HSET', KEYS[1],
            'generation', ARGV[5],
            'currentDigest', ARGV[4],
            'updatedAt', ARGV[9])
    return 'ROTATED'
end

if action == 'REVOKE_FAMILY' then
    if redis.call('EXISTS', KEYS[1]) == 0 then
        return 'MISSING'
    end
    redis.call('HSET', KEYS[1],
            'status', 'REVOKED',
            'revocationReason', ARGV[3],
            'updatedAt', ARGV[4])
    return 'REVOKED'
end

if action == 'REVOKE_SUBJECT' then
    local familyKeys = redis.call('SMEMBERS', KEYS[1])
    for _, familyKey in ipairs(familyKeys) do
        if redis.call('EXISTS', familyKey) == 1 then
            redis.call('HSET', familyKey,
                    'status', 'REVOKED',
                    'revocationReason', ARGV[2],
                    'updatedAt', ARGV[3])
        end
    end
    return 'REVOKED'
end

return 'MISSING'
