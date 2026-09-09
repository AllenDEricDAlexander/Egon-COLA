local function newer(current, incoming)
    return current and (#current > #incoming or (#current == #incoming and current > incoming))
end

if newer(redis.call('GET', KEYS[1]), ARGV[1])
        or newer(redis.call('GET', KEYS[2]), ARGV[2]) then
    return -1
end

redis.call('SET', KEYS[1], ARGV[1])
redis.call('SET', KEYS[2], ARGV[2])
return redis.call('DEL', KEYS[3])
