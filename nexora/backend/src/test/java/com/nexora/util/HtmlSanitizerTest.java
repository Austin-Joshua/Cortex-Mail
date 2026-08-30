package com.nexora.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HtmlSanitizerTest {

    @Test
    void stripsScriptTags() {
        String dirty = "<p>Hello</p><script>alert(1)</script>";
        String clean = HtmlSanitizer.sanitize(dirty);
        assertNotNull(clean);
        assertFalse(clean.toLowerCase().contains("script"));
        assertTrue(clean.contains("Hello"));
    }

    @Test
    void htmlToPlainText() {
        assertEquals("Hi there", HtmlSanitizer.htmlToPlainText("<b>Hi</b> there"));
    }

    @Test
    void nullSafe() {
        assertNull(HtmlSanitizer.sanitize(null));
        assertEquals("", HtmlSanitizer.htmlToPlainText(null));
    }

    @Test
    void stripsIframesAndEventHandlers() {
        String dirty = "<div onclick=\"steal()\"><iframe src=\"https://evil\"></iframe>Safe</div>";
        String clean = HtmlSanitizer.sanitize(dirty);
        assertNotNull(clean);
        assertFalse(clean.toLowerCase().contains("iframe"));
        assertFalse(clean.toLowerCase().contains("onclick"));
        assertTrue(clean.contains("Safe"));
    }

    @Test
    void stripsJavascriptAndDataUrls() {
        String dirty = "<a href=\"javascript:alert(1)\">x</a><img src=\"data:text/html;base64,PHNjcmlwdD4=\">";
        String clean = HtmlSanitizer.sanitize(dirty);
        assertNotNull(clean);
        assertFalse(clean.toLowerCase().contains("javascript"));
        assertFalse(clean.toLowerCase().contains("data:"));
    }

    @Test
    void enforcesSafeLinkRel() {
        String clean = HtmlSanitizer.sanitize("<a href=\"https://example.com\">Link</a>");
        assertNotNull(clean);
        assertTrue(clean.contains("noopener"));
        assertTrue(clean.contains("_blank"));
    }
}
