# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/willi/android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-dontwarn com.squareup.okhttp.**
-keep class rikka.shizuku.Shizuku { *; }

# Keep REVROR core integrity classes and signatures
-keep class com.smartpack.kernelmanager.RevrorApp { *; }
-keep class com.smartpack.kernelmanager.utils.AppIntegrity { *; }
-keepclassmembers class * {
    public static final java.lang.String REVROR_SIGNATURE;
}
-keep class com.smartpack.kernelmanager.activities.DashboardActivity { *; }
-keep class com.smartpack.kernelmanager.activities.GameLauncherActivity { *; }
-keep class com.smartpack.kernelmanager.services.GameBubbleService { *; }
-keep class com.smartpack.kernelmanager.utils.GsmProfileManager { *; }
-keep class revror.kernelmanager.** { *; }
