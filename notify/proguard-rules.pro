# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/dialog/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile



# 09/07/2021
# just updated to
# Android Studio Bumblebee | 2021.1.1 Canary 3
# Build #AI-211.7442.40.2111.7518594, built on July 2, 2021
# And had to add
#
# -keep class io.karn.notify.entities.*
#
# to my proguard files
# Without that line it ket crashing at payload.bubblize at NotificationInterop.kt
-keep class io.karn.notify.entities.*
-dontwarn java.lang.invoke.StringConcatFactory
