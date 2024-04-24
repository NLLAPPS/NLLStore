NLL Store is an Android app store designed to provide a single source for installing our apps. It was primarily developed to address the latest restriction introduced on Android 14, which imposes further limitations on enabling Accessibility Service.

&nbsp;

**Android 15 Update (24/April/2024)**
&nbsp;

As we have susspected before (*see the last line of this page*), Google might be preparing to put the final nail on the coffin of call recording and end the cat and mouse game for good.
&nbsp;
I have just read a [dooming article]( https://www.androidauthority.com/android-15-enhanced-confirmation-mode-3436697/) about improvements to the “Enhanced Confirmation Mode” 
I can confirm that **there will be no call recording possibility with Accessibility Service on Android 15**, if Google releases improved “Enhanced Confirmation Mode”.
&nbsp;

Improvements to “Enhanced Confirmation Mode” will prevent enabling Accessibility Service of any app that is not installed by a “Trusted Store” essentially ending possibility of call recording on Android 15+

&nbsp;

**Originial intro**
&nbsp;

Google seems to be committed to stop call recording. They have restricted Accessibility Service usage on Google Play Store and placed it behind an install time permission called "android.permission.ACCESS\_RESTRICTED\_SETTINGS" for sideloaded apps on Android 13.
&nbsp;

To overcome this restriction, we created [APH](https://acr.app/) and published it with ACCESS\_RESTRICTED\_SETTINGS permission. However, it appears that this workaround is no longer effective on Android 14, as side-loaded apps are unable to activate Accessibility Service at all.
&nbsp;

On Android 14, in order to use Accessibility Service, apps must be installed from a store that utilizes a specific Android API called [PackageInstaller.Session](https://developer.android.com/reference/android/content/pm/PackageInstaller.Session). Therefore, we had to create our own app store, allowing users to download and sideload it, then install APH through NLL Store. While this process may seem cumbersome, it is currently the only way.
&nbsp;

Please note that NLL Store is mainly for phones such as Pixel, Xiomi, Oppo etc without manufacturer app stores. APH is already published on Samsung Galaxy Store and Huawei App Gallery. You can simply go to [APH website](https://acr.app/) and use direct links to APH on those stores to download it.
&nbsp;

We would not be surprised if Google have plans to limit the installation of store apps that can install other apps on future versions of Android!
