package org.ttrssreader.net;

public class NotLoggedInException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    public NotLoggedInException(String msg) {
        super(msg);
    }
    
    public NotLoggedInException(String format, Object... args) {
        super(String.format(format, args));
    }
    
    public NotLoggedInException(final String message, final Throwable cause) {
        super(message, cause);
    }
    
    public NotLoggedInException(final Throwable cause) {
        super(cause);
    }
    
    public NotLoggedInException(final Throwable cause, final String format, final Object... args) {
        super(String.format(format, args), cause);
    }
}
