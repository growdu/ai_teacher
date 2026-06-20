package com.aiteacher.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Custom AI usage metrics exposed to Prometheus
 */
@Component
public class AiMetrics {

    private final Counter chatRequestsTotal;
    private final Counter chatErrorsTotal;
    private final Counter chatTokensTotal;
    private final Timer chatLatencyTimer;
    private final Counter subscriptionChecksTotal;
    private final Counter rateLimitRejectionsTotal;

    public AiMetrics(MeterRegistry registry) {
        this.chatRequestsTotal = Counter.builder("ai_teacher_chat_requests_total")
                .description("Total number of AI chat requests")
                .register(registry);

        this.chatErrorsTotal = Counter.builder("ai_teacher_chat_errors_total")
                .description("Total number of AI chat errors")
                .register(registry);

        this.chatTokensTotal = Counter.builder("ai_teacher_chat_tokens_total")
                .description("Total number of AI chat tokens consumed")
                .register(registry);

        this.chatLatencyTimer = Timer.builder("ai_teacher_chat_latency_seconds")
                .description("AI chat request latency")
                .register(registry);

        this.subscriptionChecksTotal = Counter.builder("ai_teacher_subscription_checks_total")
                .description("Total subscription validation checks")
                .register(registry);

        this.rateLimitRejectionsTotal = Counter.builder("ai_teacher_rate_limit_rejections_total")
                .description("Total rate limit rejections (429 responses)")
                .register(registry);
    }

    public void recordChatRequest(long latencyMs, int tokens) {
        chatRequestsTotal.increment();
        chatLatencyTimer.record(latencyMs, TimeUnit.MILLISECONDS);
        if (tokens > 0) {
            chatTokensTotal.increment(tokens);
        }
    }

    public void recordChatError() {
        chatErrorsTotal.increment();
    }

    public void recordSubscriptionCheck() {
        subscriptionChecksTotal.increment();
    }

    public void recordRateLimitRejection() {
        rateLimitRejectionsTotal.increment();
    }
}
