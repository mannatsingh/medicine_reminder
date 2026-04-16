# Keep Google Drive API model classes
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
