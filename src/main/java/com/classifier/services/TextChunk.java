package com.classifier.services;
import java.util.*;
public class TextChunk extends DocumentElement{
	private List<String> lines;
	public TextChunk(int sequenceIndex, List<String> lines){
		super(sequenceIndex);
		this.lines = lines;
	}

	@Override
	public void render(PdfHandler handler){
		handler.addText(this.getLines());
	}
	public List<String> getLines(){
		return lines;
	}
}