package net.gini.android.authorization.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 08.10.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */
public abstract class GiniCrypto {

    public static GiniCrypto newInstance(@NonNull final SharedPreferences sharedPreferences,
            @NonNull final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new GiniCryptoAndroidMOrGreater();
        } else {
            return new GiniCryptoPreAndroidM(sharedPreferences, context);
        }
    }

    public abstract String encrypt(@NonNull final String text) throws GiniCryptoException;

    public abstract String decrypt(@NonNull final String encrypted) throws GiniCryptoException;

}
