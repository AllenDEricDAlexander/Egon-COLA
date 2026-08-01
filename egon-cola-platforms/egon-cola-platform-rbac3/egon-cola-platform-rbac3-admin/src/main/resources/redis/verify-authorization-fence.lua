if redis.call('exists', KEYS[1]) == 1 then
    return 1
end
return 0
