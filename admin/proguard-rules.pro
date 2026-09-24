# Add project specific ProGuard rules here.

# Ktor
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-keep class kotlinx.serialization.** { *; }

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
