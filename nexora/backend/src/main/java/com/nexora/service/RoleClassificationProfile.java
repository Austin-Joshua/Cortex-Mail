package com.nexora.service;

import com.nexora.model.Email;
import com.nexora.model.Email.EmailCategory;
import com.nexora.model.Email.Priority;
import com.nexora.model.User.UserRole;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Universal mail classification for every Cortex Mail account.
 * Categories come from message content, Gmail labels, and sender signals —
 * not from profession / year-of-study / job-title profiles.
 */
public final class RoleClassificationProfile {

    private static final Set<EmailCategory> ALL_CATEGORIES = EnumSet.of(
            EmailCategory.MEETING,
            EmailCategory.ASSIGNMENT,
            EmailCategory.ANNOUNCEMENT,
            EmailCategory.FINANCE,
            EmailCategory.PLACEMENT,
            EmailCategory.INTERNSHIP,
            EmailCategory.HACKATHON,
            EmailCategory.RESEARCH,
            EmailCategory.ATTENDANCE,
            EmailCategory.PERSONAL,
            EmailCategory.PROMOTIONAL,
            EmailCategory.SPAM);

    private static final Set<EmailCategory> ACTIONABLE = EnumSet.of(
            EmailCategory.ASSIGNMENT,
            EmailCategory.PLACEMENT,
            EmailCategory.HACKATHON,
            EmailCategory.INTERNSHIP,
            EmailCategory.ATTENDANCE,
            EmailCategory.MEETING,
            EmailCategory.FINANCE,
            EmailCategory.RESEARCH);

    private RoleClassificationProfile() {}

    /** @deprecated Kept for JWT/API compatibility; classification ignores profession labels. */
    public static String roleLabel(UserRole role) {
        return "mailbox";
    }

    public static Set<EmailCategory> preferredCategories(UserRole role) {
        return EnumSet.copyOf(ALL_CATEGORIES);
    }

    public static String formatCategoryList(UserRole role) {
        List<String> names = new ArrayList<>();
        for (EmailCategory category : ALL_CATEGORIES) {
            names.add(category.name());
        }
        return String.join(", ", names);
    }

    public static boolean isActionableCategory(EmailCategory category, UserRole role) {
        if (category == null || category == EmailCategory.UNCATEGORIZED) {
            return false;
        }
        if (category == EmailCategory.SPAM || category == EmailCategory.PROMOTIONAL) {
            return false;
        }
        return ACTIONABLE.contains(category) || category == EmailCategory.ANNOUNCEMENT
                || category == EmailCategory.PERSONAL;
    }

    public static boolean worthGeminiRefinement(String localCategory, String localSummary,
                                                Email email, UserRole role) {
        if (localCategory == null || localCategory.isBlank()
                || "UNCATEGORIZED".equalsIgnoreCase(localCategory)) {
            return true;
        }
        if ("SPAM".equalsIgnoreCase(localCategory) || "PROMOTIONAL".equalsIgnoreCase(localCategory)) {
            return false;
        }

        boolean genericSummary = isGenericSummary(localSummary);

        if (email != null) {
            if (Boolean.TRUE.equals(email.getIsImportant()) || Boolean.TRUE.equals(email.getIsStarred())) {
                return genericSummary;
            }
            if (email.getPriority() == Priority.HIGH && genericSummary) {
                return true;
            }
        }

        EmailCategory parsed = parseCategoryName(localCategory);
        if (parsed == null) {
            return true;
        }
        return genericSummary && isActionableCategory(parsed, role);
    }

