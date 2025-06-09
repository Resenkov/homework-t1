package resenkov.work.t1metricsstarter.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;

@Aspect
@Component
public class CachedAspect {

    private static final Logger logger = LoggerFactory.getLogger(CachedAspect.class);

    private final ConcurrentMap<CacheKey, CacheValue> cache = new ConcurrentHashMap<>();
    private final long entryTtlMs;
    private final ScheduledExecutorService sweeper = Executors.newSingleThreadScheduledExecutor();

    public CachedAspect(@Value("${app.cache.entryTtlMs}") long entryTtlMs) {
        this.entryTtlMs = entryTtlMs;
        sweeper.scheduleAtFixedRate(this::evictExpired, entryTtlMs, entryTtlMs, TimeUnit.MILLISECONDS);
    }

    @Around("@annotation(resenkov.work.t1metricsstarter.aop.Cached)")
    public Object cache(ProceedingJoinPoint pjp) throws Throwable {
        CacheKey key = new CacheKey(pjp.getSignature().toLongString(), pjp.getArgs());
        long now = System.currentTimeMillis();
        CacheValue existing = cache.get(key);

        if (existing != null && existing.expireAt > now) {
            logger.info(">> @Cached: cache HIT for key {}", key);
            return existing.value;
        }

        logger.info(">> @Cached: cache MISS for key {}", key);
        Object result = pjp.proceed();
        cache.put(key, new CacheValue(result, now + entryTtlMs));
        return result;
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        for (Map.Entry<CacheKey, CacheValue> e : cache.entrySet()) {
            if (e.getValue().expireAt <= now) {
                cache.remove(e.getKey());
                logger.info(">> @Cached: evicted key {}", e.getKey());
            }
        }
    }

    @Override public String toString() { return "CachedAspect"; }

    private static class CacheKey {
        final String methodSig;
        final Object[] args;
        CacheKey(String methodSig, Object[] args) {
            this.methodSig = methodSig;
            this.args = args != null ? args : new Object[0];
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey k = (CacheKey) o;
            return methodSig.equals(k.methodSig) && Arrays.deepEquals(args, k.args);
        }
        @Override public int hashCode() {
            return methodSig.hashCode() * 31 + Arrays.deepHashCode(args);
        }
        @Override public String toString() {
            return methodSig + Arrays.toString(args);
        }
    }

    private static class CacheValue {
        final Object value;
        final long expireAt;
        CacheValue(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}
