package com.classifier.services;
import java.util.ArrayList;
import java.util.List;

public class ParsedDocument {
    private String fileName;
    private List<DocumentElement> elements = new ArrayList<>();
    private String category = "UNKNOWN";
    public ParsedDocument(String fileName) {
        this.fileName = fileName;
    }
    public void addElement(DocumentElement element) {
        this.elements.add(element);
    }
    public List<DocumentElement> getElements() { return elements; }
    public String getFileName() { return fileName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}