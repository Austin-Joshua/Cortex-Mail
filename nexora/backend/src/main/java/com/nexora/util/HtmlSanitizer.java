package com.nexora.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Server-side HTML sanitization for Gmail MIME bodies.
 * Must run before persistence / API / UI — never render raw Gmail HTML.
 */
public final class HtmlSanitizer {

    private static final Safelist MAIL_SAFELIST = Safelist.relaxed()
            .addTags("img", "hr", "div", "span", "table", "thead", "tbody", "tr", "td", "th", "center", "font")
            .addAttributes(":all", "class", "align", "valign", "width", "height", "bgcolor", "color")
            .addAttributes("img", "src", "alt", "title")
            .addAttributes("a", "href", "title", "target")
            .addProtocols("img", "src", "http", "https", "cid")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addEnforcedAttribute("a", "rel", "noopener noreferrer")
            .addEnforcedAttribute("a", "target", "_blank");

    private HtmlSanitizer() {}

    public static String sanitize(String html) {
        if (html == null || html.isBlank()) return null;
        Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
        String cleaned = Jsoup.clean(html, "", MAIL_SAFELIST, settings);
        return cleaned.isBlank() ? null : cleaned;
    }

    public static String htmlToPlainText(String html) {
        if (html == null || html.isBlank()) return "";
        return Jsoup.parse(html).text();
    }
}
