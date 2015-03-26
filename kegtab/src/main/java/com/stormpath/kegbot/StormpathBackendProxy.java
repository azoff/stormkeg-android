package com.stormpath.kegbot;

import android.content.Context;
import com.stormpath.sdk.api.ApiKey;
import com.stormpath.sdk.api.ApiKeys;
import com.stormpath.sdk.client.Client;
import com.stormpath.sdk.client.Clients;
import org.codehaus.jackson.JsonNode;
import org.kegbot.app.KegbotApplication;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.util.TimeSeries;
import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;
import org.kegbot.proto.Api;
import org.kegbot.proto.Models;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

/**
 * Created by Azoff on 3/25/15.
 */
public class StormpathBackendProxy implements Backend {

	private Client mClient;
	private Backend mBackend;

	public StormpathBackendProxy(Client client, Backend backend) {
		mClient = client;
		mBackend = backend;
	}

	public static StormpathBackendProxy fromContext(Context context, Backend backend) {
		final AppConfiguration config = KegbotApplication.get(context).getConfig();
		ApiKey key = ApiKeys.builder().setId(config.getStormpathId()).setSecret(config.getStormpathSecret()).build();
		Client client = Clients.builder().setApiKey(key).build();
		return new StormpathBackendProxy(client, backend);
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
	public Models.User createUser(String username, String email, String password, String imagePath) throws BackendException {
		return mBackend.createUser(username, email, password, imagePath);
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
	public Models.User getUser(String username) throws BackendException {
		return mBackend.getUser(username);
	}

	@Override
	public List<Models.User> getUsers() throws BackendException {
		return mBackend.getUsers();
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
