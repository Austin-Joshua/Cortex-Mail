package com.nexora.service;

import com.nexora.model.Email;
import com.nexora.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Summarizes email threads with 2+ messages using Gemini.
 * Stores the summary in the latest email aiSummary field.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SummarizationService {

    private final EmailRepository emailRepository;
    private final EmailClassificationService classificationService;

    private static final String THREAD_SUMMARY_PREFIX = "[Thread Summary] ";

    @Transactional
    public void summarizeThreads(Long userId) {
        List<Email> userEmails = emailRepository
                .findByUserIdOrderByReceivedAtDesc(userId, Pageable.unpaged())
                .getContent();

        Map<String, List<Email>> threads = userEmails.stream()
                .filter(e -> e.getGmailThreadId() != null && !e.getGmailThreadId().isBlank())
                .collect(Collectors.groupingBy(e -> e.getGmailThreadId()));

        int summarized = 0;
        for (Map.Entry<String, List<Email>> entry : threads.entrySet()) {
            List<Email> threadEmails = entry.getValue();
            if (threadEmails.size() < 2) continue;

            threadEmails.sort(Comparator.comparing(e -> e.getReceivedAt()));
            Email latestEmail = threadEmails.get(threadEmails.size() - 1);

            if (latestEmail.getAiSummary() != null
                    && latestEmail.getAiSummary().startsWith(THREAD_SUMMARY_PREFIX)) {
                continue;
            }

            log.info("Summarizing thread {} ({} messages) for user {}",
                    entry.getKey(), threadEmails.size(), userId);

            try {
                String threadContext = buildThreadContext(threadEmails);
                String systemPrompt = "You are Nexora's thread summarization engine. "
                        + "Analyze the email thread. Write a concise 2-3 sentence summary. "
                        + "Respond ONLY with the summary text. No markdown, no prefixes.";

                String summary = classificationService.summarizeThread(systemPrompt, threadContext);
                if (summary != null && !summary.isBlank()) {
                    latestEmail.setAiSummary(THREAD_SUMMARY_PREFIX + summary.trim());
                    emailRepository.save(latestEmail);
                    summarized++;
                    log.info("Thread {} summarized successfully", entry.getKey());
                }
            } catch (Exception e) {
                log.error("Failed to summarize thread {}: {}", entry.getKey(), e.getMessage());
            }
        }

        log.info("Summarization complete for user {} - {} threads summarized", userId, summarized);
    }

    private String buildThreadContext(List<Email> emails) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < emails.size(); i++) {
            Email e = emails.get(i);
            sb.append("--- Message #").append(i + 1).append(" ---\n");
            sb.append("From: ").append(e.getSenderName() != null ? e.getSenderName() : "Unknown")
              .append(" <").append(e.getSenderEmail()).append(">\n");
            sb.append("Received: ").append(e.getReceivedAt()).append("\n");
            sb.append("Subject: ").append(e.getSubject() != null ? e.getSubject() : "(no subject)").append("\n");
            String body = e.getBodyFull() != null ? e.getBodyFull() : e.getBodySnippet();
            if (body == null) body = "";
            if (body.length() > 1000) body = body.substring(0, 1000) + "...";
            sb.append("Body:\n").append(body).append("\n\n");
        }
        return sb.toString();
    }
}
