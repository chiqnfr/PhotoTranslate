package corbatodd.phototranslate;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Created by CorbaTodd on 5/25/15.
 */
public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    public static final String KEY_SOURCE_LANGUAGE_PREFERENCE = "sourceLanguageCodeOcrPref";
    public static final String KEY_TARGET_LANGUAGE_PREFERENCE = "targetLanguageCodeTranslationPref";
    public static final String KEY_CONTINUOUS_PREVIEW = "preference_capture_continuous";
    public static final String KEY_PAGE_SEGMENTATION_MODE = "preference_page_segmentation_mode";
    public static final String KEY_OCR_ENGINE_MODE = "preference_ocr_engine_mode";
    public static final String KEY_CHARACTER_BLACKLIST = "preference_character_blacklist";
    public static final String KEY_CHARACTER_WHITELIST = "preference_character_whitelist";
    public static final String KEY_TRANSLATOR = "preference_translator";
    public static final String KEY_AUTO_FOCUS = "preferences_auto_focus";
    public static final String KEY_DISABLE_CONTINUOUS_FOCUS = "preferences_disable_continuous_focus";
    private ListPreference listPreferenceSourceLanguage;
    private ListPreference listPreferenceTargetLanguage;
    private ListPreference listPreferenceTranslator;
    private ListPreference listPreferenceOcrEngineMode;
    private EditTextPreference editTextPreferenceCharacterBlacklist;
    private EditTextPreference editTextPreferenceCharacterWhitelist;
    private ListPreference listPreferencePageSegmentationMode;

    private static SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        listPreferenceSourceLanguage = (ListPreference) getPreferenceScreen().findPreference(KEY_SOURCE_LANGUAGE_PREFERENCE);
        listPreferenceTargetLanguage = (ListPreference) getPreferenceScreen().findPreference(KEY_TARGET_LANGUAGE_PREFERENCE);
        listPreferenceTranslator = (ListPreference) getPreferenceScreen().findPreference(KEY_TRANSLATOR);
        listPreferenceOcrEngineMode = (ListPreference) getPreferenceScreen().findPreference(KEY_OCR_ENGINE_MODE);
        editTextPreferenceCharacterBlacklist = (EditTextPreference) getPreferenceScreen().findPreference(KEY_CHARACTER_BLACKLIST);
        editTextPreferenceCharacterWhitelist = (EditTextPreference) getPreferenceScreen().findPreference(KEY_CHARACTER_WHITELIST);
        listPreferencePageSegmentationMode = (ListPreference) getPreferenceScreen().findPreference(KEY_PAGE_SEGMENTATION_MODE);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_TRANSLATOR)) {
            listPreferenceTranslator.setSummary(sharedPreferences.getString(key, MainActivity.DEFAULT_TRANSLATOR));
        } else if(key.equals(KEY_SOURCE_LANGUAGE_PREFERENCE)) {

            listPreferenceSourceLanguage.setSummary(Language.getOcrLanguageName(getBaseContext(), sharedPreferences.getString(key, MainActivity.DEFAULT_SOURCE_LANGUAGE_CODE)));

            String blacklist = OCRCharacter.getBlacklist(sharedPreferences, listPreferenceSourceLanguage.getValue());
            String whitelist = OCRCharacter.getWhitelist(sharedPreferences, listPreferenceSourceLanguage.getValue());

            sharedPreferences.edit().putString(KEY_CHARACTER_BLACKLIST, blacklist).commit();
            sharedPreferences.edit().putString(KEY_CHARACTER_WHITELIST, whitelist).commit();

            editTextPreferenceCharacterBlacklist.setSummary(blacklist);
            editTextPreferenceCharacterWhitelist.setSummary(whitelist);

        } else if (key.equals(KEY_TARGET_LANGUAGE_PREFERENCE)) {
            listPreferenceTargetLanguage.setSummary(Language.getTranslationLanguageName(this, sharedPreferences.getString(key, MainActivity.DEFAULT_TARGET_LANGUAGE_CODE)));
        } else if (key.equals(KEY_PAGE_SEGMENTATION_MODE)) {
            listPreferencePageSegmentationMode.setSummary(sharedPreferences.getString(key, MainActivity.DEFAULT_PAGE_SEGMENTATION_MODE));
        } else if (key.equals(KEY_OCR_ENGINE_MODE)) {
            listPreferenceOcrEngineMode.setSummary(sharedPreferences.getString(key, MainActivity.DEFAULT_OCR_ENGINE_MODE));
        } else if (key.equals(KEY_CHARACTER_BLACKLIST)) {

            OCRCharacter.setBlacklist(sharedPreferences, listPreferenceSourceLanguage.getValue(), sharedPreferences.getString(key, OCRCharacter.getDefaultBlacklist(listPreferenceSourceLanguage.getValue())));

            editTextPreferenceCharacterBlacklist.setSummary(sharedPreferences.getString(key, OCRCharacter.getDefaultBlacklist(listPreferenceSourceLanguage.getValue())));

        } else if (key.equals(KEY_CHARACTER_WHITELIST)) {

            OCRCharacter.setWhitelist(sharedPreferences, listPreferenceSourceLanguage.getValue(), sharedPreferences.getString(key, OCRCharacter.getDefaultWhitelist(listPreferenceSourceLanguage.getValue())));

            editTextPreferenceCharacterWhitelist.setSummary(sharedPreferences.getString(key, OCRCharacter.getDefaultWhitelist(listPreferenceSourceLanguage.getValue())));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        listPreferenceTranslator.setSummary(sharedPreferences.getString(KEY_TRANSLATOR, MainActivity.DEFAULT_TRANSLATOR));
        listPreferenceSourceLanguage.setSummary(Language.getOcrLanguageName(getBaseContext(), sharedPreferences.getString(KEY_SOURCE_LANGUAGE_PREFERENCE, MainActivity.DEFAULT_SOURCE_LANGUAGE_CODE)));
        listPreferenceTargetLanguage.setSummary(Language.getTranslationLanguageName(getBaseContext(), sharedPreferences.getString(KEY_TARGET_LANGUAGE_PREFERENCE, MainActivity.DEFAULT_TARGET_LANGUAGE_CODE)));
        listPreferencePageSegmentationMode.setSummary(sharedPreferences.getString(KEY_PAGE_SEGMENTATION_MODE, MainActivity.DEFAULT_PAGE_SEGMENTATION_MODE));
        listPreferenceOcrEngineMode.setSummary(sharedPreferences.getString(KEY_OCR_ENGINE_MODE, MainActivity.DEFAULT_OCR_ENGINE_MODE));
        editTextPreferenceCharacterBlacklist.setSummary(sharedPreferences.getString(KEY_CHARACTER_BLACKLIST, OCRCharacter.getDefaultBlacklist(listPreferenceSourceLanguage.getValue())));
        editTextPreferenceCharacterWhitelist.setSummary(sharedPreferences.getString(KEY_CHARACTER_WHITELIST, OCRCharacter.getDefaultWhitelist(listPreferenceSourceLanguage.getValue())));

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

}
