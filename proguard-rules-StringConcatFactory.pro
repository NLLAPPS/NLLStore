##################################################################################################
##################################################################################################
#
# This file added by CommonModulePlugin to defaultConfig of each module. We cannot build on AS Flamingo and above without it.
# See https://issuetracker.google.com/issues/250197571#comment7
#
#
##################################################################################################
##################################################################################################
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn javax.annotation.processing.AbstractProcessor
-dontwarn javax.annotation.processing.SupportedOptions
