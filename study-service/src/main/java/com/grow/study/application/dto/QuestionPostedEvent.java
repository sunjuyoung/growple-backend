package com.grow.study.application.dto;

import com.grow.study.domain.board.Post;

public record QuestionPostedEvent(
        Long postId,
        Post post
) {}