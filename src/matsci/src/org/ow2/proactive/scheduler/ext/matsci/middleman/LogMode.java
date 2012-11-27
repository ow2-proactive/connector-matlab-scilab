package org.ow2.proactive.scheduler.ext.matsci.middleman;

/**
 * LogMode defines for each logging attempt in the MiddlemanJVM, where the log will be printed
 *
 * @author The ProActive Team
 */
public enum LogMode {
    /**
     * In this mode the log is printed only if the debug mode is activated (and it's printed on both the STDOUT and the logfile)
     */
    STD,
    /**
     * In this mode the log is printed only in the log file and only if the debug mode is activated
     */
    FILEONLY,
    /**
     * In this mode the log is always printed in the log file and it's printed on the STDOUT if in debug
     */
    FILEALWAYS,
    /**
     * In this mode the log is always printed in the log file, and never on the STDOUT
     */
    FILEALWAYSNEVEROUT,
    /**
     * In this mode the log is printed only in the STDOUT and only if the debug mode is activated
     */
    OUTONLY,
    /**
     * In this mode the log is always printed in the STDOUT and it's printed on the log file if in debug
     */
    OUTAWAYS,
    /**
     * In this mode the log is always printed in the STDOUT, and never on the log file
     */
    OUTAWAYSNEVERFILE,
    /**
     * In this mode the log is always printed in both the STDOUT and the logfile
     */
    FILEANDOUTALWAYS
}
