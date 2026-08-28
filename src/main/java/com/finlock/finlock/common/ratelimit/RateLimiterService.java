package com.finlock.finlock.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@ConditionalOnBean(StringRedisTemplate.class)
public class RateLimiterService {
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    public Bucket resolveBucket(String key, int capacity, Duration refillPeriod){
        return buckets.computeIfAbsent(key, k -> createNewBucket(capacity, refillPeriod));
    }
    private Bucket createNewBucket(int capacity, Duration refillPeriod) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, refillPeriod));
        return Bucket.builder().addLimit(limit).build();
    }
    public boolean tryConsume(String key, int capacity, Duration refillPeriod){
        Bucket bucket = resolveBucket(key, capacity, refillPeriod);
        return bucket.tryConsume(1);
    }
}
