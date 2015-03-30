/*
 * Copyright 2014 Bevbot LLC <info@bevbot.com>
 *
 * This file is part of the Kegtab package from the Kegbot project. For
 * more information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.app.setup;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.google.common.base.Strings;
import com.stormpath.kegbot.StormpathBackendProxy;
import com.stormpath.sdk.resource.ResourceException;
import org.kegbot.app.KegbotApplication;
import org.kegbot.app.R;
import org.kegbot.app.config.AppConfiguration;

import java.util.NoSuchElementException;

public class SetupStormpathFragment extends SetupFragment {

	private final String TAG = SetupStormpathFragment.class.getSimpleName();

	private EditText appNameEditText, idEditText, secretEditText;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.setup_stormpath_fragment, null);
		final AppConfiguration prefs = ((KegbotApplication) getActivity().getApplication()).getConfig();

		appNameEditText = (EditText) view.findViewById(R.id.stormpathAppName);
		idEditText = (EditText) view.findViewById(R.id.stormpathId);
		secretEditText = (EditText) view.findViewById(R.id.stormpathSecret);

		appNameEditText.setText(prefs.getStormpathAppName());
		idEditText.setText(prefs.getStormpathId());
		secretEditText.setText(prefs.getStormpathSecret());

		return view;

	}

	private String getAppName() {
		return appNameEditText.getText().toString();
	}

	private String getAppId() {
		return idEditText.getText().toString();
	}

	private String getAppSecret() {
		return secretEditText.getText().toString();
	}

	@Override
	public String validate() {

		final AppConfiguration prefs = ((KegbotApplication) getActivity().getApplication()).getConfig();
		final String id = getAppId();
		final String name = getAppName();
		final String secret = getAppSecret();

		final boolean idEmpty = Strings.isNullOrEmpty(id);
		final boolean secretEmpty = Strings.isNullOrEmpty(secret);
		final boolean nameEmpty = Strings.isNullOrEmpty(name);

		if (idEmpty || secretEmpty || nameEmpty) {
			if (idEmpty && secretEmpty && nameEmpty) {
				Log.d(TAG, "Skipping stormpath config...");
				return "";
			} else {
				return "Please fill out all three fields if you wish to connect to Stormpath";
			}
		}

		Log.d(TAG, "Testing stormpath config...");

		prefs.setStormpathId(id);
		prefs.setStormpathAppName(name);
		prefs.setStormpathSecret(secret);

		try {
			StormpathBackendProxy.fromContext(getActivity()).getApplication();
		} catch (NoSuchElementException ex) {
			return "Unable to find stormpath application: " + prefs.getStormpathAppName();
		} catch (ResourceException ex) {
			Log.e(TAG, "Client instantiation failed", ex);
			return "Unable to connect to Stormpath service: " + ex.getDeveloperMessage();
		}

		return "";
	}

}
