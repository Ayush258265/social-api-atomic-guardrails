package com.phases.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.phases.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

}
