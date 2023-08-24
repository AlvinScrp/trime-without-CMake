/*
 * Copyright (C) 2015-present, osfans
 * waxaca@163.com https://github.com/osfans
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.osfans.trime.ime.core;

import static android.graphics.Color.parseColor;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jk.ime.JK;
import com.osfans.trime.core.Rime;
import com.osfans.trime.data.AppPrefs;
import com.osfans.trime.data.Config;
import com.osfans.trime.databinding.InputRootBinding;
import com.osfans.trime.ime.broadcast.IntentReceiver;
import com.osfans.trime.ime.enums.Keycode;
import com.osfans.trime.ime.keyboard.Event;
import com.osfans.trime.ime.keyboard.Key;
import com.osfans.trime.ime.keyboard.Keyboard;
import com.osfans.trime.ime.keyboard.KeyboardSwitcher;
import com.osfans.trime.ime.keyboard.KeyboardView;
import com.osfans.trime.ime.lifecycle.LifecycleInputMethodService;
import com.osfans.trime.ime.text.Candidate;
import com.osfans.trime.ime.text.ScrollView;
import com.osfans.trime.ime.text.TextInputManager;
import com.osfans.trime.util.ShortcutUtils;
import com.osfans.trime.util.StringUtils;
import com.osfans.trime.util.ViewUtils;

import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import kotlin.jvm.Synchronized;
import timber.log.Timber;

/**
 * {@link InputMethodService 輸入法}主程序
 */
public class Trime extends LifecycleInputMethodService {
    private static Trime self = null;
    private boolean normalTextEditor;

    @NonNull
    private AppPrefs getPrefs() {
        return AppPrefs.Companion.defaultInstance();
    }

    /**
     * 输入法配置
     */
    @NonNull
    public Config getImeConfig() {
        return Config.get(this);
    }

    private KeyboardView mainKeyboardView; // 主軟鍵盤
    public KeyboardSwitcher keyboardSwitcher; // 键盘切换器

    private Candidate mCandidate; // 候選
    private ScrollView mCandidateRoot/**, mTabRoot**/
            ;
    public InputRootBinding inputRootBinding = null;
    public CopyOnWriteArrayList<EventListener> eventListeners = new CopyOnWriteArrayList<>();
    public InputMethodManager imeManager = null;
    private IntentReceiver mIntentReceiver = null;

    private boolean isWindowShown = false; // 键盘窗口是否已显示

    private boolean isAutoCaps; // 句首自動大寫
    private final Locale[] locales = new Locale[2];

    private int oneHandMode = 0; // 单手键盘模式
    public EditorInstance activeEditorInstance;
    public TextInputManager textInputManager; // 文字输入管理器

    @Synchronized
    @NonNull
    public static Trime getService() {
        assert self != null;
        return self;
    }

    @Synchronized
    @Nullable
    public static Trime getServiceOrNull() {
        return self;
    }

