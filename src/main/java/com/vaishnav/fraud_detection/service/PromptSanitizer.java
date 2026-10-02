
package com.vaishnav.fraud_detection.service;

public class PromptSanitizer {

    // keeps letters, digits, spaces and a few harmless symbols; everything else becomes a space
    public static String clean(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        String cleaned = text.replaceAll("[^\\p{L}\\p{N} .,&'_:;()/₹-]", " ");

        // squeeze many spaces into one
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }

        return cleaned;
    }
}