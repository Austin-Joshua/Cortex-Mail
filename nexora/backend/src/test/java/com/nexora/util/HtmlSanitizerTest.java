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
}
