package corbatodd.phototranslate;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.TessBaseAPI;

/**
 * Created by CorbaTodd on 4/2/15.
 */
public class DecodeHandler extends Handler{

    private final MainActivity activity;
    private boolean running = true;
    private final TessBaseAPI baseApi;
    private Bitmap bitmap;
    private static boolean isDecodePending;
    private long timeRequired;

    DecodeHandler(MainActivity activity) {
        this.activity = activity;
        baseApi = activity.getBaseApi();
    }

    @Override
    public void handleMessage(Message message) {
        if (!running) {
            return;
        }
        switch (message.what) {
            case R.id.ocr_continuous_decode:
                if (!isDecodePending) {
                    isDecodePending = true;
                    ocrContinuousDecode((byte[]) message.obj, message.arg1, message.arg2);
                }
                break;
            case R.id.ocr_decode:
                ocrDecode((byte[]) message.obj, message.arg1, message.arg2);
                break;
            case R.id.quit:
                running = false;
                Looper.myLooper().quit();
                break;
        }
    }

    static void resetDecodeState() {
        isDecodePending = false;
    }

    private void ocrDecode(byte[] data, int width, int height) {
        activity.displayProgressDialog();

        new OCRRecognizeAsyncTask(activity, baseApi, data, width, height).execute();
    }

    private void ocrContinuousDecode(byte[] data, int width, int height) {
        PreProcessSource source = activity.getCameraManager().buildLuminanceSource(data, width, height);
        if (source == null) {
            sendContinuousOcrFailMessage();
            return;
        }
        bitmap = source.renderCroppedGreyscaleBitmap();

        OCRResult OCRResult = getOcrResult();
        Handler handler = activity.getHandler();
        if (handler == null) {
            return;
        }

        if (OCRResult == null) {
            try {
                sendContinuousOcrFailMessage();
            } catch (NullPointerException e) {
                activity.stopHandler();
            } finally {
                bitmap.recycle();
                baseApi.clear();
            }
            return;
        }

        try {
            Message message = Message.obtain(handler, R.id.ocr_continuous_decode_succeeded, OCRResult);
            message.sendToTarget();
        } catch (NullPointerException e) {
            activity.stopHandler();
        } finally {
            baseApi.clear();
        }
    }

    private OCRResult getOcrResult() {
        OCRResult OCRResult;
        String textResult;
        long start = System.currentTimeMillis();

        try {
            baseApi.setImage(ReadFile.readBitmap(bitmap));
            textResult = baseApi.getUTF8Text();
            timeRequired = System.currentTimeMillis() - start;

            if (textResult == null || textResult.equals("")) {
                return null;
            }
            OCRResult = new OCRResult();
            OCRResult.setWordConfidences(baseApi.wordConfidences());
            OCRResult.setMeanConfidence( baseApi.meanConfidence());
            if (FocusBoxView.DRAW_REGION_BOXES) {
                OCRResult.setRegionBoundingBoxes(baseApi.getRegions().getBoxRects());
            }
            if (FocusBoxView.DRAW_TEXTLINE_BOXES) {
                OCRResult.setTextlineBoundingBoxes(baseApi.getTextlines().getBoxRects());
            }
            if (FocusBoxView.DRAW_STRIP_BOXES) {
                OCRResult.setStripBoundingBoxes(baseApi.getStrips().getBoxRects());
            }

            OCRResult.setWordBoundingBoxes(baseApi.getWords().getBoxRects());

        } catch (RuntimeException e) {
            e.printStackTrace();
            try {
                baseApi.clear();
                activity.stopHandler();
            } catch (NullPointerException e1) {
            }
            return null;
        }
        timeRequired = System.currentTimeMillis() - start;
        OCRResult.setBitmap(bitmap);
        OCRResult.setText(textResult);
        OCRResult.setRecognitionTimeRequired(timeRequired);
        return OCRResult;
    }

    private void sendContinuousOcrFailMessage() {
        Handler handler = activity.getHandler();
        if (handler != null) {
            Message message = Message.obtain(handler, R.id.ocr_continuous_decode_failed, new OCRResultFail(timeRequired));
            message.sendToTarget();
        }
    }
}
