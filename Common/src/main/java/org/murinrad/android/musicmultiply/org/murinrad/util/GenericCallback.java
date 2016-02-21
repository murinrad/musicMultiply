package org.murinrad.android.musicmultiply.org.murinrad.util;

import android.os.Bundle;

/**
 * Created by Rado on 21.6.2015.
 */
public abstract class GenericCallback {

    private static final String MESSAGE_TAG = "CALLBACK_MSG";
    private static final String SUCCESS_TAG = "SUCCESS_TAG";

    Bundle data = new Bundle();

    public abstract void onCallback();

    protected String getMessage() {
        return data.getString(MESSAGE_TAG);
    }

    public void setMessage(String message) {
        data.putString(MESSAGE_TAG, message);
    }

    protected boolean getSuccess() {
        return data.getBoolean(SUCCESS_TAG, false);
    }

    public void setSuccess(Boolean status) {
        data.putBoolean(SUCCESS_TAG, status);
    }


}
