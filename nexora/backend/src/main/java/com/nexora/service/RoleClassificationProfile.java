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
 * Maps each account type (user role) to preferred mail divisions and classification rules.
 */
public final class RoleClassificationProfile {

    /** Categories valid for any account type — never remapped away. */
    private static final Set<EmailCategory> UNIVERSAL_CATEGORIES = EnumSet.of(
            EmailCategory.SPAM,
            EmailCategory.PROMOTIONAL,
            EmailCategory.PERSONAL,
            EmailCategory.FINANCE);

    private RoleClassificationProfile() {}

    public static String roleLabel(UserRole role) {
        return switch (role != null ? role : UserRole.STUDENT) {
            case STUDENT -> "Student";
            case PROFESSOR -> "Professor / Academic";
            case HR_PROFESSIONAL -> "HR Professional";
            case IT_EMPLOYEE -> "IT Employee";
            case MANAGER -> "Manager";
            case FREELANCER -> "Freelancer";
        };
    }

    public static Set<EmailCategory> preferredCategories(UserRole role) {
        UserRole r = role != null ? role : UserRole.STUDENT;
        return switch (r) {
            case STUDENT -> EnumSet.of(
                    EmailCategory.ASSIGNMENT, EmailCategory.HACKATHON, EmailCategory.PLACEMENT,
                    EmailCategory.INTERNSHIP, EmailCategory.ATTENDANCE, EmailCategory.MEETING,
                    EmailCategory.ANNOUNCEMENT, EmailCategory.RESEARCH, EmailCategory.FINANCE,
                    EmailCategory.PERSONAL, EmailCategory.PROMOTIONAL, EmailCategory.SPAM);
            case PROFESSOR -> EnumSet.of(
                    EmailCategory.RESEARCH, EmailCategory.MEETING, EmailCategory.ANNOUNCEMENT,
                    EmailCategory.ASSIGNMENT, EmailCategory.ATTENDANCE, EmailCategory.PERSONAL,
                    EmailCategory.FINANCE, EmailCategory.PROMOTIONAL, EmailCategory.SPAM);
            case HR_PROFESSIONAL -> EnumSet.of(
                    EmailCategory.PLACEMENT, EmailCategory.INTERNSHIP, EmailCategory.MEETING,
                    EmailCategory.ANNOUNCEMENT, EmailCategory.PERSONAL, EmailCategory.FINANCE,
                    EmailCategory.PROMOTIONAL, EmailCategory.SPAM);
            case IT_EMPLOYEE -> EnumSet.of(
                    EmailCategory.MEETING, EmailCategory.ANNOUNCEMENT, EmailCategory.FINANCE,
                    EmailCategory.PERSONAL, EmailCategory.PROMOTIONAL, EmailCategory.SPAM);
            case MANAGER -> EnumSet.of(
                    EmailCategory.MEETING, EmailCategory.ANNOUNCEMENT, EmailCategory.FINANCE,
                    EmailCategory.PLACEMENT, EmailCategory.PERSONAL, EmailCategory.PROMOTIONAL,
                    EmailCategory.SPAM);
            case FREELANCER -> EnumSet.of(
                    EmailCategory.FINANCE, EmailCategory.MEETING, EmailCategory.ANNOUNCEMENT,
                    EmailCategory.PERSONAL, EmailCategory.PROMOTIONAL, EmailCategory.SPAM);
        };
    }

    /** Comma-separated category names for Gemini prompts. */
    public static String formatCategoryList(UserRole role) {
        List<String> names = new ArrayList<>();
        for (EmailCategory category : preferredCategories(role)) {
            names.add(category.name());
        }
        return String.join(", ", names);
    }

    /** Categories where a richer AI summary materially helps the user act. */
    public static boolean isActionableCategory(EmailCategory category, UserRole role) {
        if (category == null || category == EmailCategory.UNCATEGORIZED) {
            return false;
        }
        if (category == EmailCategory.SPAM || category == EmailCategory.PROMOTIONAL) {
            return false;
        }
        UserRole r = role != null ? role : UserRole.STUDENT;
        return switch (r) {
            case STUDENT -> category == EmailCategory.ASSIGNMENT
                    || category == EmailCategory.PLACEMENT
                    || category == EmailCategory.HACKATHON
                    || category == EmailCategory.INTERNSHIP
                    || category == EmailCategory.ATTENDANCE
                    || category == EmailCategory.MEETING;
            case PROFESSOR -> category == EmailCategory.RESEARCH
                    || category == EmailCategory.MEETING
                    || category == EmailCategory.ASSIGNMENT
                    || category == EmailCategory.ATTENDANCE;
            case HR_PROFESSIONAL -> category == EmailCategory.PLACEMENT
                    || category == EmailCategory.INTERNSHIP
                    || category == EmailCategory.MEETING;
            case IT_EMPLOYEE -> category == EmailCategory.MEETING
                    || category == EmailCategory.ANNOUNCEMENT;
            case MANAGER -> category == EmailCategory.MEETING
                    || category == EmailCategory.FINANCE
                    || category == EmailCategory.PLACEMENT;
            case FREELANCER -> category == EmailCategory.FINANCE
                    || category == EmailCategory.MEETING;
        };
    }

