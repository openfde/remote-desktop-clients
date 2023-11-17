package com.iiordanov.bVNC;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.KeyListener;
import android.text.style.SuggestionSpan;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

public class EditableInputConnection extends BaseInputConnection {
    private static final boolean DEBUG = true;
    private static final String  TAG   = "DetectConnection hy";

    private final DetectEventEditText mTextView;

    // Keeps track of nested begin/end batch edit to ensure this connection always has a
    // balanced impact on its associated TextView.
    // A negative value means that this connection has been finished by the InputMethodManager.
    private int mBatchEditNesting;

    private final InputMethodManager mIMM;

    private OnDelEventListener delEventListener;

    public EditableInputConnection(DetectEventEditText textview) {
        super(textview, true);
        mTextView = textview;
        mIMM = (InputMethodManager) textview.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public Editable getEditable() {
        TextView tv = mTextView;
        if (tv != null) {
            return tv.getEditableText();
        }
        return null;
    }

    @Override
    public boolean beginBatchEdit() {
        Log.d(TAG, "beginBatchEdit() called");
        synchronized (this) {
            if (mBatchEditNesting >= 0) {
                mTextView.beginBatchEdit();
                mBatchEditNesting++;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean endBatchEdit() {
        Log.d(TAG, "endBatchEdit() called");
        synchronized (this) {
            if (mBatchEditNesting > 0) {
                // When the connection is reset by the InputMethodManager and reportFinish
                // is called, some endBatchEdit calls may still be asynchronously received from the
                // IME. Do not take these into account, thus ensuring that this IC's final
                // contribution to mTextView's nested batch edit count is zero.
                mTextView.endBatchEdit();
                mBatchEditNesting--;
                return true;
            }
        }
        return false;
    }

    protected void reportFinish() {
        synchronized (this) {
            while (mBatchEditNesting > 0) {
                endBatchEdit();
            }
            // Will prevent any further calls to begin or endBatchEdit
            mBatchEditNesting = -1;
        }
    }

    @Override
    public boolean clearMetaKeyStates(int states) {
        Log.d(TAG, "clearMetaKeyStates() called with: states = [" + states + "]");
        Editable content = getEditable();
        if (content == null) {
            return false;
        }
        KeyListener kl = mTextView.getKeyListener();
        if (kl != null) {
            try {
                kl.clearMetaKeyState(mTextView, content, states);
            } catch (AbstractMethodError e) {
                // This is an old listener that doesn't implement the
                // new method.
            }
        }
        return true;
    }

    @Override
    public boolean commitCompletion(CompletionInfo text) {
        if (DEBUG) {
            Log.v(TAG, "commitCompletion " + text);
        }
        mTextView.beginBatchEdit();
        mTextView.onCommitCompletion(text);
        mTextView.endBatchEdit();
        return true;
    }

    /**
     * Calls the {@link TextView#onCommitCorrection} method of the associated TextView.
     */
    @Override
    public boolean commitCorrection(CorrectionInfo correctionInfo) {
        if (DEBUG) {
            Log.v(TAG, "commitCorrection" + correctionInfo);
        }
        mTextView.beginBatchEdit();
        mTextView.onCommitCorrection(correctionInfo);
        mTextView.endBatchEdit();
        return true;
    }

    @Override
    public boolean performEditorAction(int actionCode) {
        if (DEBUG) {
            Log.v(TAG, "performEditorAction " + actionCode);
        }
        mTextView.onEditorAction(actionCode);
        return true;
    }

    @Override
    public boolean performContextMenuAction(int id) {
        if (DEBUG) {
            Log.v(TAG, "performContextMenuAction " + id);
        }
        mTextView.beginBatchEdit();
        mTextView.onTextContextMenuItem(id);
        mTextView.endBatchEdit();
        return true;
    }

    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
        Log.d(TAG, "getExtractedText() called with: request = [" + request + "], flags = [" + flags + "]");
        if (mTextView != null) {
            ExtractedText et = new ExtractedText();
            if (mTextView.extractText(request, et)) {
                if ((flags & GET_EXTRACTED_TEXT_MONITOR) != 0) {
                    Reflector.invokeMethodExceptionSafe(mTextView, "setExtracting",
                            new Reflector.TypedObject(request, ExtractedTextRequest.class));
                }
                return et;
            }
        }
        return null;
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        if(DEBUG){
            Log.d(TAG, "sendKeyEvent() called with: event = [" + event + "]");
        }
        return super.sendKeyEvent(event);
    }

    @Override
    public boolean performPrivateCommand(String action, Bundle data) {
        if(DEBUG){
            Log.d(TAG, "performPrivateCommand() called with: action = [" + action + "], data = [" + data + "]");
        }
        mTextView.onPrivateIMECommand(action, data);
        return true;
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        if(DEBUG){
            Log.d(TAG, "commitText() called with: text = [" + text + "], mTextView = [" + mTextView + "]");
        }
        if (mTextView == null) {
            return super.commitText(text, newCursorPosition);
        }
        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            SuggestionSpan[] spans = spanned.getSpans(0, text.length(), SuggestionSpan.class);
            Reflector.invokeMethodExceptionSafe(mIMM, "registerSuggestionSpansForNotification",
                    new Reflector.TypedObject(spans, SuggestionSpan[].class));
        }
        Reflector.invokeMethodExceptionSafe(mTextView, "resetErrorChangedFlag");
//        boolean success = super.commitText(text, newCursorPosition);
        Reflector.invokeMethodExceptionSafe(mTextView, "hideErrorIfUnchanged");
//        Log.d(TAG, "commitText() called with: text = [" + text + "], success = [" + success + "]");
        if(!TextUtils.isEmpty(text)){
            mTextView.commitText = text;
        }
        return true;
    }


    private void sendCurrentText() {
        Editable content = getEditable();
        if (content != null) {
            final int N = content.length();

            // 将输入文本模拟为为一个key事件，这样view就会更新内容了
            KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(),
                    content.toString(), KeyCharacterMap.VIRTUAL_KEYBOARD, 0);
            sendKeyEvent(event);
            content.clear();
        }
    }

    @Override
    public boolean requestCursorUpdates(int cursorUpdateMode) {
        if (DEBUG) {
            Log.v(TAG, "requestUpdateCursorAnchorInfo " + cursorUpdateMode);
        }

        // It is possible that any other bit is used as a valid flag in a future release.
        // We should reject the entire request in such a case.
        int KNOWN_FLAGS_MASK = InputConnection.CURSOR_UPDATE_IMMEDIATE |
                InputConnection.CURSOR_UPDATE_MONITOR;
        int unknownFlags = cursorUpdateMode & ~KNOWN_FLAGS_MASK;
        if (unknownFlags != 0) {
            if (DEBUG) {
                Log.d(TAG, "Rejecting requestUpdateCursorAnchorInfo due to unknown flags." +
                        " cursorUpdateMode=" + cursorUpdateMode +
                        " unknownFlags=" + unknownFlags);
            }
            return false;
        }

        if (mIMM == null) {
            // In this case, TYPE_CURSOR_ANCHOR_INFO is not handled.
            // TODO: Return some notification code rather than false to indicate method that
            // CursorAnchorInfo is temporarily unavailable.
            return false;
        }
        Reflector.invokeMethodExceptionSafe(mIMM, "setUpdateCursorAnchorInfoMode",
                new Reflector.TypedObject(cursorUpdateMode, int.class));
        if ((cursorUpdateMode & InputConnection.CURSOR_UPDATE_IMMEDIATE) != 0) {
            if (mTextView == null) {
                // In this case, FLAG_CURSOR_ANCHOR_INFO_IMMEDIATE is silently ignored.
                // TODO: Return some notification code for the input method that indicates
                // FLAG_CURSOR_ANCHOR_INFO_IMMEDIATE is ignored.
            } else {
                // This will schedule a layout pass of the view tree, and the layout event
                // eventually triggers IMM#updateCursorAnchorInfo.
                mTextView.requestLayout();
            }
        }
        return true;
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        return delEventListener != null && delEventListener.onDelEvent() || super
                .deleteSurroundingText(beforeLength, afterLength);
    }

    public void setDelEventListener(
            OnDelEventListener delEventListener) {
        this.delEventListener = delEventListener;
    }

    public interface OnDelEventListener {
        boolean onDelEvent();
    }
}
