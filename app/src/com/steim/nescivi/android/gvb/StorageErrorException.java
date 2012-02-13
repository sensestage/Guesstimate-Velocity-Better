package com.steim.nescivi.android.gvb;

public class StorageErrorException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8803701895510628732L;
	String message;
		
	public StorageErrorException(String message) {
		this.message = message;
	}
		
	@Override
	public String getMessage() {
		return message;
	}
		
	@Override
	public String getLocalizedMessage() {
		return message;
	}
}
