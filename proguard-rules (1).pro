# Tuner Tools - targeted R8 rules for the release build.
# Retain source positions to make Play Console crash reports useful.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# WorkManager restores the app's persisted periodic reminder by worker class name.
# Keep only the worker entry point that it constructs reflectively.
-keep class ducphi.tunnerinstrument.ReminderWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# WorkManager 2.9.x uses Room's generated WorkDatabase implementation.
# Preserve the reflective class names and its no-argument constructor to avoid
# "Failed to create an instance of androidx.work.impl.WorkDatabase" in release.
# This replaces the previous blanket -keep class androidx.work.** { *; } rule.
-keep class androidx.work.impl.WorkDatabase
-keep class androidx.work.impl.WorkDatabase_Impl {
    <init>();
}

# WorkManager can also instantiate InputMergers reflectively from saved work.
# Retain their constructors instead of every member in androidx.work.
-keep class * extends androidx.work.InputMerger {
    <init>();
}

# AndroidX Startup / Firebase / AdMob libraries already ship their own
# consumer keep rules. Do not keep entire SDK packages unless a release
# build demonstrates a specific missing reflective entry point.
# Avoid blanket -dontwarn androidx.work.** and -dontwarn androidx.room.**:
# real missing-dependency errors should be investigated, not suppressed.
