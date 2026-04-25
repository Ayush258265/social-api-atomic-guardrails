package com.phases.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@EnableScheduling
public class ScheduledNotificationSweeper {

	@Autowired
	private RedisNotificationService notificationService;

	// Runs every 5 minutes (300,000 ms) for testing.
	// In production you would change to 15 minutes (900,000 ms).
	@Scheduled(fixedRate = 300000)
	public void sweepPendingNotifications() {
		System.out.println("--- Running notification sweeper ---");

		// Get all Redis keys for pending notifications
		Set<String> keys = notificationService.getAllPendingNotificationKeys();
		if (keys == null || keys.isEmpty()) {
			System.out.println("No pending notifications.");
			return;
		}

		for (String key : keys) {
			// Extract user ID from key pattern: "user:pending_notifs:123"
			Long userId = extractUserIdFromKey(key);
			if (userId == null)
				continue;

			// Pop all pending messages for this user (list is cleared inside the method)
			List<String> messages = notificationService.popAllPendingNotifications(userId);
			if (messages == null || messages.isEmpty())
				continue;

			// Summarize: first message gives the first bot name; the rest are "others"
			String firstMessage = messages.get(0);
			String firstBotName = extractBotName(firstMessage);
			int othersCount = messages.size() - 1;

			String summary;
			if (othersCount == 0) {
				summary = "Summarized Push Notification: " + firstBotName + " interacted with your posts.";
			} else {
				summary = "Summarized Push Notification: " + firstBotName + " and " + othersCount
						+ " others interacted with your posts.";
			}
			System.out.println(summary);
		}
	}

	// Helper: extract user ID from key like "user:pending_notifs:42"
	private Long extractUserIdFromKey(String key) {
		try {
			String[] parts = key.split(":");
			if (parts.length >= 3) {
				return Long.parseLong(parts[2]);
			}
		} catch (NumberFormatException e) {
			// ignore
		}
		return null;
	}

	// Helper: extract bot name from message like "Bot HelperBot replied to your
	// post #5"
	private String extractBotName(String message) {
		Pattern pattern = Pattern.compile("Bot (\\S+) replied");
		Matcher matcher = pattern.matcher(message);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return "Some bot";
	}
}