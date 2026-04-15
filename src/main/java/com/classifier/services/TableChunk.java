package com.classifier.services;
import java.util.*;
public class TableChunk extends DocumentElement{
	private String tableID;
	private String[] headers;
	private List<String[]> rows;
	private boolean isSelected = false;
	private int sortColumnIndex = -1;
	private boolean isAscending = false;

	public TableChunk(int sequenceIndex, String tableID, String[] headers, List<String[]> rows){
		super(sequenceIndex);
		this.tableID = tableID;
		this.headers = headers;
		this.rows = rows;
	}
	@Override
	public void render(PdfHandler handler){
		handler.addTable(this.getHeaders(), this.getRows());
	}
	public String getTableID(){
		return tableID;
	}
	public String[] getHeaders(){
		return headers;
	}
	public List<String[]> getRows(){
		return rows;
	}
	public void setSelected(boolean selected){
		this.isSelected = selected;
	}
	public void setSortColumnIndex(int index){
		this.sortColumnIndex = index;
	}
	public void setAscending(boolean ascending) {
		this.isAscending = ascending;
	}
	public void sortData() {
	    if (sortColumnIndex == -1 || rows == null || rows.isEmpty()) return;
	    List<RowWrapper> wrappers = new ArrayList<>();
	    for (String[] row : rows) {
	        String val = (sortColumnIndex < row.length) ? row[sortColumnIndex].trim() : "";
	        wrappers.add(new RowWrapper(row, val));
	    }
	    wrappers.sort((w1, w2) -> {
	        // Empty values always go to the bottom
	        if (w1.isEmpty && !w2.isEmpty) return 1;
	        if (!w1.isEmpty && w2.isEmpty) return -1;
	        if (w1.isEmpty && w2.isEmpty) return 0;

	        int result;
	        if (w1.isNumeric && w2.isNumeric) {
	            result = Double.compare(w1.numericValue, w2.numericValue);
	        } else {
	            result = w1.stringValue.compareToIgnoreCase(w2.stringValue);
	        }

	        return isAscending ? result : (result * -1);
	    });
	    for (int i = 0; i < rows.size(); i++) {
	        rows.set(i, wrappers.get(i).originalRow);
	    }
	}
}