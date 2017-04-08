package com.lchli.litehotfix.util;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static android.content.ContentValues.TAG;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Created by lichenghang on 2017/3/22.
 */

public class FixUtils {

    private static final String DEX_PREFIX = "classes";
    private static final String DEX_SUFFIX = ".dex";

    private static final String EXTRACTED_NAME_EXT = ".classes";
    private static final String EXTRACTED_SUFFIX = ".zip";
    private static final int MAX_EXTRACT_ATTEMPTS = 3;


    /**
     * @param sourceApk
     * @param dexDir    dest dir extract to.
     * @return
     * @throws IOException
     */
    public static List<File> performExtractions(File sourceApk, File dexDir) throws IOException {
        /**delete old dexs.*/
        List<File> oldDexFiles = getMutiDexs(dexDir);
        if (oldDexFiles != null) {
            for (File dex : oldDexFiles) {
                dex.delete();
            }
        }

        final String extractedFilePrefix = sourceApk.getName() + EXTRACTED_NAME_EXT;

        List<File> files = new ArrayList<File>();


        final ZipFile apk = new ZipFile(sourceApk);

        try {

            int secondaryNumber = 2;

            ZipEntry dexFile = apk.getEntry(DEX_PREFIX + secondaryNumber + DEX_SUFFIX);

            while (dexFile != null) {

                String fileName = extractedFilePrefix + secondaryNumber + EXTRACTED_SUFFIX;

                File extractedFile = new File(dexDir, fileName);

                files.add(extractedFile);


                Log.i(TAG, "Extraction is needed for file " + extractedFile);

                int numAttempts = 0;

                boolean isExtractionSuccessful = false;


                while (numAttempts < MAX_EXTRACT_ATTEMPTS && !isExtractionSuccessful) {//retry.

                    numAttempts++;

                    // Create a zip file (extractedFile) containing only the secondary dex file

                    // (dexFile) from the apk.

                    extract(apk, dexFile, extractedFile, extractedFilePrefix);


                    // Verify that the extracted file is indeed a zip file.

                    isExtractionSuccessful = verifyZipFile(extractedFile);


                    // Log the sha1 of the extracted zip file


                    Log.i(TAG, "Extraction " + (isExtractionSuccessful ? "success" : "failed") +

                            " - length " + extractedFile.getAbsolutePath() + ": " +

                            extractedFile.length());

                    if (!isExtractionSuccessful) {

                        // Delete the extracted file

                        extractedFile.delete();

                        if (extractedFile.exists()) {

                            Log.w(TAG, "Failed to delete corrupted secondary dex '" +

                                    extractedFile.getPath() + "'");

                        }

                    }

                }//while retry extract.

                if (!isExtractionSuccessful) {

                    throw new IOException("Could not create zip file " +

                            extractedFile.getAbsolutePath() + " for secondary dex (" +

                            secondaryNumber + ")");

                }


                secondaryNumber++;

                dexFile = apk.getEntry(DEX_PREFIX + secondaryNumber + DEX_SUFFIX);

            }

        } finally {

            try {

                apk.close();

            } catch (IOException e) {

                Log.w(TAG, "Failed to close resource", e);

            }

        }


        return files;

    }

   public static List<File> getMutiDexs(File dexDir) {

        FileFilter filter = new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(EXTRACTED_SUFFIX);
            }
        };

        File[] files = dexDir.listFiles(filter);

        return files != null ? Arrays.asList(files) : new ArrayList<File>();

    }

    private static void extract(ZipFile apk, ZipEntry dexFile, File extractTo,
                                String extractedFilePrefix) throws IOException, FileNotFoundException {
        int BUFFER_SIZE = 0x4000;
        InputStream in = apk.getInputStream(dexFile);
        ZipOutputStream out = null;
        File tmp = File.createTempFile(extractedFilePrefix, EXTRACTED_SUFFIX,
                extractTo.getParentFile());
        Log.i(TAG, "Extracting " + tmp.getPath());
        try {
            out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)));
            try {
                ZipEntry classesDex = new ZipEntry("classes.dex");
                // keep zip entry time since it is the criteria used by Dalvik
                classesDex.setTime(dexFile.getTime());
                out.putNextEntry(classesDex);

                byte[] buffer = new byte[BUFFER_SIZE];
                int length = in.read(buffer);
                while (length != -1) {
                    out.write(buffer, 0, length);
                    length = in.read(buffer);
                }
                out.closeEntry();
            } finally {
                out.close();
            }
            Log.i(TAG, "Renaming to " + extractTo.getPath());
            if (!tmp.renameTo(extractTo)) {
                throw new IOException("Failed to rename \"" + tmp.getAbsolutePath() +
                        "\" to \"" + extractTo.getAbsolutePath() + "\"");
            }
        } finally {
            closeQuietly(in);
            tmp.delete(); // return status ignored
        }
    }

    /**
     * Returns whether the file is a valid zip file.
     */
    private static boolean verifyZipFile(File file) {
        try {
            ZipFile zipFile = new ZipFile(file);
            try {
                zipFile.close();
                return true;
            } catch (IOException e) {
                Log.w(TAG, "Failed to close zip file: " + file.getAbsolutePath());
            }
        } catch (ZipException ex) {
            Log.w(TAG, "File " + file.getAbsolutePath() + " is not a valid zip file.", ex);
        } catch (IOException ex) {
            Log.w(TAG, "Got an IOException trying to open zip file: " + file.getAbsolutePath(), ex);
        }
        return false;
    }
}
