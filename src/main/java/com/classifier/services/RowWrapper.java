package com.classifier.services;
public class RowWrapper {
    public String[] originalRow;
    public String stringValue;
    public Double numericValue = null;
    boolean isNumeric = false;
    boolean isEmpty;

    public RowWrapper(String[] row, String val) {
        this.originalRow = row;
        this.stringValue = val;
        this.isEmpty = val.isEmpty();
        if (!isEmpty) {
            try {
                // Handle European/French style commas (e.g. 12,380 -> 12.380)
                String normalizedVal = val.replace(",", ".");
                this.numericValue = Double.parseDouble(normalizedVal);
                this.isNumeric = true;
            } catch (NumberFormatException e) {
                this.isNumeric = false;
            }
        }
    }
}
