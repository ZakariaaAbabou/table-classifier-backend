package com.classifier.services;
public abstract class DocumentElement{
	protected int sequenceIndex;
	public DocumentElement(int sequenceIndex){
		this.sequenceIndex = sequenceIndex;
	}
	public abstract void render(PdfHandler handler);
	public int getSequenceIndex(){
		return sequenceIndex;
	}
}