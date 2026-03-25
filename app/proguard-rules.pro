# Add project specific ProGuard rules here.

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.health.companion.**$$serializer { *; }
-keepclassmembers class com.health.companion.** {
    *** Companion;
}
-keepclasseswithmembers class com.health.companion.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Room
-keep @androidx.room.Entity class ** { *; }
-keep @androidx.room.Dao class ** { *; }

# Data Classes
-keepclassmembers class ** {
    *** copy(...);
    *** component1(...);
    *** component2(...);
    *** component3(...);
    *** component4(...);
    *** component5(...);
}

# Compose
-keep class androidx.compose.** { *; }

# General
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# Timber
-dontwarn org.jetbrains.annotations.**
