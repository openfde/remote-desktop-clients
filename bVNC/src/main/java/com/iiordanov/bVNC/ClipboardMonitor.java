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

import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;

import java.util.TimerTask;

/*
 * This is a TimerTask which checks the clipboard for changes, and if
 * a change is detected, sends the new contents to the VNC server.
 */

public class ClipboardMonitor extends TimerTask {
    private ClipboardManager clipboard;
    private String TAG = "ClipboardMonitor";
    private Context context;
    RemoteCanvas vncCanvas;

    public static String knownClipboardContents;

    public ClipboardMonitor (Context c, RemoteCanvas vc, android.content.ClipboardManager clipboard) {
        context   = c;
        vncCanvas = vc;
        this.clipboard = clipboard;
        knownClipboardContents = new String("");
    }
    
    /*
     * Grab the current clipboard contents.
     */
    private String getClipboardContents () {
        try {
            return clipboard.getText().toString();
        } catch (NullPointerException e) {
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }
    
    /*
     * (non-Javadoc)
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
        String currentClipboardContents = getClipboardContents ();
        if (!vncCanvas.serverJustCutText && currentClipboardContents != null &&
            !currentClipboardContents.equals(knownClipboardContents)) {
            if (vncCanvas.rfbconn != null && vncCanvas.rfbconn.isInNormalProtocol()) {
                    vncCanvas.rfb.writeClipboardNotify();
                    knownClipboardContents = new String(currentClipboardContents);
            }
        } else if (vncCanvas.serverJustCutText && currentClipboardContents != null) {
            knownClipboardContents = new String(currentClipboardContents);
            vncCanvas.serverJustCutText = false;
            //Log.d(TAG, "Set knownClipboardContents to equal what server just sent over.");
        }
    }
}
