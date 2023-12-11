package com.iiordanov.bVNC;

import static android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS;

import android.app.Activity;
import android.content.Context;
import android.os.Debug;
import android.text.Editable;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.LinkedList;

public class DetectEventEditText extends EditText implements View.OnKeyListener,
        EditableInputConnection.OnDelEventListener {
    private static final String TAG = "DetectText hy";
    private DelEventListener delEventListener;
    public static CharSequence commitText;
    public static volatile LinkedList<CharSequence> commitTexts = new LinkedList<>();
    private static final boolean DEBUG = true;

    private int flag;

    public DetectEventEditText(Context context) {
        super(context);
        setKeyListener(getDefaultKeyListener());
        commitText = null;
        commitTexts.clear();
    }

    public DetectEventEditText(Context context,
                               AttributeSet attrs) {
        super(context, attrs);
        setKeyListener(getDefaultKeyListener());
        commitText = null;
        commitTexts.clear();
    }

    public DetectEventEditText(Context context,
                               AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setKeyListener(getDefaultKeyListener());
        commitText = null;
        commitTexts.clear();
    }


    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        super.onCreateInputConnection(outAttrs);
        EditableInputConnection editableInputConnection = new EditableInputConnection(this);
        outAttrs.initialSelStart = getSelectionStart();
        outAttrs.initialSelEnd = getSelectionEnd();
        outAttrs.initialCapsMode = editableInputConnection.getCursorCapsMode(getInputType());

        editableInputConnection.setDelEventListener(this);
        flag = 0;

        return editableInputConnection;
    }

    public void setDelListener(DelEventListener l) {
        delEventListener = l;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        Log.d(TAG, "onKey()  keyCode = [" + keyCode + "], event = [" + event + "]");
        if (flag == 2) {
            return false;
        }
        flag = 1;
        return delEventListener != null && keyCode == KeyEvent.KEYCODE_DEL && event
                .getAction() == KeyEvent.ACTION_DOWN && delEventListener.delEvent();
    }

    public boolean removeFirstChar(){
        if(commitTexts.size() == 0) return false;
        CharSequence charSequence = commitTexts.removeFirst();
        if(DEBUG){
            Log.d(TAG, String.format("removeFirstChar: (%s)", charSequence));
        }
        return true;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyPreIme() called with: keyCode = [" + keyCode + "], event = [" + event + "]");
        if(event.getAction() == KeyEvent.ACTION_DOWN){
//            commitText = null;
//            ((Activity)getContext()).dispatchKeyEvent(event);
        }
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        Log.d(TAG, "dispatchKeyEventPreIme() called with: event = [" + event + "]");
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
//                commitText = null;
//                String characters = event.getCharacters();
//                Log.d(TAG, "onKeyUp commitText = [" + commitText + "], keyCode = [" + keyCode + "], characters = [" + characters + "]");
                // 处理按键释放事件
                return false;
            }

            @Override
            public boolean onKeyOther(View view, Editable text, KeyEvent event) {
                Log.d(TAG, "onKeyOther text = [" + text + "], event = [" + event + "]");
                // 处理其他按键事件
                return false;
            }

            @Override
            public void clearMetaKeyState(View view, Editable content, int states) {
                // 清除meta键状态
            }
        };
    }

    @Override
    public boolean onDelEvent() {
        Log.d(TAG, "onDelEvent() called");
        if (flag == 1) {
            return false;
        }
        flag = 2;
        return delEventListener != null && delEventListener.delEvent();
    }

    public interface DelEventListener {
        boolean delEvent();
    }
}