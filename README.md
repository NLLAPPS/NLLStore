NLLStore is an Android app store designed to provide a single source for installing our apps. It was primarily developed to address the latest restriction introduced on Android 14, which imposes limitations on enabling Accessibility Service. Google have been continuing its efforts to curb call recording, and with Android 13, the company restricted Accessibility Service usage and placed it behind an install time permission called "android.permission.ACCESS\_RESTRICTED\_SETTINGS".

To overcome this restriction, we created [APH](https://acr.app/) and published it with ACCESS\_RESTRICTED\_SETTINGS permission. However, it appears that this workaround is not effective on Android 14, as side-loaded apps are unable to activate Accessibility Service at all.

In order to use Accessibility Service, apps must be installed from a store that utilizes a specific Android API called [PackageInstaller.Session](https://developer.android.com/reference/android/content/pm/PackageInstaller.Session). Therefore, we had to create our own app store, allowing users to download and sideload it, then install APH through NLLStore. While this process may seem cumbersome, it is currently the only way.

We would not be surprised if Google have plans to limit the installation of store apps that can install other apps on future versions of Android!
