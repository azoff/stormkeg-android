package com.stormpath.kegbot;

import org.kegbot.backend.BackendException;

/**
 * Created by Azoff on 3/26/15.
 */
public class StormpathApiException extends BackendException {

	public StormpathApiException(String detailMessage) {
		super(detailMessage);
	}

	public StormpathApiException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
}
