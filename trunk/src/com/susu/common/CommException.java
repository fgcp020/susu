package com.susu.common;

public class CommException extends Exception {
	private static final long serialVersionUID = -289434153301838849L;
	
	private String errorCode;
	
	public CommException(String errorCode,String exception){
		super(exception);
		this.errorCode=errorCode;
	}
	public String getErrorCode() {
		return errorCode;
	}
		
}
