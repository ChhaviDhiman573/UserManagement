package com.wellness.exception;

public class UserNotRegisteredException extends RuntimeException{
	public UserNotRegisteredException(String msg){
		super(msg);
	}
}
