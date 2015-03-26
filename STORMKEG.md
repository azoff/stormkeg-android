# StormKeg

StormKeg is a fork of the popular [Kegbot][1] project, illustrating the how [The Stormpath Java SDK][2] could work on Android, 
and how easy it is to turn a hobby project into a stat-tracking, cloud-backed beer machine!

## Prerequisites

Android runs on Java, so we'll need an up-to-date JDK. At the time of this writing, StormKeg is leveraging [The Oracle JDK8][3].
If matching editors is important to you, StormKeg was built using [Android Studio][4] on MacOSX. Android Studio will take the pain
out of downloading the latest Android libraries, which will be necessary to build the application locally.

Of course, to work with Stormpath, you'll need to [create an account][5] and generate an API key using the instructions from 
[The Java Quickstart][2]. You can store the `apiKey.json` file in the `assets` folder found in `kegtab/src/main`. The file
will be ignored by version control, so don't worry about committing it.

## Ensure The App Builds
Before we can make any changes, we need to make sure the app can be built. Importing the existing project gets you most of the way 
there, but you may need to make sure that [Gradle is working][7]. The StormKeg fork works with the latest Gradle tools and cleans up
ambiguous dependency versions. We'll also need to add Stormpath dependencies to Gradle, which we can add to the `kegtab` project's
`build.gradle` file:

```groovy
dependencies {
	// ... other dependencies ...
    compile 'com.stormpath.sdk:stormpath-sdk-api:1.0.RC3.1'
    compile 'com.stormpath.sdk:stormpath-sdk-httpclient:1.0.RC3.1'
}
```

There's also a project dependency on [A USB communication library][6], which has been forked to correct Gradle inconsistencies. 
StormKeg references this project as a submodule, and it should exist after you clone the repo. If you see errors related to usb 
dependencies, you might need to update your git submodules.

StormKeg also removes references to [Crashlytics][8], which is a paid service and should not hold sway over whether or not the
app builds. Should you be a Crashlytics user, feel free to add it back into the build process.

## Incorporating The Java Client

To communicate with the Stormpath REST API, we can leverage the existing Java instrumentation. Namely, we'll be using the 
`stormpath-sdk-api` and the `stormpath-sdk-httpclient` artifacts. Before we can do that, we

[1]:https://kegbot.org/
[2]:http://docs.stormpath.com/java/quickstart/
[3]:http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[4]:https://developer.android.com/sdk/index.html
[5]:https://api.stormpath.com/register
[6]:https://github.com/azoff/usb-serial-for-android
[7]:http://tools.android.com/tech-docs/new-build-system/migrating-to-1-0-0
[8]:https://try.crashlytics.com/