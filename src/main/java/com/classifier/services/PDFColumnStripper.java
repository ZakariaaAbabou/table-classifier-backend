package com.classifier.services;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import java.io.IOException;
import java.util.*;

/**
 * A specialized PDF stripper that groups text by vertical position (Y)
 * to ensure table rows stay on the same line, even if they are in different chunks.
 */
public class PDFColumnStripper extends PDFTextStripper {
    private final String delimiter;
    private final StringBuilder csvOutput = new StringBuilder();
    private final List<TextPosition> pagePositions = new ArrayList<>();

    public PDFColumnStripper(String delimiter) throws IOException {
        this.delimiter = delimiter;
        setSortByPosition(true);
    }

    @Override
    protected void processTextPosition(TextPosition text) {
        pagePositions.add(text);
    }

    @Override
    protected void endPage(PDPage page) throws IOException {
        if (pagePositions.isEmpty()) return;

        // Group by Y coordinate (with a small tolerance for vertical misalignment)
        Map<Integer, List<TextPosition>> rows = new TreeMap<>();
        for (TextPosition pos : pagePositions) {
            // Multiply by 10 and round to group anything within 0.1 pts of each other
            int yKey = Math.round(pos.getYDirAdj() * 2); // 0.5pt tolerance
            rows.computeIfAbsent(yKey, k -> new ArrayList<>()).add(pos);
        }

        for (List<TextPosition> rowPositions : rows.values()) {
            rowPositions.sort(Comparator.comparing(TextPosition::getXDirAdj));
            
            StringBuilder rowBuilder = new StringBuilder();
            for (int i = 0; i < rowPositions.size(); i++) {
                TextPosition pos = rowPositions.get(i);
                rowBuilder.append(pos.getUnicode());

                if (i < rowPositions.size() - 1) {
                    TextPosition next = rowPositions.get(i + 1);
                    float gap = next.getXDirAdj() - (pos.getXDirAdj() + pos.getWidthDirAdj());
                    float spaceWidth = pos.getWidthOfSpace();
                    if (spaceWidth <= 0) spaceWidth = 4.0f;

                    // If gap > 1.4x spaces, it's a new column (tighter for student lists)
                    if (gap > (spaceWidth * 1.4f)) {
                        rowBuilder.append(delimiter);
                    }
                }
            }

            String finalizedRow = deDuplicateWords(rowBuilder.toString().trim());
            if (!finalizedRow.isEmpty()) {
                csvOutput.append(finalizedRow).append("\n");
            }
        }
        
        pagePositions.clear();
    }

    private String deDuplicateWords(String row) {
        String[] words = row.split(delimiter);
        StringBuilder cleanRow = new StringBuilder();
        for (String word : words) {
            String trimmed = word.trim();
            if (trimmed.isEmpty()) continue;
            
            String[] parts = trimmed.split("\\s+");
            if (parts.length >= 2 && parts.length % 2 == 0) {
                boolean halfMatch = true;
                int half = parts.length / 2;
                for (int i = 0; i < half; i++) {
                    if (!parts[i].equals(parts[i + half])) {
                        halfMatch = false;
                        break;
                    }
                }
                if (halfMatch) {
                    StringBuilder dedupped = new StringBuilder();
                    for (int i = 0; i < half; i++) {
                        dedupped.append(parts[i]).append(" ");
                    }
                    trimmed = dedupped.toString().trim();
                }
            }
            
            if (cleanRow.length() > 0) cleanRow.append(delimiter);
            cleanRow.append(trimmed);
        }
        return cleanRow.toString();
    }

    public String getCSVResult() {
        return csvOutput.toString();
    }
}
