package com.chexiang.venus.demo.provider;

import com.meidusa.venus.annotations.RemoteException;
import com.meidusa.venus.annotations.RemoteException.Level;
import com.meidusa.venus.exception.AbstractVenusException;
import com.meidusa.venus.exception.VenusExceptionCodeConstant;

@RemoteException(errorCode = 200001, level = Level.ERROR)
public class HelloValidException extends Exception {

	private static final long serialVersionUID = 1L;


	public HelloValidException(int errorCode, String msg) {
		super(msg);
	}

	public HelloValidException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

	public HelloValidException(int errorCode, String msg, Throwable throwable) {
		super(msg, throwable);
	}

	/*
	@Override
	public int getErrorCode() {
		return 200001;
	}
	*/

}
