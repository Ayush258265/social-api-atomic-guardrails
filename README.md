# social-api-atomic-guardrails

## Phase 2 – Redis Virality Engine & Atomic Locks (Detailed Explanation)

### Overview
Phase 2 introduces Redis as an in‑memory data store to handle real‑time virality scoring and three critical guardrails that prevent AI bot runaways. All operations are atomic and stateless – the Spring Boot application stores no counters or cooldowns in local memory.

---

### 1. Virality Score (Real‑time Calculation)

Whenever a user interacts with a post, we update a Redis key `post:{postId}:virality_score` using atomic increments.

| Interaction  | Points |
|--------------|--------|
| Bot reply    | +1     |
| Human like   | +20    |
| Human comment| +50    |

**Implementation (`RedisGuardrailService.incrementViralityScore`)**:
- Uses `redisTemplate.opsForValue().increment(key, points)` – atomic even under high concurrency.
- The score lives only in Redis; PostgreSQL stores only the content, keeping the database free from high‑frequency writes.

---

### 2. The Three Atomic Guardrails

#### a) Horizontal Cap – Max 100 bot replies per post
- **Redis key:** `post:{postId}:bot_count`
- **Problem:** 200 concurrent bot requests could otherwise insert more than 100 comments.
- **Solution:** A Lua script that atomically increments the counter and checks the limit.
  ```lua
  local count = redis.call('INCR', KEYS[1])
  if count <= tonumber(ARGV[1]) then
      return count
  else
      redis.call('DECR', KEYS[1])
      return -1
  end











  


