-repackageclasses ''
-allowaccessmodification
-keepattributes RuntimeVisibleAnnotations

#proguard is removing PReferenceCompat! Keep it
-keep public class * extends androidx.preference.PreferenceFragmentCompat