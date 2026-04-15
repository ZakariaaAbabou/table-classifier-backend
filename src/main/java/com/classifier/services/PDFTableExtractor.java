package com.classifier.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A simple but precise extractor that converts PDF lines into CSV rows
 * by detecting horizontal gaps between words.
 */
public class PDFTableExtractor extends PDFTextStripper {
    private final String delimiter;
    private final List<String> lines = new ArrayList<>();
    private final StringBuilder currentLine = new StringBuilder();
    private float lastX = -1;
    private float lastWidth = -1;

    public PDFTableExtractor(String delimiter) throws IOException {
        this.delimiter = delimiter;
        setSortByPosition(true); // Ensures we process text in reading order
    }

    public List<String> extract(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            getText(document);
        }
        return lines;
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        StringBuilder row = new StringBuilder();
        if (textPositions.isEmpty()) return;

        for (int i = 0; i < textPositions.size(); i++) {
            TextPosition pos = textPositions.get(i);
            String chars = pos.getUnicode();

            if (i > 0) {
                TextPosition prev = textPositions.get(i - 1);
                float gap = pos.getXDirAdj() - (prev.getXDirAdj() + prev.getWidthDirAdj());
                
                // If the gap is larger than ~2.5 spaces (roughly 10-15 units depending on font), 
                // we treat it as a column break and inject our delimiter.
                float threshold = prev.getWidthOfSpace() * 2.2f;
                if (threshold <= 0) threshold = 10f; // fallback for fonts without space width

                if (gap > threshold) {
                    row.append(delimiter);
                }
            }
            row.append(chars);
        }

        String result = row.toString().trim();
        if (!result.isEmpty()) {
            lines.add(result);
        }
    }
}
