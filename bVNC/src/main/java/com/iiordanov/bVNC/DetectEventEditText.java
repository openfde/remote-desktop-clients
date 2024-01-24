package com.iiordanov.bVNC;

import static android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS;
import static android.view.KeyEvent.KEYCODE_0;
import static android.view.KeyEvent.KEYCODE_1;
import static android.view.KeyEvent.KEYCODE_9;
import static android.view.KeyEvent.KEYCODE_NUMPAD_0;
import static android.view.KeyEvent.KEYCODE_NUMPAD_1;
import static android.view.KeyEvent.KEYCODE_NUMPAD_9;

import static com.iiordanov.bVNC.RemoteCanvasActivity.INPUT_MODE_ONLY_KEYBOARD;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

import java.util.LinkedList;

public class DetectEventEditText extends EditText {
    private static final String TAG = "DetectText_ime";
    public CharSequence commitText;
    private static final boolean DEBUG = true;
    private RemoteCanvas canvas;
    private int mInputModeFlag = INPUT_MODE_ONLY_KEYBOARD;
    private DetectInputConnection detectInputConnection;
    public DetectEventEditText(Context context) {
        super(context);
        setKeyListener(getDefaultKeyListener());
    }

    public DetectEventEditText(Context context,
                               AttributeSet attrs) {
        super(context, attrs);
        setKeyListener(getDefaultKeyListener());
    }

    public DetectEventEditText(Context context,
                               AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setKeyListener(getDefaultKeyListener());
    }


    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        super.onCreateInputConnection(outAttrs);
        detectInputConnection = new DetectInputConnection(this);
        outAttrs.initialSelStart = getSelectionStart();
        outAttrs.initialSelEnd = getSelectionEnd();
        outAttrs.initialCapsMode = detectInputConnection.getCursorCapsMode(getInputType());
        detectInputConnection.setInputMode(mInputModeFlag);
        return detectInputConnection;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if(DEBUG){
            Log.d(TAG, "onKeyPreIme() called with: keyCode = [" + keyCode + "], event = [" + event + "]");
        }
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        Log.d(TAG, "dispatchKeyEventPreIme() called with: event = [" + event + "]");
        if(mInputModeFlag == INPUT_MODE_ONLY_KEYBOARD &&
                (event.getKeyCode() < KEYCODE_0 ||
                ( event.getKeyCode() > KEYCODE_9 && event.getKeyCode() < KEYCODE_NUMPAD_0 ) ||
                event.getKeyCode() > KEYCODE_NUMPAD_9)){
            canvas.getKeyboard().keyEvent(event.getKeyCode(), event);
        }
        return super.dispatchKeyEventPreIme(event);
    }

    private KeyListener getDefaultKeyListener() {
        return new KeyListener() {
            @Override
            public int getInputType() {
                return TYPE_TEXT_FLAG_CAP_WORDS;
            }

            @Override
            public boolean onKeyDown(View view, Editable text, int keyCode, KeyEvent event) {
                Log.d(TAG, "onKeyDown text = [" + text + "], keyCode = [" + keyCode + "], event = [" + event + "]");
                return false;
            }

            //FIXME never called
            @Override
            public boolean onKeyUp(View view, Editable text, int keyCode, KeyEvent event) {
                return false;
            }

            @Override
            public boolean onKeyOther(View view, Editable text, KeyEvent event) {
                Log.d(TAG, "onKeyOther text = [" + text + "], event = [" + event + "]");
                return false;
            }

            @Override
            public void clearMetaKeyState(View view, Editable content, int states) {
            }
        };
    }

    public void connect2canvas(RemoteCanvas canvas) {
        this.canvas = canvas;
    }

    public RemoteCanvas getCanvas(){
        return this.canvas;
    }

    public void setInputMode(int inputModeFlag) {
        this.mInputModeFlag = inputModeFlag;
        if(detectInputConnection != null){
            detectInputConnection.setInputMode(inputModeFlag);
        }
    }
}