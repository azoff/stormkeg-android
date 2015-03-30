package com.stormpath.kegbot;

import com.google.protobuf.InvalidProtocolBufferException;
import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.account.AccountStatus;
import com.stormpath.sdk.directory.CustomData;
import org.kegbot.proto.Models;

import java.nio.charset.Charset;

/**
 * Created by Azoff on 3/27/15.
 */
public class StormpathAccountBridge {

	public static Models.User userFromAccount(Account account) {
		return userFromAccount(account, Models.User.newBuilder());
	}

	public static Models.User userFromAccount(Account account, Models.User.Builder builder) {

		CustomData data = account.getCustomData();
		builder.setEmail(account.getEmail())
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
				byte[] imageData = String.valueOf(data.get(key)).getBytes();
				Models.Image image = Models.Image.parseFrom(imageData);
				builder.setImage(image);
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		}

		return builder.build();

	}

}
