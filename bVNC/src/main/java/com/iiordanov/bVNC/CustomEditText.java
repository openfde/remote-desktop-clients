package com.iiordanov.bVNC;

import static android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.TextView;

public class CustomEditText extends androidx.appcompat.widget.AppCompatTextView {

    private int previousInputType;
    private String TAG  = "CustiomEditText";

    public CustomEditText(Context context) {
        super(context);
        setKeyListener(getDefaultKeyListener());
    }

    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setKeyListener(getDefaultKeyListener());
        addTextWatcher();
        addEditListener();
    }

    private void addEditListener() {
        setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d(TAG, "onEditorAction() actionId = [" + actionId + "], event = [" + event + "]");
                if(actionId == EditorInfo.IME_ACTION_GO){
                    return true;
                }
                return false;
            }
        });
    }

    public CustomEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setKeyListener(getDefaultKeyListener());
        addEditListener();
        addTextWatcher();
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyPreIme() called with: keyCode = [" + keyCode + "], event = [" + event + "]");
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        Log.d(TAG, "onKeyMultiple() called with: keyCode = [" + keyCode + "], repeatCount = [" + repeatCount + "], event = [" + event + "]");
        return super.onKeyMultiple(keyCode, repeatCount, event);
    }

    private void addTextWatcher() {
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // 不需要处理
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // 不需要处理
            }

            @Override
            public void afterTextChanged(Editable editable) {
                int currentInputType = getInputType();
                Log.d(TAG, "afterTextChanged() called with: currentInputType = [" + currentInputType + "]");
                if (currentInputType != previousInputType) {
                    previousInputType = currentInputType;
                }
            }
        });
    }


    private KeyListener getDefaultKeyListener() {
        return new KeyListener() {
            @Override
            public int getInputType() {
                // 返回你的EditText所需的输入类型
                return TYPE_TEXT_FLAG_CAP_WORDS;
            }

            @Override
            public boolean onKeyDown(View view, Editable text, int keyCode, KeyEvent event) {
                Log.d(TAG, "onKeyDown text = [" + text + "], keyCode = [" + keyCode + "], event = [" + event + "]");
                // 处理按键按下事件
                return false;
            }

            @Override
            public boolean onKeyUp(View view, Editable text, int keyCode, KeyEvent event) {
                String characters = event.getCharacters();
                Log.d(TAG, "onKeyUp text = [" + text + "], keyCode = [" + keyCode + "], characters = [" + characters + "]");
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
                Log.d(TAG, "clearMetaKeyState content = [" + content + "], states = [" + states + "]");
                // 清除meta键状态
            }
        };
    }

//    @Override
//    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
//        // outAttrs就是我们需要设置的输入法的各种类型最重要的就是:
////        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI;
////        outAttrs.inputType = InputType.TYPE_NULL;
//        return new ProxyInputConnection(this, true);
//    }

    public class ProxyInputConnection extends BaseInputConnection {

        /**
         * Initializes a wrapper.
         * <p>
         * <p><b>Caveat:</b> Although the system can accept {@code (InputConnection) null} in some
         * places, you cannot emulate such a behavior by non-null {@link InputConnectionWrapper} that
         * has {@code null} in {@code target}.</p>
         *
         * @param target  the {@link InputConnection} to be proxied.
         * @param mutable set {@code true} to protect this object from being reconfigured to target
         *                another {@link InputConnection}.  Note that this is ignored while the target is {@code null}.
         */
        public ProxyInputConnection(View target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            Log.d(TAG, "sendKeyEvent() called with: event = [" + event + "]");
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    Log.i(TAG, "delete");
                } else {
                }
            }
            return false;
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            Log.d(TAG, "deleteSurroundingText() called with: beforeLength = [" + beforeLength + "], afterLength = [" + afterLength + "]");
            if (beforeLength == 1 && afterLength == 0) {
                return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }
            return super.deleteSurroundingText(beforeLength, afterLength);
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            Log.d(TAG, "commitText() called with: text = [" + text + "], newCursorPosition = [" + newCursorPosition + "]");
//            mOnKeyEventListener.onAdd(text + "");
            return false;
        }
    }
}

