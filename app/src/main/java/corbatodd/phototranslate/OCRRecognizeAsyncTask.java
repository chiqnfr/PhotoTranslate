package corbatodd.phototranslate;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Created by CorbaTodd on 4/2/15.
 */
public class OCRRecognizeAsyncTask extends AsyncTask<Void, Void, Boolean>{

    private MainActivity activity;
    private TessBaseAPI baseApi;
    private byte[] data;
    private int width;
    private int height;
    private OCRResult OCRResult;
    private long timeRequired;
    private Bitmap bitmap;
    private Mat mrgba;

    OCRRecognizeAsyncTask(MainActivity activity, TessBaseAPI baseApi, byte[] data, int width, int height) {
        this.activity = activity;
        this.baseApi = baseApi;
        this.data = data;
        this.width = width;
        this.height = height;
    }

    @Override
    protected Boolean doInBackground(Void... arg0) {
        long start = System.currentTimeMillis();
        bitmap = activity.getCameraManager().buildLuminanceSource(data, width, height).renderCroppedGreyscaleBitmap();
        String textResult;

        render();

        try {
            baseApi.setImage(ReadFile.readBitmap(bitmap));
            textResult = baseApi.getUTF8Text();
            timeRequired = System.currentTimeMillis() - start;
            if (textResult == null || textResult.equals("")) {
                return false;
            }
            OCRResult = new OCRResult();
            OCRResult.setWordConfidences(baseApi.wordConfidences());
            OCRResult.setMeanConfidence( baseApi.meanConfidence());
            OCRResult.setRegionBoundingBoxes(baseApi.getRegions().getBoxRects());
            OCRResult.setTextlineBoundingBoxes(baseApi.getTextlines().getBoxRects());
            OCRResult.setWordBoundingBoxes(baseApi.getWords().getBoxRects());
            OCRResult.setStripBoundingBoxes(baseApi.getStrips().getBoxRects());
        } catch (RuntimeException e) {
            e.printStackTrace();
            try {
                baseApi.clear();
                activity.stopHandler();
            } catch (NullPointerException e1) {
            }
            return false;
        }
        timeRequired = System.currentTimeMillis() - start;
        OCRResult.setBitmap(bitmap);
        OCRResult.setText(textResult);
        OCRResult.setRecognitionTimeRequired(timeRequired);
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        Handler handler = activity.getHandler();
        if (handler != null) {
            if (result) {
                Message message = Message.obtain(handler, R.id.ocr_decode_succeeded, OCRResult);
                message.sendToTarget();
            } else {
                Message message = Message.obtain(handler, R.id.ocr_decode_failed, OCRResult);
                message.sendToTarget();
            }
            activity.getProgressDialog().dismiss();
        }
        if (baseApi != null) {
            baseApi.clear();
        }
    }

    public void render(){
        if (!OpenCVLoader.initDebug()) {

        }else {
            mrgba = new Mat();
            //convert bitmap to ARGB_8888
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Utils.bitmapToMat(bitmap, mrgba);
            //set grayscale
            Imgproc.cvtColor(mrgba, mrgba, Imgproc.COLOR_BGR2GRAY);

            //blur
            //Size size = new Size(3,3);
            //Imgproc.GaussianBlur(mrgba, mrgba,size, 0);
            //Imgproc.medianBlur(mrgba, mrgba, 3);

            //Adaptive Threshold
            Imgproc.threshold(mrgba, mrgba, 0, 255, Imgproc.THRESH_OTSU);
            //Imgproc.adaptiveThreshold(mrgba, mrgba, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 61,15);
            
            Utils.matToBitmap(mrgba, bitmap, false);

        }



    }
}
