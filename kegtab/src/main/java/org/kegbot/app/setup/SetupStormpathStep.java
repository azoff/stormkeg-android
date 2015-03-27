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

import android.app.Fragment;
import com.google.common.base.Strings;
import org.kegbot.app.R;

public class SetupStormpathStep extends SetupStep {

	private final SetupStormpathFragment mControlsFragment = new SetupStormpathFragment();

	public SetupStormpathStep(SetupActivity.SetupState state) {
		super(state);
	}

	@Override
	public Fragment getContentFragment() {
		return SetupTextFragment.withText(R.string.stormpath_title,
				R.string.stormpath_caption);
	}

	@Override
	public Fragment getControlsFragment() {
		return mControlsFragment;
	}

	@Override
	public SetupStep advance() throws SetupValidationException {
		final String error = mControlsFragment.validate();
		if (!Strings.isNullOrEmpty(error)) {
			mControlsFragment.onValidationFailed();
			throw new SetupValidationException(error);
		}

		return new SetupManagerPinStep(mState);
	}
}