    /**
     * Whether Gemini is worth calling for this message. Skips obvious promos/spam and
     * mail that already has a specific local summary; targets important or ambiguous mail.
     */
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
        return switch (role != null ? role : UserRole.STUDENT) {
            case STUDENT -> """
                    - ASSIGNMENT: coursework, homework, lab submissions, professor deadlines
                    - HACKATHON: hackathons, coding competitions, tech events
                    - PLACEMENT: campus recruiting, interviews, job offers, career office
                    - INTERNSHIP: internship openings and applications
                    - ATTENDANCE: attendance warnings, class participation
                    - MEETING: lectures, office hours, calendar invites
                    - ANNOUNCEMENT: college/university notices, department updates
                    - RESEARCH: papers, journals, academic research tools (not marketing promos)
                    """;
            case PROFESSOR -> """
                    - RESEARCH: grants, publications, peer review, journal correspondence
                    - MEETING: faculty meetings, committee schedules, office hours
                    - ANNOUNCEMENT: department and university announcements
                    - ASSIGNMENT: student submissions, grading queues, coursework
                    - ATTENDANCE: class roster and attendance-related mail
                    """;
            case HR_PROFESSIONAL -> """
                    - PLACEMENT: candidates, interviews, offers, recruiting pipelines
                    - INTERNSHIP: intern programs and campus hiring
                    - MEETING: interviews, onboarding sessions, HR syncs
                    - ANNOUNCEMENT: policy updates, company-wide HR notices
                    """;
            case IT_EMPLOYEE -> """
                    - MEETING: incident bridges, sprint planning, on-call handoffs
                    - ANNOUNCEMENT: system alerts, maintenance windows, security notices
                    - FINANCE: vendor invoices, license renewals
                    """;
            case MANAGER -> """
                    - MEETING: 1:1s, team syncs, leadership updates
                    - ANNOUNCEMENT: org updates, OKRs, team broadcasts
                    - FINANCE: budgets, approvals, expense reports
                    - PLACEMENT: hiring approvals and headcount
                    """;
            case FREELANCER -> """
                    - FINANCE: invoices, payments, contracts, receipts
                    - MEETING: client calls, project kickoffs
                    - ANNOUNCEMENT: client briefs and project updates
                    """;
        };
    }

    public static String priorityRules(UserRole role) {
        return switch (role != null ? role : UserRole.STUDENT) {
            case STUDENT -> """
                    HIGH: assignment deadlines within 3 days, placement interviews, hackathon registration closing, professor direct mail
                    MEDIUM: general announcements, internship posts, meetings this week
                    LOW: newsletters, promotions, distant deadlines
                    """;
            case PROFESSOR -> """
                    HIGH: grant deadlines, student emergencies, meetings within 24 hours
                    MEDIUM: research correspondence, department notices
                    LOW: newsletters, promotions
                    """;
            case HR_PROFESSIONAL -> """
                    HIGH: offer letters, interview confirmations today, urgent candidate issues
                    MEDIUM: pipeline updates, scheduling
                    LOW: marketing tools, newsletters
                    """;
            case IT_EMPLOYEE -> """
                    HIGH: production incidents, security alerts, P0 on-call
                    MEDIUM: change windows, ticket updates
                    LOW: vendor promos
                    """;
            case MANAGER -> """
                    HIGH: approvals needed today, escalations, board prep
                    MEDIUM: team updates, budget items
                    LOW: newsletters
                    """;
            case FREELANCER -> """
                    HIGH: invoice due, client deadline today, signed contract
                    MEDIUM: project updates, meeting invites
                    LOW: marketing mail
                    """;
        };
    }

    public static String buildSystemPrompt(UserRole role, String userEmail) {
        UserRole r = role != null ? role : UserRole.STUDENT;
        return """
                You are Cortex Mail's email intelligence engine. Analyze the email for a %s account (%s).
                Respond ONLY with valid JSON — no markdown, no explanation.

                Pick the single BEST category for this account type:
                %s

                Allowed categories (pick exactly one): %s
                Never output UNCATEGORIZED — if uncertain, prefer PERSONAL or ANNOUNCEMENT based on Gmail labels and sender.

                Rules:
                - Never label marketing/newsletter tools (Paperpal, Grammarly, etc.) as ASSIGNMENT — use PROMOTIONAL.
                - ASSIGNMENT requires academic context (professor, .edu, course, homework) — not generic "submission" marketing copy.
                - Use SPAM only for Gmail SPAM label or obvious spam; PROMOTIONAL for legitimate marketing.
                - Summaries must state what the email is about and what action is needed — be specific, not generic.
                - Extract deadlines ONLY when explicitly written in the email text.

                Priority for this profile:
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
                roleLabel(r),
                userEmail != null ? userEmail : "user",
                categoryGuide(r),
                formatCategoryList(r),
                priorityRules(r));
    }

    /** One-shot profession detection from recent inbox snippets (onboarding). */
    public static String buildProfessionDetectionPrompt() {
        return """
                You are Cortex Mail's user profile analyzer. Based on the user's recent emails, determine their most likely profession or role.
                Choose ONLY from: STUDENT, PROFESSOR, IT_EMPLOYEE, HR_PROFESSIONAL, MANAGER, FREELANCER.
                Respond with valid JSON containing exactly one key "role". Example: {"role": "STUDENT"}
                No markdown, no explanation.
                """;
    }

    /** Remap categories that don't fit the user's chosen account type. */
    public static EmailCategory normalizeForRole(EmailCategory category, UserRole role) {
        if (category == null || category == EmailCategory.UNCATEGORIZED) {
            return EmailCategory.UNCATEGORIZED;
        }
        UserRole r = role != null ? role : UserRole.STUDENT;
        Set<EmailCategory> preferred = preferredCategories(r);
        if (preferred.contains(category) || UNIVERSAL_CATEGORIES.contains(category)) {
            return category;
        }
        return switch (r) {
            case STUDENT -> category;
            case PROFESSOR -> switch (category) {
                case HACKATHON, PLACEMENT, INTERNSHIP -> EmailCategory.ANNOUNCEMENT;
                default -> category;
            };
            case HR_PROFESSIONAL -> switch (category) {
                case ASSIGNMENT, ATTENDANCE, HACKATHON, RESEARCH -> EmailCategory.ANNOUNCEMENT;
                default -> category;
            };
            case IT_EMPLOYEE -> switch (category) {
                case ASSIGNMENT, HACKATHON, PLACEMENT, INTERNSHIP, ATTENDANCE, RESEARCH -> EmailCategory.ANNOUNCEMENT;
                default -> category;
            };
            case MANAGER -> switch (category) {
                case ASSIGNMENT, HACKATHON, ATTENDANCE, RESEARCH -> EmailCategory.ANNOUNCEMENT;
                default -> category;
            };
            case FREELANCER -> switch (category) {
                case ASSIGNMENT, HACKATHON, PLACEMENT, INTERNSHIP, ATTENDANCE, RESEARCH -> EmailCategory.ANNOUNCEMENT;
                default -> category;
            };
        };
    }

    /**
     * Ground-truth fallback from Gmail labels and sender when keyword rules could not classify.
     * Never returns UNCATEGORIZED — every stored inbox message gets a real division.
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

        if (sender.contains("noreply") || sender.contains("no-reply") || sender.contains("donotreply")) {
            return EmailCategory.ANNOUNCEMENT;
        }

        UserRole r = role != null ? role : UserRole.STUDENT;
        if (r == UserRole.STUDENT && (sender.contains(".edu") || sender.contains("university"))) {
            return EmailCategory.ANNOUNCEMENT;
        }
        if (r == UserRole.HR_PROFESSIONAL
                && (subject.contains("candidate") || subject.contains("interview") || body.contains("resume"))) {
            return EmailCategory.PLACEMENT;
        }
        if (r == UserRole.PROFESSOR
                && (subject.contains("research") || body.contains("research") || sender.contains(".edu"))) {
            return EmailCategory.RESEARCH;
        }
        if (r == UserRole.FREELANCER
                && (subject.contains("invoice") || subject.contains("payment") || body.contains("invoice"))) {
            return EmailCategory.FINANCE;
        }
        if (r == UserRole.IT_EMPLOYEE
                && (subject.contains("incident") || subject.contains("alert") || subject.contains("jira")
                || body.contains("on-call") || body.contains("pagerduty") || body.contains("deployment"))) {
            return EmailCategory.ANNOUNCEMENT;
        }
        if (r == UserRole.MANAGER
                && (subject.contains("approval") || subject.contains("budget") || body.contains("headcount"))) {
            return EmailCategory.FINANCE;
        }

        return EmailCategory.PERSONAL;
    }

    /** Normalize AI/local category, then apply Gmail-grounded fallback if still uncategorized. */
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
