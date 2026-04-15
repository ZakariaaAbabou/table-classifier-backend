package com.classifier.services;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.io.StringWriter;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * FileConverter stripped of Image/OCR support.
 * Focuses exclusively on Excel, PDF, and Word documents.
 */
public class FileConverter {
    private static final String delimiter = ";";

    public List<String> convertToCSV(File file) throws IllegalArgumentException {
        String filename = file.getName().toLowerCase().trim();
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex <= 0 || dotIndex == filename.length() - 1) {
            throw new IllegalArgumentException("Invalid file format or missing extension: " + filename);
        }
        String ext = filename.substring(dotIndex + 1);
        switch (ext) {
            case "xlsx": case "xls":  return handleExcel(file);
            case "docx":              return handleWord(file);
            case "pdf":               return handlePDF(file);
            case "txt": case "csv": case "log": return handleRawText(file);
            default: throw new IllegalArgumentException("Format '." + ext + "' is not supported. Please try converting your file to"
                    + " (pdf, xlsx, xls, docx, txt, csv, log)");
        }
    }

    private List<String> handleExcel(File file) {
        List<String> lines = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {
            DataFormatter formatter = new DataFormatter();
            for (Sheet sheet : workbook) {
                lines.add("##" + sheet.getSheetName() + ":");
                
                int maxCols = 0;
                for (Row row : sheet) {
                    if (row.getLastCellNum() > maxCols) {
                        maxCols = row.getLastCellNum();
                    }
                }

                for (Row row : sheet) {
                    List<String> cellValues = new ArrayList<>();
                    for (int i = 0; i < maxCols; i++) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        cellValues.add(sanitize(formatter.formatCellValue(cell)));
                    }
                    lines.add(String.join(delimiter, cellValues));
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return lines;
    }

    private List<String> handleWord(File file) {
        List<String> lines = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis)) {
            
            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph para) {
                    String text = para.getText();
                    if (text != null && !text.isEmpty()) {
                        lines.add(sanitize(text)); 
                    }
                } else if (element instanceof XWPFTable table) {
                    for (XWPFTableRow row : table.getRows()) {
                        List<String> cellValues = new ArrayList<>();
                        for (XWPFTableCell cell : row.getTableCells()) {
                            cellValues.add(sanitize(cell.getText()));
                        }
                        lines.add(String.join(delimiter, cellValues));
                    }
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return lines;
    }

    private List<String> handlePDF(File file) {
        List<String> lines = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(file)) {
            PDFColumnStripper stripper = new PDFColumnStripper(delimiter);
            stripper.setSortByPosition(true);
            stripper.writeText(doc, new StringWriter()); 

            String result = stripper.getCSVResult();
            if (result != null) {
                for (String line : result.split("\\n")) {
                    if (!line.trim().isEmpty()) {
                        lines.add(line.trim());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private List<String> handleRawText(File file) {
        List<String> lines = new ArrayList<>();
        try {    
            lines = java.nio.file.Files.readAllLines(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private String sanitize(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\r\\n\\t]", " ").trim();
    }
}