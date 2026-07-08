# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Apache POI
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn org.apache.poi.**
-keep class org.apache.xmlbeans.** { *; }
-keep class org.apache.xmlbeans.impl.schema.SchemaTypeSystemImpl { *; }
-keepclassmembers class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.** { *; }
-keepclassmembers class org.openxmlformats.** { *; }
-keep class org.apache.poi.** { *; }
-keepclassmembers class org.apache.poi.** { *; }
-keep class * implements org.apache.xmlbeans.SchemaTypeSystem { *; }
-keepclassmembers class * implements org.apache.xmlbeans.SchemaTypeSystem { *; }
-keep class schemasMicrosoftComVml.** { *; }

# Log4j (used by POI but not available on Android)
-dontwarn org.apache.logging.log4j.**

# AWT (not available on Android)
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**

# Optional compression libs used by POI (not available on Android)
-dontwarn com.github.luben.zstd.**
-dontwarn org.tukaani.xz.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
