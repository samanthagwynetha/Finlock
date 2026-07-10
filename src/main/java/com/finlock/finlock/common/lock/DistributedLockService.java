package com.finlock.finlock.common.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(StringRedisTemplate.class)
public class DistributedLockService {

    private final StringRedisTemplate redisTemplate;

    public String tryLock(String key, Duration timeout) {
        String lockValue = UUID.randomUUID().toString();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent("lock:" + key, lockValue, timeout);

        return Boolean.TRUE.equals(acquired) ? lockValue : null;
    }

    public void unlock(String key, String lockValue) {
        String currentValue = redisTemplate.opsForValue().get("lock:" + key);
        if (lockValue.equals(currentValue)) {
            redisTemplate.delete("lock:" + key);
        }
    }
}