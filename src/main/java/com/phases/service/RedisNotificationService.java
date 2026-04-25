package com.phases.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisNotificationService {

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	private static final String NOTIF_COOLDOWN_PREFIX = "user_notif_cooldown:";
	private static final String PENDING_NOTIF_PREFIX = "user:pending_notifs:";

	/**
	 * Handles bot interaction notification throttling.
	 * 
	 * @param userId  the human user who owns the post
	 * @param botName name of the bot (or botId)
	 * @param postId  the post that was replied to
	 * @return true if immediate notification was sent (cooldown was absent)
	 */
	public boolean handleBotInteractionNotification(Long userId, String botName, Long postId) {
		String cooldownKey = NOTIF_COOLDOWN_PREFIX + userId;
		String message = "Bot " + botName + " replied to your post #" + postId;

		// Check if cooldown key exists
		Boolean hasCooldown = redisTemplate.hasKey(cooldownKey);
		if (Boolean.TRUE.equals(hasCooldown)) {
			// Cooldown active → push to pending list
			String pendingKey = PENDING_NOTIF_PREFIX + userId;
			redisTemplate.opsForList().rightPush(pendingKey, message);
			// Set expiry on pending list so it doesn't stay forever (optional, e.g., 1
			// hour)
			redisTemplate.expire(pendingKey, 1, TimeUnit.HOURS);
			return false; // not sent immediately
		} else {
			// No cooldown → send immediate notification (log only)
			System.out.println("Push Notification Sent to User " + userId + ": " + message);
			// Set 15-minute cooldown
			redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofMinutes(15));
			return true; // immediate notification logged
		}
	}

	/**
	 * Get all keys for pending notifications (used by scheduler).
	 */
	public Set<String> getAllPendingNotificationKeys() {
		return redisTemplate.keys(PENDING_NOTIF_PREFIX + "*");
	}

	/**
	 * Pop all pending messages for a specific user and clear the list.
	 */
	public List<String> popAllPendingNotifications(Long userId) {
		String key = PENDING_NOTIF_PREFIX + userId;
		List<String> messages = redisTemplate.opsForList().range(key, 0, -1);
		if (messages != null && !messages.isEmpty()) {
			redisTemplate.delete(key);
		}
		return messages;
	}
}