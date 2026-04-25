package com.phases.controller;

import com.phases.dto.Commentdto;
import com.phases.dto.Postdto;
import com.phases.entity.Comment;
import com.phases.entity.Post;
import com.phases.service.SocialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SocialController {

	@Autowired
	private SocialService socialService;

	@PostMapping("/posts")
	public ResponseEntity<Post> createPost(@RequestBody Postdto request) {
		Long authorId = request.getAuthorId();
		String authorType = request.getAuthorType();
		String content = request.getContent();

		if (authorId == null || authorType == null || content == null || content.isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		Post post = socialService.createPost(authorId, authorType, content);
		return ResponseEntity.ok(post);
	}

	@PostMapping("/posts/{postId}/comments")
	public ResponseEntity<?> addComment(@PathVariable Long postId, @RequestBody Commentdto request,
			@RequestParam(required = false) Long parentCommentId) {
		Long authorId = request.getAuthorId();
		String authorType = request.getAuthorType();
		String content = request.getContent();

		if (authorId == null || authorType == null || content == null || content.isBlank()) {
			return ResponseEntity.badRequest().body("Missing authorId, authorType or content");
		}
		try {
			Comment comment = socialService.addComment(postId, authorId, authorType, content, parentCommentId);
			return ResponseEntity.ok(comment);
		} catch (RuntimeException e) {
			String msg = e.getMessage();
			if (msg.contains("429")) {
				return ResponseEntity.status(429).body(msg);
			}
			return ResponseEntity.badRequest().body(msg);
		}
	}

	@PostMapping("/posts/{postId}/like")
	public ResponseEntity<String> likePost(@PathVariable Long postId, @RequestParam Long userId) {
		try {
			socialService.likePost(postId, userId);
			return ResponseEntity.ok("Post liked");
		} catch (RuntimeException e) {
			return ResponseEntity.notFound().build();
		}
	}
}