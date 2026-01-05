package com.grow.study.application;

import com.grow.study.adapter.persistence.AiAnswerQueueRepository;
import com.grow.study.adapter.persistence.PostCommentJpaRepository;
import com.grow.study.domain.board.PostComment;
import com.grow.study.domain.llm.AiAnswerQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnswerProcessor {

    private final ChatClient.Builder chatClient;
    private final PostCommentJpaRepository commentRepository;
    private final AiAnswerQueueRepository aiAnswerQueueRepository;

    @Value("classpath:/study-assistant.st")
    private Resource systemPromptResource;

    @Transactional
    public void process(AiAnswerQueue item) {
        item.markProcessing();

        try {
            String answer = generateAnswer(item.getQuestionContent());

            PostComment aiComment = PostComment.createAiComment(item.getPost(), answer);
            commentRepository.save(aiComment);

            // Post 댓글 수 증가
            item.getPost().increaseCommentCount();

            item.markCompleted();
            log.info("AI answer created for post: {}", item.getPost().getId());

            aiAnswerQueueRepository.save(item);

        } catch (Exception e) {
            log.error("Failed to generate AI answer for post: {}", item.getPost().getId(), e);
            item.markFailed(e.getMessage());
        }
    }

    private String generateAnswer(String question) {
        return chatClient.build()
                .prompt()
                .system(new SystemPromptTemplate(systemPromptResource).createMessage().getText())
                .user(question)
                .call()
                .content();
    }
}
