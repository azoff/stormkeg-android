# StormKeg

StormKeg is a fork of the popular [Kegbot][1] project, illustrating the how [The Stormpath Java SDK][2] could work on Android, 
and how easy it is to turn a hobby project into a stat-tracking, cloud-backed beer machine!

## Prerequisites

Android runs on Java, so we'll need an up-to-date JDK. At the time of this writing, StormKeg is leveraging [The Oracle JDK8][3].
If matching editors is important to you, StormKeg was built using [Android Studio][4] on MacOSX. Android Studio will take the pain
out of downloading the latest Android libraries, which will be necessary to build the application locally.

Of course, to work with Stormpath, you'll need to [create an account][5] and generate an API key using the instructions from 
[The Java Quickstart][2]. You can store the `apiKey.json` file locally for now; we'll reference it later in the process.

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
app builds. Should you be a Crashlytics user, feel free to add it back into the build process. Now that the app compiles, we're
free to start hacking away!

## Inserting Stormpath As User Authority
Now, we need to figure out how to inject Stormpath into the app as our user authority. Luckily, there already seems to
be a file called `Backend.java` which describes the app's interface for data storage. By leveraging this interface, we could
inject the the Stormpath API right into the regular execution of the program!

We create a new class `StormpathBackendProxy` which implements the `Backend` interface. For now, it'll be empty, but let's stub 
out a constructor that shows how we'd like to use our proxy:

```java
public class StormpathBackendProxy implements Backend {

	private Client mClient;
	private Backend mBackend;

	public StormpathBackendProxy(Client client, Backend backend) {
		mClient = client;
		mBackend = backend;
	}

	public static StormpathBackendProxy fromContext(Context context, Backend backend) {
		final AppConfiguration config = KegbotApplication.get(context).getConfig();
		return new StormpathBackendProxy(null, backend); // TODO: instantiate Stormpath client from config
	}
}
```

The idea here is simple: wrap our proxy around an existing `Backend`, and use the app's configuration to create our client
connection to the Stormpath API.

## Extending The App Configuration
In order to allow us to connect to the Stormpath API, we'll need to extend the application's configuration to allow the user to provide an app id and secret for the service (i.e. the contents of `apiKey.json`). Here are the necessary configuration changes required to support
authenticating against the Stormpath API:

```java

// ... ConfigKey.java
enum ConfigKey {

	STORMPATH_ID(""),
	STORMPATH_APP(""),
	STORMPATH_SECRET(""),

	// ... other keys

}

// ... AppConfiguration.java
enum AppConfiguration {

	public String getStormpathId() {
		return get(ConfigKey.STORMPATH_ID);
	}

	public void setStormpathId(String value) {
		set(ConfigKey.STORMPATH_ID, value);
	}

	public String getStormpathSecret() {
		return get(ConfigKey.STORMPATH_SECRET);
	}

	public void setStormpathSecret(String value) {
		set(ConfigKey.STORMPATH_SECRET, value);
	}

	public String getStormpathAppName() {
		return get(ConfigKey.STORMPATH_APP);
	}

	public void setStormpathAppName(String value) {
		set(ConfigKey.STORMPATH_APP, value);
	}

	public boolean isStormpathAvailable() {
		final String id = getStormpathId();
		final String secret = getStormpathSecret();
		final String name = getStormpathAppName();
		return !Strings.isNullOrEmpty(id) && !Strings.isNullOrEmpty(secret) && !Strings.isNullOrEmpty(name);
	}

	// ... other methods

}
```

Now that the configuration is extended, we can use our proxy to hijack the `Backend` when it is instantiated.

```java

// KegbotCore.java
private KegbotCore(Context context) {

	// ... backend = local SQLLite DB - or - Remote API

	if (mConfig.isStormpathAvailable()) {
		Log.d(TAG, "Using stormpath proxy.");
		mBackend = StormpathBackendProxy.fromContext(mContext, backend);
	} else {
		mBackend = backend;
	}

}

```

We can also finish up the `fromContext` method we created for our proxy:

```java

// StormpathBackendProxy.java
public static StormpathBackendProxy fromContext(Context context, Backend backend) {
	final AppConfiguration config = KegbotApplication.get(context).getConfig();
	ApiKey key = ApiKeys.builder().setId(config.getStormpathId()).setSecret(config.getStormpathSecret()).build();
	Client client = Clients.builder().setApiKey(key).build();
	return new StormpathBackendProxy(client, backend);
}

```

Great. Now our app's configuration can support the concepts necessary to connect to the Stormpath API, and we can
can inject our proxy in between the app and it's backend.

## Using The Setup to Provide Credentials
Even though the app can now support Stormpath credentials, the credentials still need to come from somewhere; 
that's where the `SetupActivity` comes in. The Kegbot App runs an interactive setup activity the first time the app 
is launched on a tablet. This activity runs through a series of steps, eventually filling out the app's configuration.
By adding a new step for Stormpath, we can provide the necessisary credentials to connect to the API. 
We can then create a setup fragment, with the intention to add it to the step flow. 

Stated simply we'll need to add some UI and controllers- you can find them here 

- `setup_stormpath_fragment.xml` The XML description of the layout
- `strings.xml` The localized text context for the UI
- `SetupStormpathFragment.java` The content of the setup screen
- `SetupStormpathStep.java` The step logic for the Stormpath setup

<!-- To communicate with the Stormpath REST API, we can leverage the existing Java instrumentation. Namely, we'll be using 
[The Stormpath Java SDK][2], and we need to instantiate it in the application. The idiomatic way to do this is in Java is 
via a lazy-loaded, app-wide singleton. Luckily, the app already has one: `KegbotCore.java`. -->

[1]:https://kegbot.org/
[2]:http://docs.stormpath.com/java/quickstart/
[3]:http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[4]:https://developer.android.com/sdk/index.html
[5]:https://api.stormpath.com/register
[6]:https://github.com/azoff/usb-serial-for-android
[7]:http://tools.android.com/tech-docs/new-build-system/migrating-to-1-0-0
[8]:https://try.crashlytics.com/