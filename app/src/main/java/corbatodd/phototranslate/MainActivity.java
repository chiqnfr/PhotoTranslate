package corbatodd.phototranslate;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

/**
 * Created by CorbaTodd on 4/2/15.
 */

public class MainActivity extends Activity implements SurfaceHolder.Callback, TakePictureButton.OnShutterButtonListener {

    public static final String DEFAULT_SOURCE_LANGUAGE_CODE = "eng"; //Source language for OCR
    public static final String DEFAULT_TARGET_LANGUAGE_CODE = "vi"; //Target language for translate
    public static final String DEFAULT_TRANSLATOR = "Google Translate"; //Translate engine
    public static final String DEFAULT_OCR_ENGINE_MODE = "Tesseract"; //OCR engine
    public static final String DEFAULT_PAGE_SEGMENTATION_MODE = "Auto";
    public static final boolean DEFAULT_TOGGLE_CONTINUOUS = false;
    private static final boolean DISPLAY_SHUTTER_BUTTON = true;
    static final String[] CUBE_SUPPORTED_LANGUAGES = {"ara", "eng", "hin"};
    private static final String[] CUBE_REQUIRED_LANGUAGES = {"ara"};
    static final String DOWNLOAD_BASE = "http://tesseract-ocr.googlecode.com/files/";//Resource
    static final String OSD_FILENAME = "tesseract-ocr-3.01.osd.tar";//Orientation and script detection(OSD) data
    static final String OSD_FILENAME_BASE = "osd.traineddata";

    private CameraManager cameraManager;
    private CaptureHandler handler;
    private FocusBoxView viewfinderView;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private TextView ocrResultView;
    private TextView translationView;
    private View cameraButtonView;
    private View resultView;
    private View progressView;
    private OCRResult lastResult;
    private Bitmap lastBitmap;
    private boolean hasSurface;
    private TessBaseAPI baseApi;
    private String sourceLanguageCodeOcr;
    private String sourceLanguageReadable; // Language name, for example, "English"
    private int pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD;
    private int ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
    private String characterBlacklist;
    private String characterWhitelist;
    private TakePictureButton takePictureButton;
    private boolean isContinuousModeActive; // Whether we are doing OCR in continuous mode
    private SharedPreferences prefs;
    private OnSharedPreferenceChangeListener listener;
    private ProgressDialog dialog; // download language and unzip
    private ProgressDialog indeterminateDialog; //init OCR
    private boolean isEngineReady;
    private boolean isPaused;
    private static boolean isFirstLaunch;

    GoogleTranslate translator;

    Handler getHandler() {
        return handler;
    }

    TessBaseAPI getBaseApi() {
        return baseApi;
    }

    CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (isFirstLaunch) {
            setDefaultPreferences();
        }
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main_activity);
        viewfinderView = (FocusBoxView) findViewById(R.id.viewfinder_view);
        cameraButtonView = findViewById(R.id.camera_button_view);
        resultView = findViewById(R.id.result_view);
        handler = null;
        lastResult = null;
        hasSurface = false;

        if (DISPLAY_SHUTTER_BUTTON) {
            takePictureButton = (TakePictureButton) findViewById(R.id.shutter_button);
            takePictureButton.setOnShutterButtonListener(this);
        }

        ocrResultView = (TextView) findViewById(R.id.ocr_result_text_view);
        registerForContextMenu(ocrResultView);
        translationView = (TextView) findViewById(R.id.translation_text_view);
        registerForContextMenu(translationView);

        progressView = (View) findViewById(R.id.indeterminate_progress_indicator_view);

        cameraManager = new CameraManager(getApplication());
        viewfinderView.setCameraManager(cameraManager);

        viewfinderView.setOnTouchListener(new View.OnTouchListener() {
            int lastX = -1;
            int lastY = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = -1;
                        lastY = -1;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int currentX = (int) event.getX();
                        int currentY = (int) event.getY();

                        try {
                            Rect rect = cameraManager.getFramingRect();

                            final int BUFFER = 50;
                            final int BIG_BUFFER = 60;
                            if (lastX >= 0) {
                                if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER)) && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
                                    cameraManager.adjustFramingRect( 2 * (lastX - currentX), 2 * (lastY - currentY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER)) && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
                                    cameraManager.adjustFramingRect( 2 * (currentX - lastX), 2 * (lastY - currentY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER)) && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
                                    cameraManager.adjustFramingRect(2 * (lastX - currentX), 2 * (currentY - lastY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER)) && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
                                    cameraManager.adjustFramingRect(2 * (currentX - lastX), 2 * (currentY - lastY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.left - BUFFER && currentX <= rect.left + BUFFER) || (lastX >= rect.left - BUFFER && lastX <= rect.left + BUFFER)) && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
                                    cameraManager.adjustFramingRect(2 * (lastX - currentX), 0);
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.right - BUFFER && currentX <= rect.right + BUFFER) || (lastX >= rect.right - BUFFER && lastX <= rect.right + BUFFER)) && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
                                    cameraManager.adjustFramingRect(2 * (currentX - lastX), 0);
                                    viewfinderView.removeResultText();
                                } else if (((currentY <= rect.top + BUFFER && currentY >= rect.top - BUFFER) || (lastY <= rect.top + BUFFER && lastY >= rect.top - BUFFER)) && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
                                    cameraManager.adjustFramingRect(0, 2 * (lastY - currentY));
                                    viewfinderView.removeResultText();
                                } else if (((currentY <= rect.bottom + BUFFER && currentY >= rect.bottom - BUFFER) || (lastY <= rect.bottom + BUFFER && lastY >= rect.bottom - BUFFER)) && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
                                    cameraManager.adjustFramingRect(0, 2 * (currentY - lastY));
                                    viewfinderView.removeResultText();
                                }
                            }
                        } catch (NullPointerException e) {
                        }
                        v.invalidate();
                        lastX = currentX;
                        lastY = currentY;
                        return true;
                    case MotionEvent.ACTION_UP:
                        lastX = -1;
                        lastY = -1;
                        return true;
                }
                return false;
            }
        });

        isEngineReady = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetStatusView();

        String previousSourceLanguageCodeOcr = sourceLanguageCodeOcr;
        int previousOcrEngineMode = ocrEngineMode;

        retrievePreferences();

        surfaceView = (SurfaceView) findViewById(R.id.sv_camera_preview);
        surfaceHolder = surfaceView.getHolder();
        if (!hasSurface) {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        boolean doNewInit = (baseApi == null) || !sourceLanguageCodeOcr.equals(previousSourceLanguageCodeOcr) ||
                ocrEngineMode != previousOcrEngineMode;
        if (doNewInit) {
            File storageDirectory = getStorageDirectory();
            if (storageDirectory != null) {
                initOcrEngine(storageDirectory, sourceLanguageCodeOcr, sourceLanguageReadable);
            }
        } else {
            resumeOCR();
        }
    }

    void resumeOCR() {
        isEngineReady = true;

        isPaused = false;

        if (handler != null) {
            handler.resetState();
        }
        if (baseApi != null) {
            baseApi.setPageSegMode(pageSegmentationMode);
            baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, characterBlacklist);
            baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, characterWhitelist);
        }

        if (hasSurface) {
            initCamera(surfaceHolder);
        }
    }

    void onShutterButtonPressContinuous() {
        isPaused = true;
        handler.stop();
        if (lastResult != null) {
            handleOcrDecode(lastResult);
        } else {
            Toast toast = Toast.makeText(this, "Cannot recognize text! Try again!", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 0);
            toast.show();
            resumeContinuousDecoding();
        }
    }
    void resumeContinuousDecoding() {
        isPaused = false;
        resetStatusView();
        setStatusViewForContinuous();
        DecodeHandler.resetDecodeState();
        handler.resetState();
        if (takePictureButton != null && DISPLAY_SHUTTER_BUTTON) {
            takePictureButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        if (holder == null) {
        }
        if (!hasSurface && isEngineReady) {
            initCamera(holder);
        }
        hasSurface = true;
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            handler = new CaptureHandler(this, cameraManager, isContinuousModeActive);

        } catch (IOException ioe) {
            showErrorMessage("Error", "Could not initialize camera. Please try restarting device.");
        } catch (RuntimeException e) {
            showErrorMessage("Error", "Could not initialize camera. Please try restarting device.");
        }
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
        }

        cameraManager.closeDriver();

        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.sv_camera_preview);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    void stopHandler() {
        if (handler != null) {
            handler.stop();
        }
    }

    @Override
    protected void onDestroy() {
        if (baseApi != null) {
            baseApi.end();
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (isPaused) {
                resumeContinuousDecoding();
                return true;
            }
            if (lastResult == null) {
                setResult(RESULT_CANCELED);
                finish();
                return true;
            } else {
                resetStatusView();
                if (handler != null) {
                    handler.sendEmptyMessage(R.id.restart_preview);
                }
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            if (isContinuousModeActive) {
                onShutterButtonPressContinuous();
            } else {
                handler.hardwareShutterButtonClick();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_FOCUS) {
            if (event.getRepeatCount() == 0) {
                cameraManager.requestAutoFocus(500L);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    private boolean setSourceLanguage(String languageCode) {
        sourceLanguageCodeOcr = languageCode;
        sourceLanguageReadable = Language.getOcrLanguageName(this, languageCode);
        return true;
    }
    private boolean setTargetLanguage(String languageCode) {
        return true;
    }

    private File getStorageDirectory() {

        String state = null;
        try {
            state = Environment.getExternalStorageState();
        } catch (RuntimeException e) {
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable.");
        }

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            try {
                return getExternalFilesDir(Environment.MEDIA_MOUNTED);
            } catch (NullPointerException e) {
                showErrorMessage("Error", "Required external storage (such as an SD card) is full or unavailable.");
            }

        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable for data storage.");
        } else {
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable or corrupted.");
        }
        return null;
    }

    private void initOcrEngine(File storageRoot, String languageCode, String languageName) {
        isEngineReady = false;

        if (dialog != null) {
            dialog.dismiss();
        }
        dialog = new ProgressDialog(this);
        if (ocrEngineMode != TessBaseAPI.OEM_CUBE_ONLY) {
            for (String s : CUBE_REQUIRED_LANGUAGES) {
                if (s.equals(languageCode)) {
                    ocrEngineMode = TessBaseAPI.OEM_CUBE_ONLY;
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    prefs.edit().putString(Preferences.KEY_OCR_ENGINE_MODE, getOcrEngineModeName()).commit();
                }
            }
        }

        if (ocrEngineMode != TessBaseAPI.OEM_TESSERACT_ONLY) {
            boolean cubeOk = false;
            for (String s : CUBE_SUPPORTED_LANGUAGES) {
                if (s.equals(languageCode)) {
                    cubeOk = true;
                }
            }
            if (!cubeOk) {
                ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                prefs.edit().putString(Preferences.KEY_OCR_ENGINE_MODE, getOcrEngineModeName()).commit();
            }
        }

        indeterminateDialog = new ProgressDialog(this);
        indeterminateDialog.setTitle("Please wait");
        String ocrEngineModeName = getOcrEngineModeName();
        if (ocrEngineModeName.equals("Both")) {
            indeterminateDialog.setMessage("Initializing Cube and Tesseract OCR engines for " + languageName + "!");
        } else {
            indeterminateDialog.setMessage("Initializing " + ocrEngineModeName + " OCR engine for " + languageName + "!");
        }
        indeterminateDialog.setCancelable(false);
        indeterminateDialog.show();

        if (handler != null) {
            handler.quitSynchronously();
        }

        if (ocrEngineMode == TessBaseAPI.OEM_CUBE_ONLY || ocrEngineMode == TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED) {
            isContinuousModeActive = false;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putBoolean(Preferences.KEY_CONTINUOUS_PREVIEW, false);
        }
        baseApi = new TessBaseAPI();
        new OCRInitAsyncTask(this, baseApi, dialog, indeterminateDialog, languageCode, languageName, ocrEngineMode).execute(storageRoot.toString());

        TextView sourceLanguageTextView = (TextView) findViewById(R.id.source_language_text_view);
        sourceLanguageTextView.setText(sourceLanguageReadable);
    }

    boolean handleOcrDecode(OCRResult OCRResult) {
        lastResult = OCRResult;

        if (OCRResult.getText() == null || OCRResult.getText().equals("")) {
            Toast toast = Toast.makeText(this, "Cannot recognize text! Try again!", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 0);
            toast.show();
            return false;
        }

        takePictureButton.setVisibility(View.GONE);
        cameraButtonView.setVisibility(View.GONE);
        viewfinderView.setVisibility(View.GONE);
        resultView.setVisibility(View.VISIBLE);

        ImageView bitmapImageView = (ImageView) findViewById(R.id.image_view);
        lastBitmap = OCRResult.getBitmap();
        if (lastBitmap == null) {
            bitmapImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
        } else {
            bitmapImageView.setImageBitmap(lastBitmap);
        }

        ocrResultView.setText(OCRResult.getText());
        int scaledSize = Math.max(22, 32 - OCRResult.getText().length() / 4);
        ocrResultView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);

        progressView.setVisibility(View.VISIBLE);
        setProgressBarVisibility(true);

        new Translate().execute();

        return true;
    }

    void handleOcrContinuousDecode(OCRResult OCRResult) {
        lastResult = OCRResult;
        viewfinderView.addResultText(new OCRResultText(OCRResult.getText(), OCRResult.getWordConfidences(), OCRResult.getMeanConfidence(), OCRResult.getBitmapDimensions(), OCRResult.getRegionBoundingBoxes(), OCRResult.getTextlineBoundingBoxes(), OCRResult.getStripBoundingBoxes(), OCRResult.getWordBoundingBoxes(), OCRResult.getCharacterBoundingBoxes()));
        Integer meanConfidence = OCRResult.getMeanConfidence();
    }

    void handleOcrContinuousDecode(OCRResultFail obj) {
        lastResult = null;
        viewfinderView.removeResultText();
    }

    private CharSequence setSpanBetweenTokens(CharSequence text, String token, CharacterStyle... cs) {
        int tokenLen = token.length();
        int start = text.toString().indexOf(token) + tokenLen;
        int end = text.toString().indexOf(token, start);

        if (start > -1 && end > -1) {
            SpannableStringBuilder ssb = new SpannableStringBuilder(text);
            for (CharacterStyle c : cs)
                ssb.setSpan(c, start, end, 0);
            text = ssb;
        }
        return text;
    }

    private void resetStatusView() {
        resultView.setVisibility(View.GONE);
        viewfinderView.setVisibility(View.VISIBLE);
        cameraButtonView.setVisibility(View.VISIBLE);
        if (DISPLAY_SHUTTER_BUTTON) {
            takePictureButton.setVisibility(View.VISIBLE);
        }
        lastResult = null;
        viewfinderView.removeResultText();
    }

    void setStatusViewForContinuous() {
        viewfinderView.removeResultText();
    }

    void setButtonVisibility(boolean visible) {
        if (takePictureButton != null && visible == true && DISPLAY_SHUTTER_BUTTON) {
            takePictureButton.setVisibility(View.VISIBLE);
        } else if (takePictureButton != null) {
            takePictureButton.setVisibility(View.GONE);
        }
    }

    void setShutterButtonClickable(boolean clickable) {
        takePictureButton.setClickable(clickable);
    }

    void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    @Override
    public void onShutterButtonClick(TakePictureButton b) {
        if (isContinuousModeActive) {
            onShutterButtonPressContinuous();
        } else {
            if (handler != null) {
                handler.shutterButtonClick();
            }
        }
    }

    @Override
    public void onShutterButtonFocus(TakePictureButton b, boolean pressed) {
        requestDelayedAutoFocus();
    }

    private void requestDelayedAutoFocus() {
        cameraManager.requestAutoFocus(350L);
    }

    static boolean getFirstLaunch() {
        return isFirstLaunch;
    }

    String getOcrEngineModeName() {
        String ocrEngineModeName = "";
        String[] ocrEngineModes = getResources().getStringArray(R.array.ocrenginemodes);
        if (ocrEngineMode == TessBaseAPI.OEM_TESSERACT_ONLY) {
            ocrEngineModeName = ocrEngineModes[0];
        } else if (ocrEngineMode == TessBaseAPI.OEM_CUBE_ONLY) {
            ocrEngineModeName = ocrEngineModes[1];
        } else if (ocrEngineMode == TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED) {
            ocrEngineModeName = ocrEngineModes[2];
        }
        return ocrEngineModeName;
    }

    private void retrievePreferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setSourceLanguage(prefs.getString(Preferences.KEY_SOURCE_LANGUAGE_PREFERENCE, MainActivity.DEFAULT_SOURCE_LANGUAGE_CODE));
        setTargetLanguage(prefs.getString(Preferences.KEY_TARGET_LANGUAGE_PREFERENCE, MainActivity.DEFAULT_TARGET_LANGUAGE_CODE));

        if (prefs.getBoolean(Preferences.KEY_CONTINUOUS_PREVIEW, MainActivity.DEFAULT_TOGGLE_CONTINUOUS)) {
            isContinuousModeActive = true;
        } else {
            isContinuousModeActive = false;
        }

        String[] pageSegmentationModes = getResources().getStringArray(R.array.pagesegmentationmodes);
        String pageSegmentationModeName = prefs.getString(Preferences.KEY_PAGE_SEGMENTATION_MODE, pageSegmentationModes[0]);
        if (pageSegmentationModeName.equals(pageSegmentationModes[0])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[1])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[2])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[3])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_CHAR;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[4])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_COLUMN;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[5])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[6])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_WORD;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[7])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK_VERT_TEXT;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[8])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT;
        }

        String[] ocrEngineModes = getResources().getStringArray(R.array.ocrenginemodes);
        String ocrEngineModeName = prefs.getString(Preferences.KEY_OCR_ENGINE_MODE, ocrEngineModes[0]);
        if (ocrEngineModeName.equals(ocrEngineModes[0])) {
            ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
        } else if (ocrEngineModeName.equals(ocrEngineModes[1])) {
            ocrEngineMode = TessBaseAPI.OEM_CUBE_ONLY;
        } else if (ocrEngineModeName.equals(ocrEngineModes[2])) {
            ocrEngineMode = TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED;
        }

        characterBlacklist = OCRCharacter.getBlacklist(prefs, sourceLanguageCodeOcr);
        characterWhitelist = OCRCharacter.getWhitelist(prefs, sourceLanguageCodeOcr);

        prefs.registerOnSharedPreferenceChangeListener(listener);

    }

   private void setDefaultPreferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(Preferences.KEY_CONTINUOUS_PREVIEW, MainActivity.DEFAULT_TOGGLE_CONTINUOUS).commit();
        prefs.edit().putString(Preferences.KEY_SOURCE_LANGUAGE_PREFERENCE, MainActivity.DEFAULT_SOURCE_LANGUAGE_CODE).commit();
        prefs.edit().putString(Preferences.KEY_TARGET_LANGUAGE_PREFERENCE, MainActivity.DEFAULT_TARGET_LANGUAGE_CODE).commit();
        prefs.edit().putString(Preferences.KEY_TRANSLATOR, MainActivity.DEFAULT_TRANSLATOR).commit();
        prefs.edit().putString(Preferences.KEY_OCR_ENGINE_MODE, MainActivity.DEFAULT_OCR_ENGINE_MODE).commit();
    }

    void displayProgressDialog() {
        indeterminateDialog = new ProgressDialog(this);
        indeterminateDialog.setTitle("Please wait");
        String ocrEngineModeName = getOcrEngineModeName();
        if (ocrEngineModeName.equals("Both")) {
            indeterminateDialog.setMessage("Recognize text using Cube and Tesseract!");
        } else {
            indeterminateDialog.setMessage("Recognize text using " + ocrEngineModeName + "!");
        }
        indeterminateDialog.setCancelable(false);
        indeterminateDialog.show();
    }

    ProgressDialog getProgressDialog() {
        return indeterminateDialog;
    }

    void showErrorMessage(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setOnCancelListener(new FinishListener(this)).setPositiveButton( "Done!", new FinishListener(this)).show();
    }

    private class Translate extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progress = null;

        protected void onError(Exception ex) {

        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                translator = new GoogleTranslate("AIzaSyD7aQ--zucjX7lckDRLY8dGI8d_k96AMAs");

                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(MainActivity.this, "Processing", "Please wait !");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void result) {
            progress.dismiss();

            super.onPostExecute(result);
            translated();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

    }

    public void translated() {

        String translateText = ocrResultView.getText().toString();
        String result = translator.translate(translateText, "en", "vi");
        translationView.setText(result);
        int scaledSize = Math.max(22, 32 - result.length() / 4);
        translationView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);

    }

}
