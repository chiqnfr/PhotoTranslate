package corbatodd.phototranslate;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;


import com.googlecode.tesseract.android.TessBaseAPI;

import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarInputStream;

/**
 * Created by CorbaTodd on 4/2/15.
 */
public class OCRInitAsyncTask extends AsyncTask<String, String, Boolean> {

    private static final String[] CUBE_DATA_FILES = {".cube.bigrams", ".cube.fold", ".cube.lm", ".cube.nn", ".cube.params", ".cube.word-freq", ".tesseract_cube.nn", ".traineddata"};

    private MainActivity activity;
    private Context context;
    private TessBaseAPI baseApi;
    private ProgressDialog dialog;
    private ProgressDialog indeterminateDialog;
    private final String languageCode;
    private String languageName;
    private int ocrEngineMode;

    OCRInitAsyncTask(MainActivity activity, TessBaseAPI baseApi, ProgressDialog dialog, ProgressDialog indeterminateDialog, String languageCode, String languageName, int ocrEngineMode) {
        this.activity = activity;
        this.context = activity.getBaseContext();
        this.baseApi = baseApi;
        this.dialog = dialog;
        this.indeterminateDialog = indeterminateDialog;
        this.languageCode = languageCode;
        this.languageName = languageName;
        this.ocrEngineMode = ocrEngineMode;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog.setTitle("Please wait!");
        dialog.setMessage("Checking data for installation!");
        dialog.setIndeterminate(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.show();
        activity.setButtonVisibility(false);
    }

    protected Boolean doInBackground(String... params) {
        String destinationFilenameBase = languageCode + ".traineddata";
        boolean isCubeSupported = false;
        for (String s : MainActivity.CUBE_SUPPORTED_LANGUAGES) {
            if (s.equals(languageCode)) {
                isCubeSupported = true;
            }
        }
        destinationFilenameBase = "tesseract-ocr-3.02." + languageCode + ".tar";

        String destinationDirBase = params[0];
        File tessdataDir = new File(destinationDirBase + File.separator + "tessdata");
        if (!tessdataDir.exists() && !tessdataDir.mkdirs()) {
            return false;
        }

        File downloadFile = new File(tessdataDir, destinationFilenameBase);

        File incomplete = new File(tessdataDir, destinationFilenameBase + ".download");
        File tesseractTestFile = new File(tessdataDir, languageCode + ".traineddata");
        if (incomplete.exists()) {
            incomplete.delete();
            if (tesseractTestFile.exists()) {
                tesseractTestFile.delete();
            }
            deleteCubeDataFiles(tessdataDir);
        }

        boolean isAllCubeDataInstalled = false;
        if (isCubeSupported) {
            boolean isAFileMissing = false;
            File dataFile;
            for (String s : CUBE_DATA_FILES) {
                dataFile = new File(tessdataDir.toString() + File.separator + languageCode + s);
                if (!dataFile.exists()) {
                    isAFileMissing = true;
                }
            }
            isAllCubeDataInstalled = !isAFileMissing;
        }

        boolean installSuccess = false;
        if (!tesseractTestFile.exists()
                || (isCubeSupported && !isAllCubeDataInstalled)) {
            deleteCubeDataFiles(tessdataDir);

            try {
                installSuccess = installFromAssets(destinationFilenameBase + ".zip", tessdataDir, downloadFile);
            } catch (IOException e) {
            } catch (Exception e) {
            }

            if (!installSuccess) {
                try {
                    installSuccess = downloadFile(destinationFilenameBase, downloadFile);
                    if (!installSuccess) {
                        return false;
                    }
                } catch (IOException e) {
                    return false;
                }
            }

            String extension = destinationFilenameBase.substring(
                    destinationFilenameBase.lastIndexOf('.'),
                    destinationFilenameBase.length());
            if (extension.equals(".tar")) {
                try {
                    untar(new File(tessdataDir.toString() + File.separator + destinationFilenameBase), tessdataDir);
                    installSuccess = true;
                } catch (IOException e) {
                    return false;
                }
            }

        } else {
            installSuccess = true;
        }
        File osdFile = new File(tessdataDir, MainActivity.OSD_FILENAME_BASE);
        boolean osdInstallSuccess = false;
        if (!osdFile.exists()) {
            languageName = "orientation and script detection!";
            try {
                String[] badFiles = { MainActivity.OSD_FILENAME + ".gz.download",
                        MainActivity.OSD_FILENAME + ".gz", MainActivity.OSD_FILENAME };
                for (String filename : badFiles) {
                    File file = new File(tessdataDir, filename);
                    if (file.exists()) {
                        file.delete();
                    }
                }

                osdInstallSuccess = installFromAssets(MainActivity.OSD_FILENAME_BASE + ".zip", tessdataDir, new File(MainActivity.OSD_FILENAME));
            } catch (IOException e) {
            } catch (Exception e) {
            }

            if (!osdInstallSuccess) {
                try {
                    osdInstallSuccess = downloadFile(MainActivity.OSD_FILENAME, new File(tessdataDir, MainActivity.OSD_FILENAME));
                    if (!osdInstallSuccess) {
                        return false;
                    }
                } catch (IOException e) {
                    return false;
                }
            }

            try {
                untar(new File(tessdataDir.toString() + File.separator + MainActivity.OSD_FILENAME), tessdataDir);
            } catch (IOException e) {
                return false;
            }

        } else {
            osdInstallSuccess = true;
        }

        try {
            dialog.dismiss();
        } catch (IllegalArgumentException e) {
        }

        if (baseApi.init(destinationDirBase + File.separator, languageCode, ocrEngineMode)) {
            return installSuccess && osdInstallSuccess;
        }
        return false;
    }

    private void deleteCubeDataFiles(File tessdataDir) {
        File badFile;
        for (String s : CUBE_DATA_FILES) {
            badFile = new File(tessdataDir.toString() + File.separator + languageCode + s);
            if (badFile.exists()) {
                badFile.delete();
            }
            badFile = new File(tessdataDir.toString() + File.separator + "tesseract-ocr-3.01." + languageCode + ".tar");
            if (badFile.exists()) {
                badFile.delete();
            }
        }
    }

    private boolean downloadFile(String sourceFilenameBase, File destinationFile) throws IOException {
        try {
            return downloadGzippedFileHttp(new URL(MainActivity.DOWNLOAD_BASE + sourceFilenameBase + ".gz"), destinationFile);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Bad URL string.");
        }
    }

    private boolean downloadGzippedFileHttp(URL url, File destinationFile) throws IOException {
        publishProgress("Downloading data for " + languageName + "!", "0");
        HttpURLConnection urlConnection = null;
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setAllowUserInteraction(false);
        urlConnection.setInstanceFollowRedirects(true);
        urlConnection.setRequestMethod("GET");
        urlConnection.connect();
        if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return false;
        }
        int fileSize = urlConnection.getContentLength();
        InputStream inputStream = urlConnection.getInputStream();
        File tempFile = new File(destinationFile.toString() + ".gz.download");

        final int BUFFER = 8192;
        FileOutputStream fileOutputStream = null;
        Integer percentComplete;
        int percentCompleteLast = 0;
        try {
            fileOutputStream = new FileOutputStream(tempFile);
        } catch (FileNotFoundException e) {
        }
        int downloaded = 0;
        byte[] buffer = new byte[BUFFER];
        int bufferLength = 0;
        while ((bufferLength = inputStream.read(buffer, 0, BUFFER)) > 0) {
            fileOutputStream.write(buffer, 0, bufferLength);
            downloaded += bufferLength;
            percentComplete = (int) ((downloaded / (float) fileSize) * 100);
            if (percentComplete > percentCompleteLast) {
                publishProgress("Downloading data for " + languageName + "!", percentComplete.toString());
                percentCompleteLast = percentComplete;
            }
        }
        fileOutputStream.close();
        if (urlConnection != null) {
            urlConnection.disconnect();
        }

        try {
            gunzip(tempFile,
                    new File(tempFile.toString().replace(".gz.download", "")));
            return true;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return false;
    }

    private void gunzip(File zippedFile, File outFilePath) throws FileNotFoundException, IOException {
        int uncompressedFileSize = getGzipSizeUncompressed(zippedFile);
        Integer percentComplete;
        int percentCompleteLast = 0;
        int unzippedBytes = 0;
        final Integer progressMin = 0;
        int progressMax = 100 - progressMin;
        publishProgress("Uncompressing data for " + languageName + "!", progressMin.toString());

        String extension = zippedFile.toString().substring(zippedFile.toString().length() - 16);
        if (extension.equals(".tar.gz.download")) {
            progressMax = 50;
        }
        GZIPInputStream gzipInputStream = new GZIPInputStream(
                new BufferedInputStream(new FileInputStream(zippedFile)));
        OutputStream outputStream = new FileOutputStream(outFilePath);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

        final int BUFFER = 8192;
        byte[] data = new byte[BUFFER];
        int len;
        while ((len = gzipInputStream.read(data, 0, BUFFER)) > 0) {
            bufferedOutputStream.write(data, 0, len);
            unzippedBytes += len;
            percentComplete = (int) ((unzippedBytes / (float) uncompressedFileSize) * progressMax) + progressMin;

            if (percentComplete > percentCompleteLast) {
                publishProgress("Uncompressing data for " + languageName + "!", percentComplete.toString());
                percentCompleteLast = percentComplete;
            }
        }
        gzipInputStream.close();
        bufferedOutputStream.flush();
        bufferedOutputStream.close();

        if (zippedFile.exists()) {
            zippedFile.delete();
        }
    }

    private int getGzipSizeUncompressed(File zipFile) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(zipFile, "r");
        raf.seek(raf.length() - 4);
        int b4 = raf.read();
        int b3 = raf.read();
        int b2 = raf.read();
        int b1 = raf.read();
        raf.close();
        return (b1 << 24) | (b2 << 16) + (b3 << 8) + b4;
    }
    private void untar(File tarFile, File destinationDir) throws IOException {
        final int uncompressedSize = getTarSizeUncompressed(tarFile);
        Integer percentComplete;
        int percentCompleteLast = 0;
        int unzippedBytes = 0;
        final Integer progressMin = 50;
        final int progressMax = 100 - progressMin;
        publishProgress("Uncompressing data for " + languageName + "!", progressMin.toString());

        TarInputStream tarInputStream = new TarInputStream(new BufferedInputStream(new FileInputStream(tarFile)));
        TarEntry entry;
        while ((entry = tarInputStream.getNextEntry()) != null) {
            int len;
            final int BUFFER = 8192;
            byte data[] = new byte[BUFFER];
            String pathName = entry.getName();
            String fileName = pathName.substring(pathName.lastIndexOf('/'), pathName.length());
            OutputStream outputStream = new FileOutputStream(destinationDir + fileName);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

            while ((len = tarInputStream.read(data, 0, BUFFER)) != -1) {
                bufferedOutputStream.write(data, 0, len);
                unzippedBytes += len;
                percentComplete = (int) ((unzippedBytes / (float) uncompressedSize) * progressMax) + progressMin;
                if (percentComplete > percentCompleteLast) {
                    publishProgress("Uncompressing data for " + languageName + "!", percentComplete.toString());
                    percentCompleteLast = percentComplete;
                }
            }
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
        }
        tarInputStream.close();

        if (tarFile.exists()) {
            tarFile.delete();
        }
    }

