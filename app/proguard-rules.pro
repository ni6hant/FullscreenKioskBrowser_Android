# ProGuard / R8 rules for Simple Android Kiosk Browser

# Keep app classes
-keep class com.simplekioskbrowser.** { *; }

# Keep WebView-related classes (R8 can strip these)
-keep class android.webkit.** { *; }
-keep class android.net.http.SslError { *; }

# Keep settings and data classes
-keep class com.simplekioskbrowser.settings.** { *; }

# Suppress warnings for optional APIs
-dontwarn android.webkit.**

# Keep line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
