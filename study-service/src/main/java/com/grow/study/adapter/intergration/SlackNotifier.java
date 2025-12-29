package com.grow.study.adapter.intergration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SlackNotifier {

    private final RestClient restClient;
    private final String webhookUrl;
    private final boolean enabled;

    public SlackNotifier(
             RestClient.Builder restClientBuilder,
            @Value("${slack.webhook.url:}") String webhookUrl,
            @Value("${slack.webhook.enabled:true}") boolean enabled) {
        this.restClient = restClientBuilder.build();
        this.webhookUrl = webhookUrl;
        this.enabled = enabled;
    }

    @Async("notificationExecutor")
    public void sendError(String title, String detail) {
        send(AlertLevel.ERROR, title, detail);
    }

    @Async("notificationExecutor")
    public void sendWarning(String title, String detail) {
        send(AlertLevel.WARNING, title, detail);
    }

    @Async("notificationExecutor")
    public void sendInfo(String title, String detail) {
        send(AlertLevel.INFO, title, detail);
    }

    @Async("notificationExecutor")
    public void sendSuccess(String title, String detail) {
        send(AlertLevel.SUCCESS, title, detail);
    }

    private void send(AlertLevel level, String title, String detail) {
        if (!enabled || webhookUrl.isBlank()) {
            log.debug("Slack 알림 비활성화 상태");
            return;
        }

        try {
            Map<String, Object> payload = buildPayload(level, title, detail);

            restClient.post()
                    .uri(webhookUrl)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Slack 메시지 전송 완료 - level: {}, title: {}", level, title);
        } catch (Exception e) {
            log.error("Slack 메시지 전송 실패: {}", e.getMessage());
        }
    }

    private Map<String, Object> buildPayload(AlertLevel level, String title, String detail) {
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("color", level.getColor());
        attachment.put("title", level.getEmoji() + " " + title);
        attachment.put("text", detail);
        attachment.put("footer", "Growple Study Service");
        attachment.put("ts", Instant.now().getEpochSecond());

        return Map.of("attachments", List.of(attachment));
    }

    @Getter
    @RequiredArgsConstructor
    private enum AlertLevel {
        ERROR(":rotating_light:", "#FF0000"),      // 🚨 빨강
        WARNING(":warning:", "#FFA500"),           // ⚠️ 주황
        INFO(":information_source:", "#0078D7"),   // ℹ️ 파랑
        SUCCESS(":white_check_mark:", "#36A64F");  // ✅ 초록

        private final String emoji;
        private final String color;
    }
}