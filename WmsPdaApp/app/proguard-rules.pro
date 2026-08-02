# ============================================================================
#  WMS PDA App - ProGuard Rules
# ============================================================================

# ----------------------------------------------------------------------------
#  General optimization settings
# ----------------------------------------------------------------------------
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-verbose

# Optimization
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-allowaccessmodification

# Keep important attributes
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# ----------------------------------------------------------------------------
#  Application model / data classes
# ----------------------------------------------------------------------------
-keep class com.wms.pda.model.** { *; }
-keep class com.wms.pda.bean.** { *; }
-keep class com.wms.pda.entity.** { *; }
-keep class com.wms.pda.dto.** { *; }
-keep class com.wms.pda.data.** { *; }

# ----------------------------------------------------------------------------
#  Gson serialization
# ----------------------------------------------------------------------------
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * extends com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * extends com.google.gson.JsonSerializer
-keep class * extends com.google.gson.JsonDeserializer

# Keep fields annotated with @SerializedName
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep generic type information for Gson
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ----------------------------------------------------------------------------
#  Retrofit
# ----------------------------------------------------------------------------
-dontwarn retrofit2.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface * {
    @retrofit2.http.* <methods>;
}

# ----------------------------------------------------------------------------
#  OkHttp
# ----------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class okhttp3.internal.platform.** { *; }

# ----------------------------------------------------------------------------
#  ZXing barcode / QR scanning
# ----------------------------------------------------------------------------
-dontwarn com.google.zxing.**
-keep class com.google.zxing.** { *; }
-keep class com.google.zxing.**$* { *; }

-dontwarn com.journeyapps.barcodescanner.**
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.journeyapps.barcodescanner.**$* { *; }

# ----------------------------------------------------------------------------
#  AndroidX / Support libraries
# ----------------------------------------------------------------------------
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**
-dontwarn com.google.android.material.**
-keep class com.google.android.material.** { *; }

# ----------------------------------------------------------------------------
#  Serializable
# ----------------------------------------------------------------------------
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ----------------------------------------------------------------------------
#  Enum
# ----------------------------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ----------------------------------------------------------------------------
#  Parcelable (Android)
# ----------------------------------------------------------------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ----------------------------------------------------------------------------
#  R files
# ----------------------------------------------------------------------------
-keep class **.R$* { *; }

# ----------------------------------------------------------------------------
#  Keep application core classes
# ----------------------------------------------------------------------------
-keep class com.wms.pda.app.** { *; }
-keep class com.wms.pda.App { *; }
-keep class com.wms.pda.App$* { *; }

# ----------------------------------------------------------------------------
#  Native methods
# ----------------------------------------------------------------------------
-keepclasseswithmembernames class * {
    native <methods>;
}

# ----------------------------------------------------------------------------
#  Custom views
# ----------------------------------------------------------------------------
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ----------------------------------------------------------------------------
#  Activities, Services, Receivers (manifest-registered components)
# ----------------------------------------------------------------------------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Application

# ----------------------------------------------------------------------------
#  JavaScript interface (WebView)
# ----------------------------------------------------------------------------
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
