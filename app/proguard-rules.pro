# Add project specific ProGuard rules here.

# Keep Gson serialized classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.course.imchat.** { *; }
-keep class com.google.gson.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**
-keep class okio.** { *; }

# Keep Markwon
-keep class io.noties.markwon.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep WebSocket classes
-keep class org.java_websocket.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
