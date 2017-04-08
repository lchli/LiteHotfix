package com.lchli.litehotfix;

/**
 * Created by lichenghang on 2017/4/8.
 */

public class HotFixException extends RuntimeException {

    public HotFixException() {
    }

    public HotFixException(String message) {
        super(message);
    }

    public HotFixException(String message, Throwable cause) {
        super(message, cause);
    }

    public HotFixException(Throwable cause) {
        super(cause);
    }


}
