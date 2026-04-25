package com.phases.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class RedisGuardrailService {

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	// ========== 1. Virality Score ==========
	public void incrementViralityScore(Long postId, String interactionType) {
		String key = "post:" + postId + ":virality_score";
		int points = 0;
		switch (interactionType) {
		case "BOT_REPLY":
			points = 1;
			break;
		case "HUMAN_LIKE":
			points = 20;
			break;
		case "HUMAN_COMMENT":
			points = 50;
			break;
		}
		if (points > 0) {
			redisTemplate.opsForValue().increment(key, points);
		}
	}

	// ========== 2. Horizontal Cap (max 100 bot replies per post) ==========
	public boolean tryAddBotReply(Long postId) {
		String key = "post:" + postId + ":bot_count";
		// Lua script: increment, if <=100 return new count, else decrement back and
		// return -1
		String luaScript = "local count = redis.call('INCR', KEYS[1]) " + "if count <= tonumber(ARGV[1]) then "
				+ "    return count " + "else " + "    redis.call('DECR', KEYS[1]) " + "    return -1 " + "end";
		DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
		Long result = redisTemplate.execute(script, List.of(key), "100");
		return result != -1; // true = allowed (<=100), false = cap exceeded
	}

	// ========== 3. Cooldown Cap (10 min bot-human) ==========
	public boolean canBotInteractWithHuman(Long botId, Long humanId) {
		String key = "cooldown:bot_" + botId + ":human_" + humanId;
		// setIfAbsent succeeds only if key does not already exist
		Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(10));
		return Boolean.TRUE.equals(success); // true = allowed (no cooldown), false = blocked
	}

	// Optional: get virality score (for debugging)
	public String getViralityScore(Long postId) {
		return redisTemplate.opsForValue().get("post:" + postId + ":virality_score");
	}
}