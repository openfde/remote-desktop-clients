/**
 * Copyright (C) 2012 Iordan Iordanov
 * 
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

package com.iiordanov.bVNC;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.TimerTask;

/*
 * This is a TimerTask which checks the clipboard for changes, and if
 * a change is detected, sends the new contents to the VNC server.
 */

public class ClipboardMonitor extends TimerTask {
    private ClipboardManager clipboard;
    private String TAG = "ClipboardMonitor hy";
    private Context context;
    RemoteCanvas vncCanvas;

    public static String knownClipboardContents;
    public static String inputUnicode;

    public ClipboardMonitor (Context c, RemoteCanvas vc, android.content.ClipboardManager clipboard) {
        context   = c;
        vncCanvas = vc;
        this.clipboard = clipboard;
        knownClipboardContents = new String("");
    }
    
    /*
     * Grab the current clipboard contents.
     */
//    private String getClipboardContents () {
//        try {
//            return clipboard.getText().toString();
//        } catch (NullPointerException e) {
//            return null;
//        } catch (RuntimeException e) {
//            return null;
//        }
//    }
    
    /*
     * (non-Javadoc)
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
        ClipData primaryClip = clipboard.getPrimaryClip();
        if (primaryClip != null) {
            boolean vnc = primaryClip.getDescription().getLabel() != null && primaryClip.getDescription().getLabel().equals("vnc");
            ClipData.Item itemAt = primaryClip.getItemAt(0);
            CharSequence text = itemAt.getText();
            String cliptext = null;
            if(text != null){
                cliptext = text.toString();
            }
            if (!TextUtils.isEmpty(cliptext) && !cliptext.equals(knownClipboardContents) && !vnc) {
                if (vncCanvas.rfbconn != null && vncCanvas.rfbconn.isInNormalProtocol()) {
                    knownClipboardContents = cliptext;
                    Log.d(TAG, "ClipboardMonitor get local String:" + knownClipboardContents);
                    vncCanvas.rfb.writeClipboardNotify();
                }
            }
        }
    }
}