    public static boolean isGenericSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return true;
        }
        return summary.startsWith("Email from ")
                || summary.startsWith("Notice from ")
                || summary.startsWith("Promotion from ");
    }

    public static EmailCategory parseCategoryName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return EmailCategory.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String categoryGuide(UserRole role) {
        return """
                - MEETING: calendar invites, Zoom/Meet/Teams, 1:1s, scheduled calls
                - ASSIGNMENT: tasks, homework, submissions, action items with a clear deliverable
                - ANNOUNCEMENT: org/school/product notices, security alerts, system updates
                - FINANCE: invoices, receipts, payments, bills, banking, expenses
                - PLACEMENT: job offers, recruiting, interviews, career opportunities
                - INTERNSHIP: intern programs and applications
                - HACKATHON: hackathons, competitions, conferences, events to register for
                - RESEARCH: papers, journals, grants, academic research correspondence
                - ATTENDANCE: attendance warnings, check-ins, roster notices
                - PERSONAL: 1:1 human conversation that does not fit a stronger category
                - PROMOTIONAL: marketing, newsletters, sales (not spam)
                - SPAM: Gmail SPAM label or obvious junk
                """;
    }

    public static String priorityRules(UserRole role) {
        return """
                HIGH: explicit deadlines within 3 days, interviews, approvals needed today, security alerts, starred/important mail that needs a reply
                MEDIUM: meetings this week, useful updates, opportunities without an urgent close date
                LOW: newsletters, promotions, FYI announcements with no action
                """;
    }

    public static String buildSystemPrompt(UserRole role, String userEmail) {
        return """
                You are Cortex Mail's email intelligence engine. Analyze this email for the signed-in mailbox (%s).
                Personalize from THIS message's content, sender, and Gmail signals — never assume a profession, job title, or year of study.
                Respond ONLY with valid JSON — no markdown, no explanation.

                Pick the single BEST category from content:
                %s

                Allowed categories (pick exactly one): %s
                Never output UNCATEGORIZED — if uncertain, prefer PERSONAL or ANNOUNCEMENT based on Gmail labels and sender.

                Rules:
                - Never label marketing/newsletter tools as ASSIGNMENT — use PROMOTIONAL.
                - ASSIGNMENT requires a real task/deliverable — not generic "submission" marketing copy.
                - Use SPAM only for Gmail SPAM label or obvious spam; PROMOTIONAL for legitimate marketing.
                - Summaries must state what the email is about and what action is needed — be specific.
                - Extract deadlines ONLY when explicitly written in the email text.

                Priority:
                %s

                JSON structure:
                {
                  "category": "CATEGORY_NAME",
                  "priority": "HIGH | MEDIUM | LOW",
                  "summary": "2-3 specific sentences",
                  "action_items": [{"action_type": "REGISTER|REPLY|SUBMIT|UPLOAD|REVIEW|ATTEND|OTHER", "description": "...", "deadline": null}],
                  "deadline": null
                }
                """.formatted(
                userEmail != null ? userEmail : "user",
                categoryGuide(role),
                formatCategoryList(role),
                priorityRules(role));
    }

    /** Pass-through — no profession remapping. */
    public static EmailCategory normalizeForRole(EmailCategory category, UserRole role) {
        if (category == null) {
            return EmailCategory.UNCATEGORIZED;
        }
        return category;
    }

    /**
     * Fallback from Gmail labels and sender when keyword rules could not classify.
     * Content-driven for every account — never gated on profession.
     */
    public static EmailCategory inferFromGmailAndSender(Email email, UserRole role) {
        if (email == null) {
            return EmailCategory.PERSONAL;
        }

        String labels = (email.getGmailLabelIds() != null ? email.getGmailLabelIds() : "").toUpperCase();
        String sender = (email.getSenderEmail() != null ? email.getSenderEmail() : "").toLowerCase();
        String subject = (email.getSubject() != null ? email.getSubject() : "").toLowerCase();
        String body = bodyText(email).toLowerCase();

        if (labels.contains("SPAM")) return EmailCategory.SPAM;
        if (labels.contains("CATEGORY_PROMOTIONS")) return EmailCategory.PROMOTIONAL;
        if (labels.contains("CATEGORY_PURCHASES")) return EmailCategory.FINANCE;
        if (labels.contains("CATEGORY_SOCIAL")) return EmailCategory.PERSONAL;
        if (labels.contains("CATEGORY_FORUMS")) return EmailCategory.ANNOUNCEMENT;
        if (labels.contains("CATEGORY_UPDATES")) return EmailCategory.ANNOUNCEMENT;
        if (labels.contains("CATEGORY_PERSONAL")) return EmailCategory.PERSONAL;

        if (sender.contains("canvas.") || sender.contains("moodle.") || sender.contains("blackboard")
                || sender.contains("classroom.google")) {
            return EmailCategory.ASSIGNMENT;
        }

        if (sender.contains("google.com") || sender.contains("accounts.google")
                || subject.contains("security alert") || subject.contains("sign-in")
                || subject.contains("verification code") || body.contains("security alert")) {
            return EmailCategory.ANNOUNCEMENT;
        }

        if (body.contains("unsubscribe") || body.contains("view in browser") || body.contains("% off")) {
            return EmailCategory.PROMOTIONAL;
        }

        if (subject.contains("meeting") || body.contains("google meet") || body.contains("zoom")
                || subject.contains("invite:") || labels.contains("IMPORTANT")) {
            return EmailCategory.MEETING;
        }

        if (subject.contains("invoice") || subject.contains("payment") || subject.contains("receipt")
                || body.contains("invoice") || body.contains("amount due")) {
            return EmailCategory.FINANCE;
        }

        if (subject.contains("interview") || subject.contains("job offer") || subject.contains("application status")
                || body.contains("we would like to invite you to interview")) {
            return EmailCategory.PLACEMENT;
        }

        if (subject.contains("internship") || body.contains("internship")) {
            return EmailCategory.INTERNSHIP;
        }

        if (subject.contains("hackathon") || subject.contains("conference") || body.contains("register for the event")) {
            return EmailCategory.HACKATHON;
        }

        if (subject.contains("research") || body.contains("peer review") || body.contains("manuscript")) {
            return EmailCategory.RESEARCH;
        }

        if (subject.contains("attendance") || body.contains("attendance")) {
            return EmailCategory.ATTENDANCE;
        }

        if (sender.contains("noreply") || sender.contains("no-reply") || sender.contains("donotreply")
                || sender.contains(".edu") || sender.contains("university")) {
            return EmailCategory.ANNOUNCEMENT;
        }

        return EmailCategory.PERSONAL;
    }

    public static EmailCategory resolveCategory(EmailCategory parsed, Email email, UserRole role) {
        EmailCategory normalized = normalizeForRole(parsed, role);
        if (normalized != EmailCategory.UNCATEGORIZED) {
            return normalized;
        }
        EmailCategory inferred = inferFromGmailAndSender(email, role);
        EmailCategory resolved = normalizeForRole(inferred, role);
        return resolved != EmailCategory.UNCATEGORIZED ? resolved : inferred;
    }

    private static String bodyText(Email email) {
        if (email.getBodySnippet() != null && !email.getBodySnippet().isBlank()) {
            return email.getBodySnippet();
        }
        return email.getBodyFull() != null ? email.getBodyFull() : "";
    }
}
