local fingerprint = ARGV[3] .. ':' .. ARGV[6]
local existing = redis.call('GET', KEYS[3])

if existing and existing ~= fingerprint then
    return {3}
end

local currentVersion = redis.call('GET', KEYS[2])
if not existing and currentVersion then
    if currentVersion ~= ARGV[1] and currentVersion ~= ARGV[2] then
        return {4, currentVersion}
    end
    if currentVersion == ARGV[2] then
        local currentValue = redis.call('GET', KEYS[1])
        if currentValue and currentValue ~= ARGV[4] then
            return {3}
        end
    end
end

redis.call('SET', KEYS[1], ARGV[4])
redis.call('SET', KEYS[2], ARGV[2])
redis.call('SET', KEYS[3], fingerprint)
redis.call('PUBLISH', KEYS[4], ARGV[5])

if existing then
    return {2, ARGV[2]}
end
return {1, ARGV[2]}
