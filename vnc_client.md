# 输入法在APP的实现

通过DetectEventEditText作为输入控件，接管输入事件
通过EditableInputConnection是hook的系统inputconnection，注入到DetectEventEditText，即可以接管输入法事件

再通过如下函数处理事件，分为unicode和keycode
com.iiordanov.bVNC.RemoteCanvasActivity.dispatchKeyEvent

字符串也是拆成一个一个的字符成本发送down up事件
````
    ((RfbProto) rfb).writeKeyStringEvent(keyCode, c.charAt(index), true);
    ((RfbProto) rfb).writeKeyStringEvent(keyCode, c.charAt(index), false);
````

在空闲byte位填入0xff作为unicode标识给vncserver
````
    public void writeSpacialKeyEvent(int keycode, int keysym, boolean down) {
        if (viewOnly)
            return;
        GeneralUtils.debugLog(this.debugLogging, TAG, "writeKeyEvent, sending keysym:" +
                keysym + ", down: " + down);
        eventBuf[eventBufLen++] = (byte) KeyboardEvent;
        eventBuf[eventBufLen++] = (byte) (down ? 1 : 0);
        eventBuf[eventBufLen++] = (byte) 0;
        eventBuf[eventBufLen++] = (byte) 0xff;
        eventBuf[eventBufLen++] = (byte) ((keysym >> 24) & 0xff);
        eventBuf[eventBufLen++] = (byte) ((keysym >> 16) & 0xff);
        eventBuf[eventBufLen++] = (byte) ((keysym >> 8) & 0xff);
        eventBuf[eventBufLen++] = (byte) (keysym & 0xff);
    }
````

# 输入法在vncserver的实现

简单来说就是拿到unicode后，通过ibus按键协议发送到ibus，所以在app侧只要确保发送的是正确的keysym,其他逻辑就在vncserver了