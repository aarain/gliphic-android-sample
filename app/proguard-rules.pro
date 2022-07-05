# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in .../Android/sdk/tools/proguard/proguard-android.txt
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

###############################################################################

#See: https://github.com/google/gson/blob/master/examples/android-proguard-example/proguard.cfg

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
######-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
######-keep class com.google.gson.examples.android.model.** { <fields>; }
# Ignore any misleading "Unresolved class name" error from the IDE.
-keep class pojo.** { <fields>; }
-keep class org.jivesoftware.smack.** { <fields>; }
-keep class org.jivesoftware.smackx.** { <fields>; }
-keep class org.json.** { <fields>; }

# Enumerations' members that will be serialized/deserialized over Gson.
-keepclassmembers enum libraries.** { <fields>; }
-keepclassmembers enum gliphic.android.operation.storage_handlers.ForcedDialogs$ForcedSignOutAction { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
######-keep class * extends com.google.gson.TypeAdapter
######-keep class * implements com.google.gson.TypeAdapterFactory
######-keep class * implements com.google.gson.JsonSerializer
######-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

##---------------End: proguard configuration for Gson  ----------
