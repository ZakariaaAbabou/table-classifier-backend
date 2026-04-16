package com.classifier.services;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

/**
 * Robust version with Dynamic Header Detection and Debug Logging.
 */
public class DocumentParser {
    private static final String delimiter = ";";

    public ParsedDocument parse(List<String> rawLines, String fileName) {
        writeDebug(rawLines, fileName);
        ParsedDocument doc = new ParsedDocument(fileName);
        int sequenceCounter = 0;
        int tableCounter = 1;

        List<String[]> tableRowsBuffer = new ArrayList<>();
        List<String> textLinesBuffer = new ArrayList<>();

        for (String line : rawLines) {
            String trimmed = (line != null) ? line.trim() : "";
            if (trimmed.isEmpty()) {
                flushBuffers(doc, sequenceCounter++, tableCounter++, tableRowsBuffer, textLinesBuffer);
                tableRowsBuffer.clear();
                textLinesBuffer.clear();
                continue;
            }

            boolean hasDelimiter = line.contains(delimiter);
            boolean isHeuristicRow = trimmed.matches("^\\d+([\\s;\\:].*)"); 

            if (hasDelimiter || (isHeuristicRow && !tableRowsBuffer.isEmpty())) {
                if (tableRowsBuffer.isEmpty() && !textLinesBuffer.isEmpty()) {
                    String lastText = textLinesBuffer.get(textLinesBuffer.size() - 1);
                    if (isLikelyHeader(lastText)) {
                        tableRowsBuffer.add(lastText.split("\\s{2,}|;", -1)); 
                        textLinesBuffer.remove(textLinesBuffer.size() - 1);
                    }
                }

                if (!textLinesBuffer.isEmpty()) {
                    doc.addElement(createText(sequenceCounter++, textLinesBuffer));
                    textLinesBuffer.clear();
                }
                
                if (hasDelimiter) {
                    tableRowsBuffer.add(line.split(delimiter, -1));
                } else {
                    tableRowsBuffer.add(line.split("\\s{2,}", -1));
                }
            } else {
                if (!tableRowsBuffer.isEmpty()) {
                    flushBuffers(doc, sequenceCounter++, tableCounter++, tableRowsBuffer, textLinesBuffer);
                    tableRowsBuffer.clear();
                }
                textLinesBuffer.add(line);
            }
        }
        
        flushBuffers(doc, sequenceCounter++, tableCounter++, tableRowsBuffer, textLinesBuffer);
        return consolidateTables(doc);
    }

    private void writeDebug(List<String> rawLines, String fileName) {
        try {
            File debugDir = new File("debug_logs");
            if (!debugDir.exists()) debugDir.mkdirs();
            File debugFile = new File(debugDir, "parsing_debug.txt");
            try (PrintWriter writer = new PrintWriter(new FileWriter(debugFile, true))) {
                writer.println("\n--- NEW PARSE SESSION: " + fileName + " (" + new Date() + ") ---");
                for (String line : rawLines) writer.println(line);
                writer.println("----------------------------------------------------------");
            }
        } catch (Exception e) {}
    }

    private boolean isLikelyHeader(String line) {
        String l = line.toLowerCase();
        return l.contains("nom") || l.contains("prénom") || l.contains("prenom") || l.contains("matricule") || l.contains("moyenne");
    }

    private void flushBuffers(ParsedDocument doc, int seq, int tableNum, List<String[]> tableRows, List<String> textLines) {
        if (!tableRows.isEmpty()) doc.addElement(createTable(seq, tableNum, tableRows));
        if (!textLines.isEmpty()) doc.addElement(createText(seq, textLines));
    }

    private ParsedDocument consolidateTables(ParsedDocument original) {
        ParsedDocument doc = new ParsedDocument(original.getFileName());
        TableChunk lastTable = null;

        for (DocumentElement element : original.getElements()) {
            if (element instanceof TableChunk currentTable) {
                if (lastTable != null && shouldMerge(lastTable, currentTable)) {
                    lastTable.getRows().addAll(currentTable.getRows());
                } else {
                    doc.addElement(currentTable);
                    lastTable = currentTable;
                }
            } else if (element instanceof TextChunk tc) {
                if (lastTable != null && tc.getLines().size() <= 2) {
                     doc.addElement(tc);
                } else {
                    doc.addElement(tc);
                    lastTable = null; 
                }
            }
        }
        return doc;
    }

    private boolean shouldMerge(TableChunk last, TableChunk current) {
        try {
            int lastId = extractLastId(last);
            if (lastId == -1) return false;
            if (containsId(current.getHeaders(), lastId + 1)) return true;
            for (String[] row : current.getRows()) {
                if (containsId(row, lastId + 1)) return true;
            }
        } catch (Exception e) {}
        return false;
    }

