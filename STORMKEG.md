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
	private Application mApp;
	private Backend mBackend;

	public StormpathBackendProxy(Client client, Application app, Backend backend) {
		mApp = app;
		mClient = client;
		mBackend = backend;
	}

	public static StormpathBackendProxy fromContext(Context context, Backend backend) {
		// TODO: use the context to connect to stormpath
		return new StormpathBackendProxy(null, null, backend);
	}

	// ... backend interface methods ...

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
	final ApiKey key = ApiKeys.builder().setId(config.getStormpathId()).setSecret(config.getStormpathSecret()).build();
	final Client client = Clients.builder().setApiKey(key).build();
	final String appName = config.getStormpathAppName();
	final Application app = client.getApplications(Applications.where(Applications.name().eqIgnoreCase(appName))).iterator().next();
	return new StormpathBackendProxy(client, backend);
}	

```

Great. Now our app's configuration can support the concepts necessary to connect to the Stormpath API, and we can
can inject our proxy in between the app and it's backend.

## Extending Setup to Provide Credentials
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

## Hijacking Authentication
Now that all the tools are in place, and we've set up our client, it's now time to start incorporating Stormpath into the 
authentication workflow. We'll need to create a bridge between KegBot _users_ and Stormpath _accounts_. Here's the basic
logic from `StormpathAccountBridge.java`:

```java

public static Models.User userFromAccount(Account account) {

	CustomData data = account.getCustomData();
	Models.User.Builder builder = Models.User.newBuilder()
			.setEmail(account.getEmail())
			.setDisplayName(account.getUsername())
			.setUsername(account.getUsername())
			.setFirstName(account.getGivenName())
			.setLastName(account.getSurname())
			.setIsActive(account.getStatus() != AccountStatus.DISABLED)
			.setUrl(account.getHref());

	String key = StormpathCustomDataKey.DATE_JOINED.name();
	if (data.containsKey(key)) {
		builder.setDateJoined((String) data.get(key));
	}

	key = StormpathCustomDataKey.PROFILE_IMAGE.name();
	if (data.containsKey(key)) {
		try {
			byte[] imageData = (byte[]) data.get(key);
			Models.Image image = Models.Image.parseFrom(imageData);
			builder.setImage(image);
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
	}

	return builder.build();

}

```

Now we can take over the user creation methods of `Backend`, leveraging the Stormpath Account API!

```java

// StormpathBackendProxy.java

@Override
public Models.User createUser(String username, String email, String password, String imagePath) throws BackendException {

	Account account = mClient.instantiate(Account.class);

	account.setEmail(email);
	account.setUsername(username);
	account.setPassword(password);

	CustomData data = account.getCustomData();
	Date date = new Date();
	data.put(StormpathCustomDataKey.DATE_JOINED.name(), date.toString());

	File file = new File(imagePath);
	if (file.exists()) {
		try {
			data.put(StormpathCustomDataKey.PROFILE_IMAGE.name(), Files.toByteArray(file));
		} catch (IOException ex) {
			throw new BackendException("unable to read user image file", ex);
		}
	}

	// save the account to stormpath
	try {
		account = mApp.createAccount(account);
	} catch (ResourceException ex) {
		throw new StormpathApiException("unable to create account", ex);
	}

	// save the account to the backend
	Models.User.Builder builder = Models.User.newBuilder();
	if (!(mBackend instanceof LocalBackend)) {
		builder = mBackend.createUser(username, email, password, imagePath).toBuilder();
	}

	return StormpathAccountBridge.userFromAccount(account, builder);

}

@Override
public Models.User getUser(String username) throws BackendException {

	// first grab any local data
	Models.User.Builder builder;
	if (!(mBackend instanceof LocalBackend)) {
		builder = mBackend.getUser(username).toBuilder();
	} else {
		builder = Models.User.newBuilder();
	}

	// next, merge in account data
	AccountCriteria where = Accounts.where(Accounts.username().eqIgnoreCase(username)).limitTo(1);
	Iterator<Account> accounts = mApp.getAccounts(where).iterator();
	if (!accounts.hasNext())
		throw new StormpathApiException("unable to find user: " + username);

	return StormpathAccountBridge.userFromAccount(accounts.next(), builder);

}

@Override
public List<Models.User> getUsers() throws BackendException {

	// first, grab in any local data
	Map<String, Models.User.Builder> builders = new HashMap<String, Models.User.Builder>();
	if (!(mBackend instanceof LocalBackend))
		for (Models.User user : mBackend.getUsers())
			builders.put(user.getUsername(), user.toBuilder());


	// next, merge in account data
	ArrayList<Models.User> users = new ArrayList<Models.User>();
	for (Account account : mApp.getAccounts()) {
		Models.User.Builder builder = Models.User.newBuilder();
		if (builders.containsKey(account.getUsername()))
			builder = builders.get(account.getUsername());
		users.add(StormpathAccountBridge.userFromAccount(account, builder));
	}

	return users;
	
}

```

That's it! We should be able to run the project now.

## Troubleshooting

The Stormpath SDK relies on [The Apache HTTP Components Library][9] for instantiating Clients. Unfortunately, The Android SDK 
insists that developers favor the built-in java APIs. As a result, using Stormpath might require a little working around the
limitations. First, we can import the android-port of the http component libraries using gradle:

```groovy

// kegtab/build.gradle
compile 'org.apache.httpcomponents:httpclient-android:4.3.3'

```

Then we can [fork the SDK][10] to work with the modified HTTP client and replace the official reference with our compiled jars:

```groovy

// kegtab/build.gradle

compile files('../modules/stormpath-sdk-java/api/build/libs/stormpath-sdk-api-1.0.RC4-SNAPSHOT.jar')
compile files('../modules/stormpath-sdk-java/impl/build/libs/stormpath-sdk-impl-1.0.RC4-SNAPSHOT.jar')
compile files('../modules/stormpath-sdk-java/extensions/oauth/build/libs/stormpath-sdk-oauth-1.0.RC4-SNAPSHOT.jar')
compile files('../modules/stormpath-sdk-java/extensions/httpclient/build/libs/stormpath-sdk-httpclient-1.0.RC4-SNAPSHOT.jar')

```

_Note: These jars are not properly packaged, and are simply listed flat to enumerate the dependencies._



## Next Steps
Here are some steps we could take to make the integration even better:

- Replace the device authentication with Facebook and other federated logins
- Use a local cache to map NFC card tokens to stormpath accounts
- Save some small statistic data to the stormpath accounts so that we can use stats outside of the proprietary server

[1]:https://kegbot.org/
[2]:http://docs.stormpath.com/java/quickstart/
[3]:http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[4]:https://developer.android.com/sdk/index.html
[5]:https://api.stormpath.com/register
[6]:https://github.com/azoff/usb-serial-for-android
[7]:http://tools.android.com/tech-docs/new-build-system/migrating-to-1-0-0
[8]:https://try.crashlytics.com/
[9]:https://hc.apache.org/httpcomponents-client-4.3.x/android-port.html
[10]:https://github.com/azoff/stormpath-sdk-java