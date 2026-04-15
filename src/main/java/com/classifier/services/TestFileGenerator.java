package com.classifier.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A utility to generate difficult test cases for document classification and extraction.
 */
public class TestFileGenerator {

    public static void main(String[] args) {
        String baseDir = "C:\\Users\\HP\\Desktop\\TableTests";
        File dir = new File(baseDir);
        if (!dir.exists()) dir.mkdirs();

        try {
            generateExcel(baseDir + "\\finance_merged.xlsx");
            generateWord(baseDir + "\\inventory_mixed.docx");
            generatePDF(baseDir + "\\complex_deliberation.pdf");
            generateMultiStructurePDF(baseDir + "\\multi_structure.pdf");
            System.out.println("[GEN-DONE] All test files generated in: " + baseDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void generateExcel(String path) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Financials");

            // Table 1: Merged Headers
            Row r1 = sheet.createRow(0);
            r1.createCell(0).setCellValue("ID");
            r1.createCell(1).setCellValue("NOM ET PRENOM");
            r1.createCell(2).setCellValue("MODULE M231");
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 2, 3)); // Merge for Category

            Row r2 = sheet.createRow(1);
            r2.createCell(2).setCellValue("Note");
            r2.createCell(3).setCellValue("Résultat");

            // Data rows
            String[][] data = {
                {"1", "AADIL ROMAISSA", "12,38", "V"},
                {"2", "ABBASSI HAFSA", "", "NV"}, // Blank value
                {"3", "ABDI SIDINA", "15,50", "V"}
            };
            for (int i=0; i<data.length; i++) {
                Row dr = sheet.createRow(2+i);
                dr.createCell(0).setCellValue(data[i][0]);
                dr.createCell(1).setCellValue(data[i][1]);
                dr.createCell(2).setCellValue(data[i][2]);
                dr.createCell(3).setCellValue(data[i][3]);
            }

            try (FileOutputStream out = new FileOutputStream(path)) {
                workbook.write(out);
            }
        }
    }

    private static void generateWord(String path) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p1 = doc.createParagraph();
            p1.createRun().setText("This document contains an inventory table separated by text paragraphs.");

            XWPFTable table = doc.createTable(4, 3);
            table.getRow(0).getCell(0).setText("Item ID");
            table.getRow(0).getCell(1).setText("Description");
            table.getRow(0).getCell(2).setText("Quantity");

            table.getRow(1).getCell(0).setText("A-101");
            table.getRow(1).getCell(1).setText("Computer Screen");
            table.getRow(1).getCell(2).setText("45");

            table.getRow(2).getCell(0).setText(""); // Blank ID
            table.getRow(2).getCell(1).setText("Keyboard / Mouse");
            table.getRow(2).getCell(2).setText("12");

            doc.createParagraph().createRun().setText("End of first table. Below is another one.");

            XWPFTable table2 = doc.createTable(3, 2);
            table2.getRow(0).getCell(0).setText("Location");
            table2.getRow(0).getCell(1).setText("Status");
            table2.getRow(1).getCell(0).setText("Warehouse A");
            table2.getRow(1).getCell(1).setText("Full");

            try (FileOutputStream out = new FileOutputStream(path)) {
                doc.write(out);
            }
        }
    }

    private static void generatePDF(String path) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int p=1; p<=2; p++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    cs.setLeading(15f);
                    cs.newLineAtOffset(50, 750);
                    cs.showText("Résultat Délibération - Page " + p);
                    cs.newLine();
                    cs.newLine();
                    
                    // Table Header Row 1
                    cs.showText("ID       Nom & Prenom            Moyenne      Resultat");
                    cs.newLine();
                    cs.showText("-------------------------------------------------------");
                    cs.newLine();
                    
                    int startId = (p-1)*15 + 1;
                    for (int i=0; i<15; i++) {
                        int id = startId + i;
                        String grade = (10 + (i%5)) + ",500";
                        cs.showText(id + "      ETUDIANT " + id + "             " + grade + "       V");
                        cs.newLine();
                    }
                    cs.endText();
                }
            }
            doc.save(path);
        }
    }

    private static void generateMultiStructurePDF(String path) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setLeading(12f);
                cs.newLineAtOffset(50, 750);
                
                cs.showText("Table 1 (3 Columns)");
                cs.newLine();
                cs.showText("ID ; Name ; Status");
                cs.newLine();
                cs.showText("1 ; Alpha ; OK");
                cs.newLine();
                
                cs.newLine();
                cs.newLine();
                cs.showText("Table 2 (6 Columns - Dense)");
                cs.newLine();
                cs.showText("ID ; Nom ; M1 ; M2 ; M3 ; Moy");
                cs.newLine();
                cs.showText("101 ; Student A ; 12 ; 14 ; 10 ; 12,00");
                cs.newLine();
                
                cs.endText();
            }
            doc.save(path);
        }
    }
}
