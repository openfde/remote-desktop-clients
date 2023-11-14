package com.ft.fdevnc;

public class Constants {
    public static String BASIP = "127.0.0.1";
    public static String BASEURL = "http://" + BASIP + ":18080";
    public static final String URL_GETALLAPP = "/api/v1/apps";
    public static final String URL_STARTAPP = "/api/v1/vnc";
    public static final String URL_STOPAPP = "/api/v1/vnc";

    public static final String URL_KILLAPP = "/api/v1/stop_vnc";

    public static String app = null;
}
