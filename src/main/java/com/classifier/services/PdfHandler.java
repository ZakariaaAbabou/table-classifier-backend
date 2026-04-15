package com.classifier.services;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import java.io.FileOutputStream;
import java.util.List;

public class PdfHandler {

    private Document document;
    private String outputPath;

    public PdfHandler(String outputPath) {
        this.outputPath = outputPath;
    }

    public void generatePdf(ParsedDocument parsedDoc) {
        try {
            PdfWriter writer = new PdfWriter(new FileOutputStream(outputPath));
            PdfDocument pdf = new PdfDocument(writer);
            this.document = new Document(pdf);
            for (DocumentElement element : parsedDoc.getElements()) {
                element.render(this);
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addText(List<String> lines) {
        for (String line : lines) {
            document.add(new Paragraph(line));
        }
    }

    public void addTable(String[] headers, List<String[]> rows) {
        Table pdfTable = new Table(UnitValue.createPercentArray(headers.length)).useAllAvailableWidth();

        for (String header : headers) {
            pdfTable.addHeaderCell(new Paragraph(header).setBold());
        }

        for (String[] row : rows) {
            for (String cell : row) {
                pdfTable.addCell(new Paragraph(cell));
            }
        }

        document.add(pdfTable);
    }
}