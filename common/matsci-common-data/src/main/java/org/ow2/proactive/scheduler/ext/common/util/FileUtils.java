/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive.scheduler.ext.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


/**
 * Contains various utility methods for file manipulations.
 *
 * @author The ProActive Team
 */
public final class FileUtils {

    /**
     * Uncompresses the zip archive into the specified directory.
     *
     * @param zipFile contains the archive to unzip
     * @param destDir the destination directory that will contain unzipped files
     * @return Returns true if the unzip was successful, false otherwise
     */
    public static boolean unzip(final File zipFile, final File destDir) {
        final String destDirAbsPath = destDir.getAbsolutePath() + File.separatorChar;
        final byte[] buf = new byte[2048];
        ZipInputStream ziStream = null;
        FileInputStream fiStream = null;

        try {
            fiStream = new FileInputStream(zipFile);
            ziStream = new ZipInputStream(fiStream);

            ZipEntry zipEntry = ziStream.getNextEntry();

            // Cycle through all entries and write them into the dest dir
            while (zipEntry != null) {
                File absPath = new File(destDirAbsPath + zipEntry.getName());
                if (!zipEntry.isDirectory()) {
                    String absPathName = absPath.getAbsolutePath();
                    int endIndex = absPathName.lastIndexOf(File.separatorChar + absPath.getName());
                    String absDirName = absPathName.substring(0, endIndex);
                    File absDir = new File(absDirName);
                    absDir.mkdirs();
                    FileOutputStream fos = new FileOutputStream(absPath);
                    try {
                        int len;
                        while ((len = ziStream.read(buf)) > 0) {
                            fos.write(buf, 0, len);
                        }
                    } finally {
                        fos.close();
                    }
                }
                zipEntry = ziStream.getNextEntry();
            }

            // Close the input stream
            try {
                ziStream.close();
            } catch (Exception e) {

            }
            try {
                fiStream.close();
            } catch (Exception e) {

            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Uncompresses the zip archive into the specified directory.
     *
     * @param zipFile contains the archive to unzip
     * @param destDir the destination directory that will contain unzipped files
     * @return Returns true if the unzip was successful, false otherwise
     */
    public static boolean unzip(final String zipFile, final String destDir) {
        return unzip(new File(zipFile), new File(destDir));
    }

    /**
     * compresses the specified files into a zip archive
     *
     * @param zipFile contains the zip archive name
     * @param files   files to put in the archive
     * @return Returns true if the zip was successful, false otherwise
     */
    public static boolean zip(final File zipFile, final File[] files) {
        ZipOutputStream out = null;
        try {
            byte[] buffer = new byte[4096]; // Create a buffer for copying
            int bytesRead;

            out = new ZipOutputStream(new FileOutputStream(zipFile));

            for (File f : files) {
                FileInputStream in = null;
                try {
                    if (f.isDirectory())
                        continue;//Ignore directory
                    in = new FileInputStream(f); // Stream to read file
                    ZipEntry entry = new ZipEntry(f.getPath()); // Make a ZipEntry
                    out.putNextEntry(entry); // Store entry
                    while ((bytesRead = in.read(buffer)) != -1)
                        out.write(buffer, 0, bytesRead);
                    in.close();
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {

                        }
                    }
                }
            }
            out.close();
        } catch (Exception e) {
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {

                }
            }
        }
        return true;
    }

    /**
     * compresses the specified files into a zip archive
     *
     * @param zipFile contains the zip archive name
     * @param files   files to put in the archive
     * @return Returns true if the zip was successful, false otherwise
     */
    public static boolean zip(final String zipFile, final String[] files) {
        File[] ffiles = new File[files.length];
        for (int i = 0; i < files.length; i++) {
            ffiles[i] = new File(files[i]);
        }
        return zip(new File(zipFile), ffiles);
    }

    public static void deleteDirectory(String dirPath) {
        File dir = new File(dirPath);
        File[] currList;
        Stack<File> stack = new Stack<File>();
        stack.push(dir);
        while (!stack.isEmpty()) {
            if (stack.lastElement().isDirectory()) {
                currList = stack.lastElement().listFiles();
                if (currList.length > 0) {
                    for (File curr : currList) {
                        stack.push(curr);
                    }
                } else {
                    stack.pop().delete();
                }
            } else {
                stack.pop().delete();
            }
        }
    }
}
