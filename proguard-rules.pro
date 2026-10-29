# Giữ lại số dòng lỗi để dễ debug trên Google Play Console sau này
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Giữ lại các class của WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Khắc phục triệt để lỗi "Failed to create an instance of WorkDatabase"
-keep class androidx.work.impl.WorkDatabase** { *; }
-keep class androidx.work.impl.WorkDatabase_* { *; }

# Bỏ qua cảnh báo thiếu thư viện Room (Giúp mất dòng báo đỏ)
-dontwarn androidx.room.**

# Giữ lại AndroidX Startup Initializer
-keep class androidx.startup.** { *; }
-keep class * extends androidx.startup.Initializer { *; }