    private int extractLastId(TableChunk table) {
        if (table.getRows().isEmpty()) return -1;
        String[] lastRow = table.getRows().get(table.getRows().size() - 1);
        for (int i = 0; i < Math.min(3, lastRow.length); i++) {
            if (i >= lastRow.length) break;
            String val = lastRow[i].replaceAll("[^0-9]", "");
            if (!val.isEmpty()) return Integer.parseInt(val);
        }
        return -1;
    }

    private boolean containsId(String[] row, int targetId) {
        for (int i = 0; i < Math.min(5, row.length); i++) {
            if (i >= row.length) break;
            String val = row[i].replaceAll("[^0-9]", "").trim();
            if (!val.isEmpty()) {
                try {
                   if (Integer.parseInt(val) == targetId) return true;
                } catch (NumberFormatException e) {}
            }
        }
        return false;
    }

    private TableChunk createTable(int seqIndex, int tableNum, List<String[]> allRows) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (String[] row : allRows) {
            counts.put(row.length, counts.getOrDefault(row.length, 0) + 1);
        }
        int dominantCount = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);

        int anchorIndex = findHeaderAnchor(allRows);
        String[] headers;
        int dataOffset;

        if (anchorIndex != -1) {
            if (anchorIndex > 0 && isParentHeader(allRows.get(anchorIndex - 1))) {
                headers = mergeHeaders(allRows.get(anchorIndex - 1), allRows.get(anchorIndex));
                dataOffset = anchorIndex + 1;
            } else {
                headers = allRows.get(anchorIndex);
                dataOffset = anchorIndex + 1;
            }
        } else {
            headers = allRows.get(0);
            dataOffset = 1;
        }

        if (headers.length < dominantCount) {
            String[] padded = new String[dominantCount];
            System.arraycopy(headers, 0, padded, 0, headers.length);
            for (int i = headers.length; i < dominantCount; i++) padded[i] = "";
            headers = padded;
        }

        List<String[]> dataRows = new ArrayList<>();
        for (int i = dataOffset; i < allRows.size(); i++) {
            String[] row = allRows.get(i);
            boolean isStudentRow = row.length > 0 && row[0].trim().matches("\\d+");
            if (isStudentRow || row.length >= dominantCount - 2) {
                if (row.length < dominantCount) {
                    String[] padded = new String[dominantCount];
                    System.arraycopy(row, 0, padded, 0, row.length);
                    for (int j=row.length; j<dominantCount; j++) padded[j] = "";
                    row = padded;
                }
                dataRows.add(row);
            }
        }
        return new TableChunk(seqIndex, "TABLE_" + tableNum, headers, dataRows);
    }

    private int findHeaderAnchor(List<String[]> rows) {
        String[] keywords = {"nom", "prenom", "prénom", "moy", "déc", "dec", "matricule", "id", "module", "ue", "crédit", "résultat"};
        for (int i = 0; i < Math.min(15, rows.size()); i++) {
            String line = String.join(" ", rows.get(i)).toLowerCase();
            for (String k : keywords) {
                if (line.contains(k)) return i;
            }
        }
        return -1;
    }

    private boolean isParentHeader(String[] row) {
        int filledCount = 0;
        int totalLength = 0;
        for (String s : row) {
            String t = s.trim();
            if (!t.isEmpty()) {
                filledCount++;
                totalLength += t.length();
            }
        }
        
        // If it's just one giant string (like University Name), it's a TITLE, not a header
        if (filledCount == 1 && totalLength > 40) return false;

        int emptyCount = 0;
        for (String s : row) if (s.trim().isEmpty()) emptyCount++;
        
        // Parent headers should have at least 2 categories or be highly structured
        return !row[0].matches("\\d+") && (filledCount >= 2 || (emptyCount > 0 && totalLength > 15));
    }

    private String[] mergeHeaders(String[] r1, String[] r2) {
        int len = Math.max(r1.length, r2.length);
        String[] merged = new String[len];
        String parent = "";
        for (int i = 0; i < len; i++) {
            // Only carry over parent if it's explicitly set or we are in a merged block
            if (i < r1.length && !r1[i].trim().isEmpty()) {
                parent = r1[i].trim();
            }
            
            String child = (i < r2.length) ? r2[i].trim() : "";
            
            if (parent.isEmpty() || parent.equalsIgnoreCase(child)) {
                merged[i] = child;
            } else if (child.isEmpty()) {
                merged[i] = parent;
            } else {
                merged[i] = parent + " / " + child;
            }
        }
        return merged;
    }

    private TextChunk createText(int seqIndex, List<String> lines) {
        return new TextChunk(seqIndex, new ArrayList<>(lines));
    }
}