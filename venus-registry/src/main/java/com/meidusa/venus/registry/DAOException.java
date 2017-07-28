package com.meidusa.venus.registry;

public class DAOException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public DAOException(String msg) {
		super(msg);
	}

	public DAOException() {
	}

	public DAOException(Throwable throwable) {
		super(throwable);
	}

	public DAOException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

}
