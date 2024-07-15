-assumenosideeffects class android.util.Log {

public static boolean isLoggable(java.lang.String,int);

public static int v(...);

public static int i(...);

public static int d(...);

#public static int w(...);

#public static int e(...);

}

-keep class android.database.** { *; }
-keep interface android.database.** { *; }
# OkHttp3
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# sqlcipher
-keep,includedescriptorclasses class net.sqlcipher.** { *; }
-keep,includedescriptorclasses interface net.sqlcipher.** { *; }

-keep,includedescriptorclasses class android.app.** { *; }
-keep,includedescriptorclasses interface android.app.** { *; }

-keep,includedescriptorclasses class com.yanzhenjie.** { *; }
-keep,includedescriptorclasses interface com.yanzhenjie.** { *; }

-keep,includedescriptorclasses class com.iiordanov.bVNC.** { *; }
-keep,includedescriptorclasses interface com.iiordanov.bVNC.** { *; }

-keep,includedescriptorclasses class com.undatech.opaque.** { *; }
-keep,includedescriptorclasses interface com.undatech.opaque.** { *; }

-keep,includedescriptorclasses class org.apache.** { *; }
-keep,includedescriptorclasses interface org.apache.** { *; }

-keep,includedescriptorclasses class ft.fdevnc.** { *; }
-keep,includedescriptorclasses interface ft.fdevnc.** { *; }

-keep,includedescriptorclasses class tigervnc.** { *; }
-keep,includedescriptorclasses interface tigervnc.** { *; }


-keep,includedescriptorclasses class com.github.luben.** { *; }
-keep,includedescriptorclasses interface com.github.luben.** { *; }

-keep,includedescriptorclasses class com.** { *; }
-keep,includedescriptorclasses interface com.** { *; }

# 基于 sdk/tools/proguard/proguard-android-optimize.txt 修改
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# 不要删除无用代码
-dontshrink

# 不混淆泛型
-keepattributes Signature

# 不混淆注解类
-keepattributes *Annotation*

# 不混淆本地方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 不混淆 Activity 在 XML 布局所设置的 onClick 属性值
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# 不混淆枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 不混淆 Parcelable 子类
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

# 不混淆 Serializable 子类
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 不混淆 R 文件中的字段
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 不混淆 WebView 设置的 JS 接口的方法名
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class net.i2p.crypto.eddsa.** { *; }
