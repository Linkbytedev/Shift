# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep encryption classes
-keep class com.Linkbyte.Shift.security.** { *; }

# Keep data models for Firebase
-keep class com.Linkbyte.Shift.data.model.** { *; }

# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# WebRTC
-keep class org.webrtc.** { *; }

# Keep Entry Points
-keep class com.Linkbyte.Shift.MainActivity { *; }
-keep class com.Linkbyte.Shift.presentation.splash.SplashActivity { *; }
-keep class com.Linkbyte.Shift.MessengerApplication { *; }
