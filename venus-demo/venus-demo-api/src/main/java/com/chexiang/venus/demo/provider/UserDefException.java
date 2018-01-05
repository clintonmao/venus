package com.chexiang.venus.demo.provider;

import com.meidusa.venus.annotations.RemoteException;
import com.meidusa.venus.annotations.RemoteException.Level;
import com.meidusa.venus.exception.AbstractVenusException;
import com.meidusa.venus.exception.VenusExceptionCodeConstant;

@RemoteException(errorCode = 200001, level = Level.ERROR)
public class UserDefException extends Exception {

	private static final long serialVersionUID = 1L;


	public UserDefException(int errorCode, String msg) {
		super(msg);
	}

	public UserDefException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

	public UserDefException(int errorCode, String msg, Throwable throwable) {
		super(msg, throwable);
	}

//	@Override
//	public int getErrorCode() {
//		return 200001;
//	}

}