    public Trime() {
        try {
            self = this;
            textInputManager = TextInputManager.Companion.getInstance();
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }

    @Override
    public void onWindowShown() {
        super.onWindowShown();
        if (isWindowShown) {
            Timber.i("Ignoring (is already shown)");
            return;
        } else {
            Timber.i("onWindowShown...");
        }
        isWindowShown = true;

        updateComposing();

        for (EventListener listener : eventListeners) {
            if (listener != null) listener.onWindowShown();
        }
    }

    @Override
    public void onWindowHidden() {
        String methodName =
                "\t<TrimeInit>\t" + Thread.currentThread().getStackTrace()[2].getMethodName() + "\t";
        Timber.d(methodName);
        super.onWindowHidden();
        Timber.d(methodName + "super finish");
        if (!isWindowShown) {
            Timber.i("Ignoring (is already hidden)");
            return;
        } else {
            Timber.i("onWindowHidden...");
        }
        isWindowShown = false;

        Timber.d(methodName + "eventListeners");
        for (EventListener listener : eventListeners) {
            if (listener != null) listener.onWindowHidden();
        }
    }


    public void loadConfig() {
        final Config imeConfig = getImeConfig();
        textInputManager.setShouldResetAsciiMode(imeConfig.getBoolean("reset_ascii_mode"));
        isAutoCaps = imeConfig.getBoolean("auto_caps");
        textInputManager.setShouldUpdateRimeOption(true);
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean updateRimeOption() {
        try {
            if (textInputManager.getShouldUpdateRimeOption()) {
                Rime.setOption("soft_cursor", getPrefs().getKeyboard().getSoftCursorEnabled()); // 軟光標
                Rime.setOption("_horizontal", getImeConfig().getBoolean("horizontal")); // 水平模式
                textInputManager.setShouldUpdateRimeOption(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void onCreate() {

        StrictMode.setVmPolicy(
                new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                        .detectLeakedClosableObjects()
                        .build());
        String methodName =
                "\t<TrimeInit>\t" + Thread.currentThread().getStackTrace()[2].getMethodName() + "\t";
        Timber.d(methodName);
        // MUST WRAP all code within Service onCreate() in try..catch to prevent any crash loops
        try {
            // Additional try..catch wrapper as the event listeners chain or the super.onCreate() method
            // could crash
            //  and lead to a crash loop
            try {
                Timber.i("onCreate...");

                activeEditorInstance = new EditorInstance(this);
                imeManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                Timber.d(methodName + "keyboardSwitcher");
                keyboardSwitcher = new KeyboardSwitcher();
                clipBoardMonitor();
            } catch (Exception e) {
                e.printStackTrace();
                super.onCreate();
                return;
            }
            Timber.d(methodName + "super.onCreate()");
            super.onCreate();
            Timber.d(methodName + "create listener");
            for (EventListener listener : eventListeners) {
                if (listener != null) listener.onCreate();
            }
        } catch (Exception e) {
            e.fillInStackTrace();
        }
        Timber.d(methodName + "finish");
    }

    public void pasteByChar() {
        final ClipboardManager clipBoard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        final ClipData clipData = clipBoard.getPrimaryClip();

        final ClipData.Item item = clipData.getItemAt(0);
        if (item == null) return;

        final String text = item.coerceToText(self).toString();
        commitTextByChar(text);
    }

    public void invalidate() {
        Rime.get(this);
        getImeConfig().destroy();
        reset();
        textInputManager.setShouldUpdateRimeOption(true);
    }

    public void loadBackground() {
        final Config mConfig = getImeConfig();
        final int orientation = getResources().getConfiguration().orientation;
        if (mCandidateRoot != null) {
            final Drawable candidateBackground =
                    mConfig.getDrawable(
                            "candidate_background",
                            "candidate_border",
                            "candidate_border_color",
                            "candidate_border_round",
                            null);
            if (candidateBackground != null) mCandidateRoot.setBackground(candidateBackground);
        }

        if (inputRootBinding == null) return;

        int[] padding =
                mConfig.getKeyboardPadding(oneHandMode, orientation == Configuration.ORIENTATION_LANDSCAPE);
        Timber.i(
                "update KeyboardPadding: Trime.loadBackground, padding= %s %s %s, orientation=%s",
                padding[0], padding[1], padding[2], orientation);
        mainKeyboardView.setPadding(padding[0], 0, padding[1], padding[2]);

        final Drawable inputRootBackground = mConfig.getDrawable_("root_background");
        if (inputRootBackground != null) {
            inputRootBinding.inputRoot.setBackground(inputRootBackground);
        } else {
            // 避免因为键盘整体透明而造成的异常
            inputRootBinding.inputRoot.setBackgroundColor(Color.BLACK);
        }
    }

    public void resetKeyboard() {
        if (mainKeyboardView != null) {
            mainKeyboardView.setShowHint(!Rime.getOption("_hide_key_hint"));
            mainKeyboardView.setShowSymbol(!Rime.getOption("_hide_key_symbol"));
            mainKeyboardView.reset(this); // 實體鍵盤無軟鍵盤
        }
    }

    public void resetCandidate() {
        if (mCandidateRoot != null) {
            loadBackground();
            setShowComment(!Rime.getOption("_hide_comment"));
            mCandidateRoot.setVisibility(!Rime.getOption("_hide_candidate") ? View.VISIBLE : View.GONE);
            mCandidate.reset(this);
        }
    }

    /**
     * 重置鍵盤、候選條、狀態欄等 !!注意，如果其中調用Rime.setOption，切換方案會卡住
     */
    private void reset() {
        if (inputRootBinding == null) return;
        final LinearLayout mainInputView = inputRootBinding.main.mainInput;
        if (mainInputView != null) mainInputView.setVisibility(View.VISIBLE);
        getImeConfig().reset();
        loadConfig();
        getImeConfig().initCurrentColors();
        if (keyboardSwitcher != null) keyboardSwitcher.newOrReset();
        resetCandidate();
        resetKeyboard();
    }

    /**
     * Must be called on the UI thread
     */
    public void initKeyboard() {
        reset();
        textInputManager.setShouldUpdateRimeOption(true); // 不能在Rime.onMessage中調用set_option，會卡死
        bindKeyboardToInputView();
        // loadBackground(); // reset()调用过resetCandidate()，resetCandidate()一键调用过loadBackground();
        updateComposing(); // 切換主題時刷新候選
    }

    @Override
    public void onDestroy() {
        if (mIntentReceiver != null) mIntentReceiver.unregisterReceiver(this);
        mIntentReceiver = null;
        inputRootBinding = null;
        imeManager = null;


        for (EventListener listener : eventListeners) {
            if (listener != null) listener.onDestroy();
        }
        eventListeners.clear();
        super.onDestroy();

        self = null;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        final Configuration config = getResources().getConfiguration();
        if (config != null) {
            if (config.orientation != newConfig.orientation) {
                // Clear composing text and candidates for orientation change.
                performEscape();
                config.orientation = newConfig.orientation;
            }
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onUpdateSelection(
            int oldSelStart,
            int oldSelEnd,
            int newSelStart,
            int newSelEnd,
            int candidatesStart,
            int candidatesEnd) {
        super.onUpdateSelection(
                oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        if ((candidatesEnd != -1) && ((newSelStart != candidatesEnd) || (newSelEnd != candidatesEnd))) {
            // 移動光標時，更新候選區
            if ((newSelEnd < candidatesEnd) && (newSelEnd >= candidatesStart)) {
                final int n = newSelEnd - candidatesStart;
                Rime.RimeSetCaretPos(n);
                updateComposing();
            }
        }
        if ((candidatesStart == -1 && candidatesEnd == -1) && (newSelStart == 0 && newSelEnd == 0)) {
            // 上屏後，清除候選區
            performEscape();
        }
        // Update the caps-lock status for the current cursor position.
        dispatchCapsStateToInputView();
    }

    @Override
    public void onComputeInsets(InputMethodService.Insets outInsets) {
        super.onComputeInsets(outInsets);
        outInsets.contentTopInsets = outInsets.visibleTopInsets;
    }

    @Override
    public View onCreateInputView() {
        Timber.e("onCreateInputView()");
        // 初始化键盘布局
        super.onCreateInputView();
        inputRootBinding = InputRootBinding.inflate(LayoutInflater.from(this));
        mainKeyboardView = inputRootBinding.main.mainKeyboardView;

        // 初始化候选栏
        mCandidateRoot = inputRootBinding.main.candidateView.candidateRoot;
        mCandidate = inputRootBinding.main.candidateView.candidates;

        for (EventListener listener : eventListeners) {
            assert inputRootBinding != null;
            if (listener != null) listener.onInitializeInputUi(inputRootBinding);
        }
        JK.INSTANCE.init(inputRootBinding.main);
        getImeConfig().initCurrentColors();
        loadBackground();

//        if (keyboardSwitcher != null) keyboardSwitcher.newOrReset();
        Timber.i("onCreateInputView() finish");

        return inputRootBinding.inputRoot;
    }

    public void setShowComment(boolean show_comment) {
        // if (mCandidateRoot != null) mCandidate.setShowComment(show_comment);
//        mComposition.setShowComment(show_comment);
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        for (EventListener listener : eventListeners) {
            if (listener != null) listener.onStartInputView(activeEditorInstance, restarting);
        }
        bindKeyboardToInputView();
        setCandidatesViewShown(!Rime.isEmpty()); // 軟鍵盤出現時顯示候選欄

        if ((attribute.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION)
                == EditorInfo.IME_FLAG_NO_ENTER_ACTION) {
            mainKeyboardView.resetEnterLabel();
        } else {
            mainKeyboardView.setEnterLabel(
                    attribute.imeOptions & EditorInfo.IME_MASK_ACTION, attribute.actionLabel);
        }

        switch (attribute.inputType & InputType.TYPE_MASK_VARIATION) {
            case InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
            case InputType.TYPE_TEXT_VARIATION_PASSWORD:
            case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
            case InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
            case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:
                Timber.i(
                        "EditorInfo: private;"
                                + " packageName="
                                + attribute.packageName
                                + "; fieldName="
                                + attribute.fieldName
                                + "; actionLabel="
                                + attribute.actionLabel
                                + "; inputType="
                                + attribute.inputType
                                + "; VARIATION="
                                + (attribute.inputType & InputType.TYPE_MASK_VARIATION)
                                + "; CLASS="
                                + (attribute.inputType & InputType.TYPE_MASK_CLASS)
                                + "; ACTION="
                                + (attribute.imeOptions & EditorInfo.IME_MASK_ACTION));
                normalTextEditor = false;
                break;

            default:
                Timber.i(
                        "EditorInfo: normal;"
                                + " packageName="
                                + attribute.packageName
                                + "; fieldName="
                                + attribute.fieldName
                                + "; actionLabel="
                                + attribute.actionLabel
                                + "; inputType="
                                + attribute.inputType
                                + "; VARIATION="
                                + (attribute.inputType & InputType.TYPE_MASK_VARIATION)
                                + "; CLASS="
                                + (attribute.inputType & InputType.TYPE_MASK_CLASS)
                                + "; ACTION="
                                + (attribute.imeOptions & EditorInfo.IME_MASK_ACTION));

                if ((attribute.imeOptions & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
                        == EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) {
                    //  应用程求以隐身模式打开键盘应用程序
                    Timber.i("EditorInfo: normal -> private, IME_FLAG_NO_PERSONALIZED_LEARNING");
                } else {
                    normalTextEditor = true;
                    activeEditorInstance.cacheDraft();
                    addDraft();
                }
        }
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        if (normalTextEditor) addDraft();
        super.onFinishInputView(finishingInput);
        // Dismiss any pop-ups when the input-view is being finished and hidden.
        mainKeyboardView.closing();
        performEscape();
    }

    public void bindKeyboardToInputView() {
        if (mainKeyboardView != null) {
            // Bind the selected keyboard to the input view.
            Keyboard sk = keyboardSwitcher.getCurrentKeyboard();
            mainKeyboardView.setKeyboard(sk);
            dispatchCapsStateToInputView();
        }
    }

    /**
     * Dispatches cursor caps info to input view in order to implement auto caps lock at the start of
     * a sentence.
     */
    private void dispatchCapsStateToInputView() {
        if ((isAutoCaps && Rime.isAsciiMode())
                && (mainKeyboardView != null && !mainKeyboardView.isCapsOn())) {
            mainKeyboardView.setShifted(false, activeEditorInstance.getCursorCapsMode() != 0);
        }
    }

    private boolean isComposing() {
        return Rime.isComposing();
    }

    public void commitText(String text) {
        activeEditorInstance.commitText(text, true);
    }

    public void commitTextByChar(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!activeEditorInstance.commitText(text.substring(i, i + 1))) break;
        }
    }

    /**
     * 如果爲{@link KeyEvent#KEYCODE_BACK Back鍵}，則隱藏鍵盤
     *
     * @param keyCode {@link KeyEvent#getKeyCode() 鍵碼}
     * @return 是否處理了Back鍵事件
     */
    private boolean handleBack(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            requestHideSelf(0);
            return true;
        }
        return false;
    }

    public boolean onRimeKey(int[] event) {
        updateRimeOption();
        // todo 改为异步处理按键事件、刷新UI
        final boolean ret = Rime.onKey(event);
        activeEditorInstance.commitRimeText();
        return ret;
    }

    private boolean composeEvent(@NonNull KeyEvent event) {
        final int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_MENU) return false; // 不處理 Menu 鍵
        if (Keycode.Companion.hasSymbolLabel(keyCode)) return false; // 只處理安卓標準按鍵
        if (event.getRepeatCount() == 0 && Key.isTrimeModifierKey(keyCode)) {
            boolean ret =
                    onRimeKey(
                            Event.getRimeEvent(
                                    keyCode, event.getAction() == KeyEvent.ACTION_DOWN ? 0 : Rime.META_RELEASE_ON));
            if (isComposing()) setCandidatesViewShown(textInputManager.isComposable()); // 藍牙鍵盤打字時顯示候選欄
            return ret;
        }
        return textInputManager.isComposable() && !Rime.isVoidKeycode(keyCode);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Timber.i("\t<TrimeInput>\tonKeyDown()\tkeycode=%d, event=%s", keyCode, event.toString());
        if (composeEvent(event) && onKeyEvent(event)) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Timber.i("\t<TrimeInput>\tonKeyUp()\tkeycode=%d, event=%s", keyCode, event.toString());
        if (composeEvent(event) && textInputManager.getNeedSendUpRimeKey()) {
            textInputManager.onRelease(keyCode);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 處理實體鍵盤事件
     *
     * @param event {@link KeyEvent 按鍵事件}
     * @return 是否成功處理
     */
    // KeyEvent 处理实体键盘事件
    private boolean onKeyEvent(@NonNull KeyEvent event) {
        Timber.i("\t<TrimeInput>\tonKeyEvent()\tRealKeyboard event=%s", event.toString());
        int keyCode = event.getKeyCode();
        textInputManager.setNeedSendUpRimeKey(Rime.isComposing());
        if (!isComposing()) {
            if (keyCode == KeyEvent.KEYCODE_DEL
                    || keyCode == KeyEvent.KEYCODE_ENTER
                    || keyCode == KeyEvent.KEYCODE_ESCAPE
                    || keyCode == KeyEvent.KEYCODE_BACK) {
                return false;
            }
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            keyCode = KeyEvent.KEYCODE_ESCAPE; // 返回鍵清屏
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.isCtrlPressed()
                && event.getRepeatCount() == 0
                && !KeyEvent.isModifierKey(keyCode)) {
            if (hookKeyboard(keyCode, event.getMetaState())) return true;
        }

        final int unicodeChar = event.getUnicodeChar();
        final String s = String.valueOf((char) unicodeChar);
        final int i = Event.getClickCode(s);
        int mask = 0;
        if (i > 0) {
            keyCode = i;
        } else { // 空格、回車等
            mask = event.getMetaState();
        }
        final boolean ret = handleKey(keyCode, mask);
        if (isComposing()) setCandidatesViewShown(textInputManager.isComposable()); // 藍牙鍵盤打字時顯示候選欄
        return ret;
    }

    public void switchToPrevIme() {
        try {
            if (VERSION.SDK_INT >= VERSION_CODES.P) {
                switchToPreviousInputMethod();
            } else {
                Window window = getWindow().getWindow();
                if (window != null) {
                    if (imeManager != null) {
                        imeManager.switchToLastInputMethod(window.getAttributes().token);
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Unable to switch to the previous IME.");
            if (imeManager != null) {
                imeManager.showInputMethodPicker();
            }
        }
    }

    public void switchToNextIme() {
        try {
            if (VERSION.SDK_INT >= VERSION_CODES.P) {
                switchToNextInputMethod(false);
            } else {
                Window window = getWindow().getWindow();
                if (window != null) {
                    if (imeManager != null) {
                        imeManager.switchToNextInputMethod(window.getAttributes().token, false);
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Unable to switch to the next IME.");
            if (imeManager != null) {
                imeManager.showInputMethodPicker();
            }
        }
    }

    // 处理键盘事件(Android keycode)
    public boolean handleKey(int keyEventCode, int metaState) { // 軟鍵盤
        textInputManager.setNeedSendUpRimeKey(false);
        if (onRimeKey(Event.getRimeEvent(keyEventCode, metaState))) {
            // 如果输入法消费了按键事件，则需要释放按键
            textInputManager.setNeedSendUpRimeKey(true);
            Timber.d(
                    "\t<TrimeInput>\thandleKey()\trimeProcess, keycode=%d, metaState=%d",
                    keyEventCode, metaState);
        } else if (hookKeyboard(keyEventCode, metaState)) {
            Timber.d("\t<TrimeInput>\thandleKey()\thookKeyboard, keycode=%d", keyEventCode);
        } else if (performEnter(keyEventCode) || handleBack(keyEventCode)) {
            // 处理返回键（隐藏软键盘）和回车键（换行）
            // todo 确认是否有必要单独处理回车键？是否需要把back和escape全部占用？
            Timber.d("\t<TrimeInput>\thandleKey()\tEnterOrHide, keycode=%d", keyEventCode);
        } else if (ShortcutUtils.INSTANCE.openCategory(keyEventCode)) {
            // 打开系统默认应用
            Timber.d("\t<TrimeInput>\thandleKey()\topenCategory keycode=%d", keyEventCode);
        } else {
            textInputManager.setNeedSendUpRimeKey(true);
            Timber.d(
                    "\t<TrimeInput>\thandleKey()\treturn FALSE, keycode=%d, metaState=%d",
                    keyEventCode, metaState);
            return false;
        }
        return true;
    }

    public boolean shareText() {
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            final @Nullable InputConnection ic = getCurrentInputConnection();
            if (ic == null) return false;
            CharSequence cs = ic.getSelectedText(0);
            if (cs == null) ic.performContextMenuAction(android.R.id.selectAll);
            return ic.performContextMenuAction(android.R.id.shareText);
        }
        return false;
    }

    private boolean hookKeyboard(int code, int mask) { // 編輯操作
        final @Nullable InputConnection ic = getCurrentInputConnection();
        if (ic == null) return false;
        if (Event.hasModifier(mask, KeyEvent.META_CTRL_ON)) {

            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                if (getPrefs().getKeyboard().getHookCtrlZY()) {
                    switch (code) {
                        case KeyEvent.KEYCODE_Y:
                            return ic.performContextMenuAction(android.R.id.redo);
                        case KeyEvent.KEYCODE_Z:
                            return ic.performContextMenuAction(android.R.id.undo);
                    }
                }
            }
            switch (code) {
                case KeyEvent.KEYCODE_A:
                    if (getPrefs().getKeyboard().getHookCtrlA())
                        return ic.performContextMenuAction(android.R.id.selectAll);
                    return false;
                case KeyEvent.KEYCODE_X:
                    if (getPrefs().getKeyboard().getHookCtrlCV()) {
                        ExtractedTextRequest etr = new ExtractedTextRequest();
                        etr.token = 0;
                        ExtractedText et = ic.getExtractedText(etr, 0);
                        if (et != null) {
                            if (et.selectionEnd - et.selectionStart > 0)
                                return ic.performContextMenuAction(android.R.id.cut);
                        }
                    }
                    Timber.i("hookKeyboard cut fail");
                    return false;
                case KeyEvent.KEYCODE_C:
                    if (getPrefs().getKeyboard().getHookCtrlCV()) {
                        ExtractedTextRequest etr = new ExtractedTextRequest();
                        etr.token = 0;
                        ExtractedText et = ic.getExtractedText(etr, 0);
                        if (et != null) {
                            if (et.selectionEnd - et.selectionStart > 0)
                                return ic.performContextMenuAction(android.R.id.copy);
                        }
                    }
                    Timber.i("hookKeyboard copy fail");
                    return false;
                case KeyEvent.KEYCODE_V:
                    if (getPrefs().getKeyboard().getHookCtrlCV()) {
                        ExtractedTextRequest etr = new ExtractedTextRequest();
                        etr.token = 0;
                        ExtractedText et = ic.getExtractedText(etr, 0);
                        if (et == null) {
                            Timber.d("hookKeyboard paste, et == null, try commitText");
                            if (ic.commitText(ShortcutUtils.INSTANCE.pasteFromClipboard(self), 1)) {
                                return true;
                            }
                        } else if (ic.performContextMenuAction(android.R.id.paste)) {
                            return true;
                        }
                        Timber.w("hookKeyboard paste fail");
                    }
                    return false;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (getPrefs().getKeyboard().getHookCtrlLR()) {
                        ExtractedTextRequest etr = new ExtractedTextRequest();
                        etr.token = 0;
                        ExtractedText et = ic.getExtractedText(etr, 0);
                        if (et != null) {
                            int move_to =
                                    StringUtils.INSTANCE.findNextSection(et.text, et.startOffset + et.selectionEnd);
                            ic.setSelection(move_to, move_to);
                            return true;
                        }
                        break;
                    }
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (getPrefs().getKeyboard().getHookCtrlLR()) {
                        ExtractedTextRequest etr = new ExtractedTextRequest();
                        etr.token = 0;
                        ExtractedText et = ic.getExtractedText(etr, 0);
                        if (et != null) {
                            int move_to =
                                    StringUtils.INSTANCE.findPrevSection(et.text, et.startOffset + et.selectionStart);
                            ic.setSelection(move_to, move_to);
                            return true;
                        }
                        break;
                    }
            }
        }
        return false;
    }



    /**
     * 更新Rime的中西文狀態、編輯區文本
     */
    public int updateComposing() {
        activeEditorInstance.updateComposingText();
        int startNum = 0;
        if (mCandidateRoot != null) {
                mCandidate.setText(0);
            mCandidate.setExpectWidth(mainKeyboardView.getWidth()*5);
        }
        if (mainKeyboardView != null) mainKeyboardView.invalidateComposingKeys();
        if (!onEvaluateInputViewShown())
            setCandidatesViewShown(textInputManager.isComposable()); // 實體鍵盤打字時顯示候選欄
        JK.INSTANCE.onUpdateComposing(Rime.getCandidates() != null);

        return startNum;
    }



    /**
     * 如果爲{@link KeyEvent#KEYCODE_ENTER 回車鍵}，則換行
     *
     * @param keyCode {@link KeyEvent#getKeyCode() 鍵碼}
     * @return 是否處理了回車事件
     */
    private boolean performEnter(int keyCode) { // 回車
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            activeEditorInstance.cacheDraft();
            if (textInputManager.getPerformEnterAsLineBreak()) {
                commitText("\n");
            } else {
                sendKeyChar('\n');
            }
            return true;
        }
        return false;
    }

    /**
     * 模擬PC鍵盤中Esc鍵的功能：清除輸入的編碼和候選項
     */
    private void performEscape() {
        if (isComposing()) textInputManager.onKey(KeyEvent.KEYCODE_ESCAPE, 0);
    }



    @Override
    public boolean onEvaluateFullscreenMode() {
        final Configuration config = getResources().getConfiguration();
        if (config != null) {
            if (config.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                return false;
            } else {
                switch (getPrefs().getKeyboard().getFullscreenMode()) {
                    case AUTO_SHOW:
                        final EditorInfo ei = getCurrentInputEditorInfo();
                        if (ei != null && (ei.imeOptions & EditorInfo.IME_FLAG_NO_FULLSCREEN) != 0) {
                            return false;
                        }
                    case ALWAYS_SHOW:
                        return true;
                    case NEVER_SHOW:
                        return false;
                }
            }
        }
        return false;
    }

    @Override
    public void updateFullscreenMode() {
        super.updateFullscreenMode();
        updateSoftInputWindowLayoutParameters();
    }

    /**
     * Updates the layout params of the window and input view.
     */
    private void updateSoftInputWindowLayoutParameters() {
        final Window w = getWindow().getWindow();
        if (w == null) return;
        final LinearLayout inputRoot = inputRootBinding != null ? inputRootBinding.inputRoot : null;
        if (inputRoot != null) {
            final int layoutHeight =
                    isFullscreenMode()
                            ? WindowManager.LayoutParams.WRAP_CONTENT
                            : WindowManager.LayoutParams.MATCH_PARENT;
            final View inputArea = w.findViewById(android.R.id.inputArea);
            // TODO: 需要获取到文本编辑框、完成按钮，设置其色彩和尺寸。
            if (isFullscreenMode()) {
                Timber.i("isFullscreenMode");
                /* In Fullscreen mode, when layout contains transparent color,
                 * the background under input area will disturb users' typing,
                 * so set the input area as light pink */
                inputArea.setBackgroundColor(parseColor("#ff660000"));
            } else {
                Timber.i("NotFullscreenMode");
                /* Otherwise, set it as light gray to avoid potential issue */
                inputArea.setBackgroundColor(parseColor("#dddddddd"));
            }

            ViewUtils.updateLayoutHeightOf(inputArea, layoutHeight);
            ViewUtils.updateLayoutGravityOf(inputArea, Gravity.BOTTOM);
            ViewUtils.updateLayoutHeightOf(inputRoot, layoutHeight);
        }
    }

    public boolean addEventListener(@NonNull EventListener listener) {
        return eventListeners.add(listener);
    }

    public boolean removeEventListener(@NonNull EventListener listener) {
        return eventListeners.remove(listener);
    }

    public interface EventListener {
        default void onCreate() {
        }

        default void onInitializeInputUi(@NonNull InputRootBinding uiBinding) {
        }

        default void onDestroy() {
        }

        default void onStartInputView(@NonNull EditorInstance instance, boolean restarting) {
        }

        default void osFinishInputView(boolean finishingInput) {
        }

        default void onWindowShown() {
        }

        default void onWindowHidden() {
        }

        default void onUpdateSelection() {
        }
    }

    private String ClipBoardString = "";

    /**
     * 此方法设置监听剪贴板变化，如有新的剪贴内容，就启动选定的剪贴板管理器
     *
     * <p>ClipBoardCompare 比较规则。每次通知剪贴板管理器，都会保存 ClipBoardCompare 处理过的 string。如果两次处理过的内容不变，则不通知。
     * ClipBoardOut 输出规则。如果剪贴板内容与规则匹配，则不通知剪贴板管理器。
     */
    private void clipBoardMonitor() {
        final ClipboardManager clipBoard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        final Config imeConfig = getImeConfig();
        clipBoard.addPrimaryClipChangedListener(
                () -> {
                    if (imeConfig.getClipboardLimit() != 0) {
                        final ClipData clipData = clipBoard.getPrimaryClip();
                        if (clipData == null) return;
                        final ClipData.Item item = clipData.getItemAt(0);
                        if (item == null) return;

                        final String rawText = item.coerceToText(self).toString();
                        final String filteredText =
                                StringUtils.replace(rawText, imeConfig.getClipBoardCompare());
                        if (filteredText.length() < 1 || filteredText.equals(ClipBoardString)) return;

                        if (StringUtils.mismatch(rawText, imeConfig.getClipBoardOutput())) {
                            ClipBoardString = filteredText;
//                            liquidKeyboard.addClipboardData(rawText);
                        }
                    }
                });
    }

    private String draftString = "", draftCache = "";

    private void addDraft() {
        draftCache = activeEditorInstance.getDraftCache();
        if (draftCache.isEmpty() || draftCache.trim().equals(draftString)) return;

        if (getImeConfig().getDraftLimit() != 0) {
            Timber.i("addDraft() cache=%s, string=%s", draftString, draftCache);
            if (StringUtils.mismatch(draftCache, getImeConfig().getDraftOutput())) {
                draftString = draftCache.trim();
            }
        }
    }
}