    private int getTarSizeUncompressed(File tarFile) throws IOException {
        int size = 0;
        TarInputStream tis = new TarInputStream(new BufferedInputStream(new FileInputStream(tarFile)));
        TarEntry entry;
        while ((entry = tis.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                size += entry.getSize();
            }
        }
        tis.close();
        return size;
    }

    private boolean installFromAssets(String sourceFilename, File modelRoot, File destinationFile) throws IOException {
        String extension = sourceFilename.substring(sourceFilename.lastIndexOf('.'), sourceFilename.length());
        try {
            if (extension.equals(".zip")) {
                return installZipFromAssets(sourceFilename, modelRoot, destinationFile);
            } else {
                throw new IllegalArgumentException("Extension " + extension + " is unsupported.");
            }
        } catch (FileNotFoundException e) {
        }
        return false;
    }

    private boolean installZipFromAssets(String sourceFilename, File destinationDir, File destinationFile) throws IOException, FileNotFoundException {
        publishProgress("Uncompressing data for " + languageName + "!", "0");
        ZipInputStream inputStream = new ZipInputStream(context.getAssets().open(sourceFilename));

        for (ZipEntry entry = inputStream.getNextEntry(); entry != null; entry = inputStream.getNextEntry()) {
            destinationFile = new File(destinationDir, entry.getName());

            if (entry.isDirectory()) {
                destinationFile.mkdirs();
            } else {
                long zippedFileSize = entry.getSize();

                FileOutputStream outputStream = new FileOutputStream(destinationFile);
                final int BUFFER = 8192;

                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream, BUFFER);
                int unzippedSize = 0;

                int count = 0;
                Integer percentComplete = 0;
                Integer percentCompleteLast = 0;
                byte[] data = new byte[BUFFER];
                while ((count = inputStream.read(data, 0, BUFFER)) != -1) {
                    bufferedOutputStream.write(data, 0, count);
                    unzippedSize += count;
                    percentComplete = (int) ((unzippedSize / (long) zippedFileSize) * 100);
                    if (percentComplete > percentCompleteLast) {
                        publishProgress("Uncompressing data for " + languageName + "!", percentComplete.toString(), "0");
                        percentCompleteLast = percentComplete;
                    }
                }
                bufferedOutputStream.close();
            }
            inputStream.closeEntry();
        }
        inputStream.close();
        return true;
    }

    @Override
    protected void onProgressUpdate(String... message) {
        super.onProgressUpdate(message);
        int percentComplete = 0;

        percentComplete = Integer.parseInt(message[1]);
        dialog.setMessage(message[0]);
        dialog.setProgress(percentComplete);
        dialog.show();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        try {
            indeterminateDialog.dismiss();
        } catch (IllegalArgumentException e) {
        }

        if (result) {
            activity.resumeOCR();
        } else {
            activity.showErrorMessage("Error", "Network is unreachable - cannot download language data! " + "Please enable network access and restart application!");
        }
    }

}
