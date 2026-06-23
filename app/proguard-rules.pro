# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Apache POI
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn org.apache.poi.**
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.** { *; }

# Log4j (used by POI but not available on Android)
-dontwarn org.apache.logging.log4j.**

# AWT (not available on Android)
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
