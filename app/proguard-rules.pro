# Add project specific ProGuard rules here.

# Ktor
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-keep class kotlinx.serialization.** { *; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**
-dontwarn org.fusesource.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# DataStore
-keep class * extends androidx.datastore.preferences.core.Preferences$Key { *; }

# Firebase
-keep class com.google.firebase.** { *; }

# ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# App models passed through Intent / Serializable
-keep class com.toolbox.pro.fileshare.server.SharedFile { *; }
-keepclassmembers class com.toolbox.pro.fileshare.server.SharedFile { *; }

# API DTOs
-keep class com.toolbox.pro.core.api.** { *; }
-keepclassmembers class com.toolbox.pro.core.api.** { *; }

# Kotlin
-dontwarn kotlin.**
-keepattributes *Annotation*, InnerClasses, Signature
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
