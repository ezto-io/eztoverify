-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}

## HMS Core SDK
-ignorewarnings
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keep class com.hianalytics.android.**{*;}