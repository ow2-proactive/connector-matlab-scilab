package org.ow2.proactive.scheduler.ext.matsci.common.exception;

/**
 * MatSciTaskException
 *
 * @author The ProActive Team
 */
public class MatSciTaskException extends RuntimeException {

    private static final long serialVersionUID = 10L;

    public MatSciTaskException() {
        super();
    }

    public MatSciTaskException(String message) {
        super(message);
    }

    public MatSciTaskException(String message, Throwable cause) {
        super(message, cause);
    }
}
