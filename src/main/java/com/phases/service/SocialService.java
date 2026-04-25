package com.phases.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.phases.entity.Bot;
import com.phases.entity.Comment;
import com.phases.entity.Post;
import com.phases.repository.BotRepository;
import com.phases.repository.CommentRepository;
import com.phases.repository.PostRepository;

@Service
public class SocialService {

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private BotRepository botRepository;

	@Autowired
	private RedisGuardrailService redisGuardrailService;

	@Autowired
	private RedisNotificationService notificationService;

	// ==================== CREATE POST ====================
	public Post createPost(Long authorId, String authorType, String content) {
		Post post = new Post();
		post.setAuthorId(authorId);
		post.setAuthorType(authorType);
		post.setContent(content);
		post.setCreatedAt(LocalDateTime.now());
		return postRepository.save(post);
	}

	// ==================== ADD COMMENT (with all guardrails) ====================
	public Comment addComment(Long postId, Long authorId, String authorType, String content, Long parentCommentId) {

		// 1. Verify post exists
		Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));

		// 2. Calculate depth (vertical cap)
		int depth = 0;
		if (parentCommentId != null) {
			Comment parent = commentRepository.findById(parentCommentId)
					.orElseThrow(() -> new RuntimeException("Parent comment not found"));
			depth = parent.getDepthLevel() + 1;
		}

		// 3. VERTICAL CAP: max 20 levels
		if (depth > 20) {
			throw new RuntimeException("Comment thread cannot exceed 20 levels deep");
		}

		// 4. Guardrails for BOT authors only
		if ("BOT".equals(authorType)) {
			// HORIZONTAL CAP: max 100 bot replies per post
			if (!redisGuardrailService.tryAddBotReply(postId)) {
				throw new RuntimeException("429 Too Many Requests: Post already has 100 bot replies");
			}

			// COOLDOWN CAP: bot cannot interact with same human more than once per 10 min
			if ("USER".equals(post.getAuthorType())) {
				if (!redisGuardrailService.canBotInteractWithHuman(authorId, post.getAuthorId())) {
					throw new RuntimeException("Bot cannot interact with this human again for 10 minutes");
				}
			}
		}

		// 5. Save comment
		Comment comment = new Comment();
		comment.setPostId(postId);
		comment.setAuthorId(authorId);
		comment.setAuthorType(authorType);
		comment.setContent(content);
		comment.setDepthLevel(depth);
		comment.setCreatedAt(LocalDateTime.now());
		Comment saved = commentRepository.save(comment);

		// 6. Update virality score
		if ("BOT".equals(authorType)) {
			redisGuardrailService.incrementViralityScore(postId, "BOT_REPLY");

			// 7. NOTIFICATION: only when bot replies to a human's top‑level post
			// (parentCommentId == null)
			if (parentCommentId == null && "USER".equals(post.getAuthorType())) {
				// Get bot name for the notification message
				String botName = botRepository.findById(authorId).map(Bot::getName).orElse("Bot_" + authorId);
				notificationService.handleBotInteractionNotification(post.getAuthorId(), botName, postId);
			}
		} else if ("USER".equals(authorType)) {
			redisGuardrailService.incrementViralityScore(postId, "HUMAN_COMMENT");
		}

		return saved;
	}

	// ==================== LIKE POST ====================
	public void likePost(Long postId, Long userId) {
		// Verify post exists
		postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));

		// Update virality score for human like
		redisGuardrailService.incrementViralityScore(postId, "HUMAN_LIKE");
		System.out.println("Post " + postId + " liked by user " + userId);
	}
}