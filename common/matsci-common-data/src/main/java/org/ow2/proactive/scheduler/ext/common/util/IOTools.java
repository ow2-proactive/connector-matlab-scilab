/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s): ActiveEon Team - http://www.activeeon.com
 *
 * ################################################################
 * $$ACTIVEEON_CONTRIBUTOR$$
 */
package org.ow2.proactive.scheduler.ext.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


/**
 * Utility class which performs some IO work
 *
 * @author The ProActive Team
 */
public class IOTools {

    protected static final SimpleDateFormat ISO8601FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:sss");

    public static String generateHash(File file) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        if (!file.exists() || !file.canRead()) {
            throw new IOException("File doesn't exist : " + file);
        }
        MessageDigest md = MessageDigest.getInstance("SHA"); // SHA or MD5
        String hash = "";

        byte[] data = new byte[(int) file.length()];
        FileInputStream fis = new FileInputStream(file);
        fis.read(data);
        fis.close();

        md.update(data); // Reads it all at one go. Might be better to chunk it.

        byte[] digest = md.digest();

        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(digest[i]);
            if (hex.length() == 1)
                hex = "0" + hex;
            hex = hex.substring(hex.length() - 2);
            hash += hex;
        }

        return hash;
    }

    public static String generateHash(String bigString)
            throws NoSuchAlgorithmException, FileNotFoundException, IOException {

        MessageDigest md = MessageDigest.getInstance("SHA"); // SHA or MD5
        String hash = "";

        char[] data = new char[bigString.length()];
        StringReader fis = new StringReader(bigString);
        fis.read(data);
        fis.close();

        byte[] input = toByteArray(data);
        md.update(input); // Reads it all at one go. Might be better to chunk it.

        byte[] digest = md.digest();

        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(digest[i]);
            if (hex.length() == 1)
                hex = "0" + hex;
            hex = hex.substring(hex.length() - 2);
            hash += hex;
        }

        return hash;
    }

    public static byte[] toByteArray(char[] array) {
        return toByteArray(array, Charset.defaultCharset());
    }

    public static byte[] toByteArray(char[] array, Charset charset) {
        CharBuffer cbuf = CharBuffer.wrap(array);
        ByteBuffer bbuf = charset.encode(cbuf);
        return bbuf.array();
    }

    /**
     * An utility class (Thread) which collects the output from a process and prints it on the JVM's standard output
     *
     * @author The ProActive Team
     */
    public static class LoggingThread implements Runnable, Serializable {

        private String appendMessage;

        private PrintStream outputStream;

        private BufferedReader inputReader;

        private String startpattern;

        private String stoppattern;

        private String[] patternsToFind;

        private HashMap<String, Boolean> patternFound = new HashMap<String, Boolean>();

        private static String HOSTNAME;

        static {
            try {
                HOSTNAME = java.net.InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
            }
        }

        public ArrayList<String> output = new ArrayList<String>();

        public LoggingThread(InputStream is, String appendMessage, PrintStream outputStream) {
            this(is, appendMessage, outputStream, null, null, null);
        }

        public LoggingThread(InputStream is, String appendMessage, PrintStream outputStream, String startpattern,
                             String stoppattern, String[] patternsToFind) {
            this.inputReader = new BufferedReader(new InputStreamReader(is));
            this.appendMessage = appendMessage;
            this.outputStream = outputStream;
            this.startpattern = startpattern;
            this.stoppattern = stoppattern;
            this.patternsToFind = patternsToFind;
            if (this.patternsToFind != null) {
                for (String p : this.patternsToFind) {
                    this.patternFound.put(p, false);
                }
            }
        }

        public boolean patternFound(String pattern) {
            return patternFound.get(pattern);
        }

        /**
         * @see Runnable#run()
         */
        public void run() {

            String line;
            try {

                // we eat everything until startpattern if provided
                if (this.startpattern != null) {
                    while ((line = waitForLine()) != null && !line.contains(this.startpattern));
                }
                // now we print all lines until stoppattern if provided
                while ((line = waitForLine()) != null &&
                        (this.stoppattern == null || (this.stoppattern != null && !line.contains(this.stoppattern)))) {
                    printLine(line);

                    // patterns detection
                    if (this.patternsToFind != null) {
                        for (String p : this.patternsToFind) {
                            if (line.contains(p)) {
                                this.patternFound.put(p, true);
                            }
                        }
                    }
                }

                try {
                    this.inputReader.close();
                } catch (IOException e) {
                    // SCHEDULING-1296 not necessary to print the Exception but we need to try catch blocks
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String waitForLine() throws IOException {
            try {
                while (!inputReader.ready()) {
                    Thread.sleep(10);
                }
                return inputReader.readLine();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        private void printLine(String line) {
            this.outputStream.println("[" + ISO8601FORMAT.format(new Date()) + " " + HOSTNAME + "]" + appendMessage +
                    line);
        }

    }
}
