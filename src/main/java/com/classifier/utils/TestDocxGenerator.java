package com.classifier.utils;

import org.apache.poi.xwpf.usermodel.*;
import java.io.FileOutputStream;
import java.io.File;

public class TestDocxGenerator {
    public static void main(String[] args) {
        XWPFDocument document = new XWPFDocument();
        
        // Add some text
        XWPFParagraph para1 = document.createParagraph();
        XWPFRun run1 = para1.createRun();
        run1.setText("This is a test document to verify DOCX table detection.");
        
        // Add a table
        XWPFTable table = document.createTable(3, 3); // 3 rows, 3 columns
        
        // Fill table with data
        table.getRow(0).getCell(0).setText("Header A");
        table.getRow(0).getCell(1).setText("Header B");
        table.getRow(0).getCell(2).setText("Header C");
        
        table.getRow(1).getCell(0).setText("Data 1A");
        table.getRow(1).getCell(1).setText("Data 1B");
        table.getRow(1).getCell(2).setText("Data 1C");
        
        table.getRow(2).getCell(0).setText("Data 2A");
        table.getRow(2).getCell(1).setText("Data 2B");
        table.getRow(2).getCell(2).setText("Data 2C");
        
        // Add more text below
        XWPFParagraph para2 = document.createParagraph();
        XWPFRun run2 = para2.createRun();
        run2.setText("End of test document.");

        try {
            File outputFile = new File("test_table.docx");
            FileOutputStream out = new FileOutputStream(outputFile);
            document.write(out);
            out.close();
            document.close();
            System.out.println("Sussess! Generated: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
