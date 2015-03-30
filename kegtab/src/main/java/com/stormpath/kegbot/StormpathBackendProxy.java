package com.stormpath.kegbot;

import android.content.Context;
import android.util.Log;
import com.google.common.io.Files;
import com.stormpath.sdk.account.*;
import com.stormpath.sdk.api.ApiKey;
import com.stormpath.sdk.api.ApiKeys;
import com.stormpath.sdk.application.Application;
import com.stormpath.sdk.application.ApplicationCriteria;
import com.stormpath.sdk.application.Applications;
import com.stormpath.sdk.cache.CacheManager;
import com.stormpath.sdk.client.AuthenticationScheme;
import com.stormpath.sdk.client.Client;
import com.stormpath.sdk.client.Clients;
import com.stormpath.sdk.directory.CustomData;
import com.stormpath.sdk.query.Criterion;
import com.stormpath.sdk.resource.ResourceException;
import org.codehaus.jackson.JsonNode;
import org.kegbot.app.KegbotApplication;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.util.TimeSeries;
import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;
import org.kegbot.backend.LocalBackend;
import org.kegbot.proto.Api;
import org.kegbot.proto.Models;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Azoff on 3/25/15.
 */
public class StormpathBackendProxy implements Backend {

	private String mAppName;
	private Client mClient;
	private Backend mBackend;

	private static final String TAG = StormpathBackendProxy.class.getSimpleName();

	private static final Map<String,Application> mAppCache = new HashMap<String, Application>();

	public StormpathBackendProxy(String appName, Client client, Backend backend) {
		mClient = client;
		mAppName = appName;
		mBackend = backend;
	}

	public static StormpathBackendProxy fromContext(Context context, Backend backend)
			throws ResourceException, NoSuchElementException {
		final AppConfiguration config = KegbotApplication.get(context).getConfig();
		final ApiKey key = ApiKeys.builder().setId(config.getStormpathId()).setSecret(config.getStormpathSecret()).build();
		final Client client = Clients.builder().setApiKey(key).setAuthenticationScheme(AuthenticationScheme.BASIC).build();
		final String name = config.getStormpathAppName();
		return new StormpathBackendProxy(name, client, backend);
	}

	public static StormpathBackendProxy fromContext(Context context) {
		return fromContext(context, null);
	}

	public Application getApplication() throws NoSuchElementException {
		if (mAppCache.containsKey(mAppName)) return mAppCache.get(mAppName);
		ApplicationCriteria query = Applications.where(Applications.name().eqIgnoreCase(mAppName));
		Application app = mClient.getApplications(query).iterator().next();
		mAppCache.put(mAppName, app);
		return app;
	}

	@Override
	public void start(Context context) {
		mBackend.start(context);
	}

	@Override
	public Models.KegTap startKeg(Models.KegTap tap, String beerName, String brewerName, String styleName, String kegType) throws BackendException {
		return mBackend.startKeg(tap, beerName, brewerName, styleName, kegType);
	}

	@Override
	public Models.AuthenticationToken assignToken(String authDevice, String tokenValue, String username) throws BackendException {
		return mBackend.assignToken(authDevice, tokenValue, username);
	}

	@Override
	public Models.Image attachPictureToDrink(int drinkId, File picture) throws BackendException {
		return mBackend.attachPictureToDrink(drinkId, picture);
	}

	@Override
	public Models.Keg endKeg(Models.Keg keg) throws BackendException {
		return mBackend.endKeg(keg);
	}

	@Override
	public Models.AuthenticationToken getAuthToken(String authDevice, String tokenValue) throws BackendException {
		return mBackend.getAuthToken(authDevice, tokenValue);
	}

	@Override
	public Models.Session getCurrentSession() throws BackendException {
		return mBackend.getCurrentSession();
	}

	@Override
	public List<Models.SystemEvent> getEvents() throws BackendException {
		return mBackend.getEvents();
	}

	@Override
	public List<Models.SystemEvent> getEventsSince(long sinceEventId) throws BackendException {
		return mBackend.getEventsSince(sinceEventId);
	}

	@Override
	public JsonNode getSessionStats(int sessionId) throws BackendException {
		return mBackend.getSessionStats(sessionId);
	}

