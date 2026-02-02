# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Zoom SDK classes
-keep class us.zoom.** { *; }
-dontwarn us.zoom.**

# Keep ZoomSession library public API
-keep public class com.yourcompany.zoomsession.** { public *; }
