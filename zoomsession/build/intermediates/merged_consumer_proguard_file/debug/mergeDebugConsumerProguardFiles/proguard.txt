# Consumer ProGuard rules for apps that use this library
# Keep Zoom SDK classes
-keep class us.zoom.** { *; }
-dontwarn us.zoom.**
# Keep ZoomSession library public classes
-keep public class com.yourcompany.zoomsession.** { public *; }
