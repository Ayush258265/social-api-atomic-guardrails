package com.phases.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.phases.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

}
