-- Compare non-negative decimal versions without Lua's floating-point rounding.
local function newer(current, incoming)
    return current and (#current > #incoming or (#current == #incoming and current > incoming))
end

if newer(redis.call('GET', KEYS[3]), ARGV[3])
        or newer(redis.call('GET', KEYS[4]), ARGV[4]) then
    return -1
end

local created = 1 - redis.call('EXISTS', KEYS[5])
redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[6])
redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[6])
-- Version watermarks outlive snapshots so expiration cannot admit an older writer.
redis.call('SET', KEYS[3], ARGV[3])
redis.call('SET', KEYS[4], ARGV[4])
redis.call('SET', KEYS[5], ARGV[5], 'PX', ARGV[6])
redis.call('DEL', KEYS[6])
return created
