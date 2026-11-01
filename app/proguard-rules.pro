# Room keep rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Coroutines keep rules
-keepclassmembers class * {
    @kotlinx.coroutines.InternalCoroutinesApi *;
}

# Preserve entity models and backup data classes
-keep class com.example.data.** { *; }

# Preserve custom views and widgets
-keep class com.example.widget.** { *; }
-keep class com.example.receiver.** { *; }

