-keep class com.google.android.gms.ads.** { *; }
-keep class com.yourpackage.dungeoncrawler.** { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