	@Override
	public List<Models.SoundEvent> getSoundEvents() throws BackendException {
		return mBackend.getSoundEvents();
	}

	@Override
	public List<Models.KegTap> getTaps() throws BackendException {
		return mBackend.getTaps();
	}

	@Override
	public Models.KegTap createTap(String tapName) throws BackendException {
		return mBackend.createTap(tapName);
	}

	@Override
	public void deleteTap(Models.KegTap tap) throws BackendException {
		mBackend.deleteTap(tap);
	}

	@Override
	public Models.User createUser(String username, String email, String password, String imagePath) throws BackendException {

		Account account = mClient.instantiate(Account.class);

		if (password == null) {
			password = "SK" + UUID.randomUUID().toString();
		}

		account.setEmail(email);
		account.setUsername(username);
		account.setPassword(password);

		account.setGivenName("Joe");
		account.setSurname("Shmoe");

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
			account = getApplication().createAccount(account);
		} catch (ResourceException ex) {
			Log.e(TAG, "unable to create account", ex);
			throw new StormpathApiException("unable to create account: "+ ex.getDeveloperMessage(), ex);
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
		Iterator<Account> accounts = getApplication().getAccounts(where).iterator();
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
		for (Account account : getApplication().getAccounts()) {
			Models.User.Builder builder = Models.User.newBuilder();
			if (builders.containsKey(account.getUsername()))
				builder = builders.get(account.getUsername());
			users.add(StormpathAccountBridge.userFromAccount(account, builder));
		}

		return users;

	}

	@Nullable
	@Override
	public Models.Drink recordDrink(String tapName, long volumeMl, long ticks, @Nullable String shout, @Nullable String username, @Nullable String recordDate, long durationMillis, @Nullable TimeSeries timeSeries, @Nullable File picture) throws BackendException {
		return mBackend.recordDrink(tapName, volumeMl, ticks, shout, username, recordDate, durationMillis, timeSeries, picture);
	}

	@Override
	public Models.ThermoLog recordTemperature(Api.RecordTemperatureRequest request) throws BackendException {
		return mBackend.recordTemperature(request);
	}

	@Override
	public Models.FlowMeter calibrateMeter(Models.FlowMeter meter, double ticksPerMl) throws BackendException {
		return mBackend.calibrateMeter(meter, ticksPerMl);
	}

	@Override
	public Models.Controller createController(String name, String serialNumber, String deviceType) throws BackendException {
		return mBackend.createController(name, serialNumber, deviceType);
	}

	@Override
	public List<Models.Controller> getControllers() throws BackendException {
		return mBackend.getControllers();
	}

	@Override
	public Models.Controller updateController(Models.Controller controller) throws BackendException {
		return mBackend.updateController(controller);
	}

	@Override
	public Models.FlowMeter createFlowMeter(Models.Controller controller, String portName, double ticksPerMl) throws BackendException {
		return mBackend.createFlowMeter(controller, portName, ticksPerMl);
	}

	@Override
	public List<Models.FlowMeter> getFlowMeters() throws BackendException {
		return mBackend.getFlowMeters();
	}

	@Override
	public Models.FlowMeter updateFlowMeter(Models.FlowMeter flowMeter) throws BackendException {
		return mBackend.updateFlowMeter(flowMeter);
	}

	@Override
	public Models.KegTap connectMeter(Models.KegTap tap, Models.FlowMeter meter) throws BackendException {
		return mBackend.connectMeter(tap, meter);
	}

	@Override
	public Models.KegTap disconnectMeter(Models.KegTap tap) throws BackendException {
		return mBackend.disconnectMeter(tap);
	}

	@Override
	public List<Models.FlowToggle> getFlowToggles() throws BackendException {
		return mBackend.getFlowToggles();
	}

	@Override
	public Models.FlowToggle updateFlowToggle(Models.FlowToggle flowToggle) throws BackendException {
		return mBackend.updateFlowToggle(flowToggle);
	}

	@Override
	public Models.KegTap connectToggle(Models.KegTap tap, Models.FlowToggle toggle) throws BackendException {
		return mBackend.connectToggle(tap, toggle);
	}

	@Override
	public Models.KegTap disconnectToggle(Models.KegTap tap) throws BackendException {
		return mBackend.disconnectToggle(tap);
	}
}