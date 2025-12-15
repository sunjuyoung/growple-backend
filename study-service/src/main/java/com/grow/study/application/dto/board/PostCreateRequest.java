package com.grow.study.application.dto.board;

import com.grow.study.domain.board.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostCreateRequest {

    @NotNull(message = "카테고리는 필수입니다.")
    private PostCategory category;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Builder
    public PostCreateRequest(PostCategory category, String title, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
    }
}
