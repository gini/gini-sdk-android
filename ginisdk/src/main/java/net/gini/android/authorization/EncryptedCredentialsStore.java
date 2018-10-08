package net.gini.android.authorization;

import static net.gini.android.Utils.checkNotNull;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import net.gini.android.authorization.crypto.GiniCrypto;
import net.gini.android.authorization.crypto.GiniCryptoException;

/**
 * Created by Alpar Szotyori on 08.10.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */
public class EncryptedCredentialsStore implements CredentialsStore {

    static final String ENCRYPTION_VERSION_KEY = "GiniEncryptionVersion";
    static final int ENCRYPTION_VERSION = 1;

    @VisibleForTesting
    final SharedPreferences mSharedPreferences;
    @VisibleForTesting
    final SharedPreferencesCredentialsStore mSharedPreferencesCredentialsStore;
    private final GiniCrypto mCrypto;

    public EncryptedCredentialsStore(@NonNull final SharedPreferences sharedPreferences,
            @NonNull final Context context) {
        mSharedPreferences = checkNotNull(sharedPreferences);
        mSharedPreferencesCredentialsStore = new SharedPreferencesCredentialsStore(
                sharedPreferences);
        mCrypto = GiniCrypto.newInstance(sharedPreferences, context);
        encryptExistingCredentials();
    }

    @VisibleForTesting
    void encryptExistingCredentials() {
        final UserCredentials userCredentials =
                mSharedPreferencesCredentialsStore.getUserCredentials();
        if (userCredentials != null && !isEncrypted()) {
            storeUserCredentials(userCredentials);
        }
    }

    private boolean isEncrypted() {
        return mSharedPreferences.getInt(ENCRYPTION_VERSION_KEY, 0) != 0;
    }

    private void setEncryptionVersion() {
        mSharedPreferences.edit()
                .putInt(ENCRYPTION_VERSION_KEY, ENCRYPTION_VERSION)
                .apply();
    }

    private void removeEncryptionVersion() {
        mSharedPreferences.edit()
                .remove(ENCRYPTION_VERSION_KEY)
                .apply();
    }

    @Override
    public boolean storeUserCredentials(UserCredentials userCredentials) {
        try {
            final UserCredentials encryptedUserCredentials = new UserCredentials(
                    mCrypto.encrypt(userCredentials.getUsername()),
                    mCrypto.encrypt(userCredentials.getPassword()));
            final boolean stored = mSharedPreferencesCredentialsStore.storeUserCredentials(
                    encryptedUserCredentials);
            if (stored) {
                setEncryptionVersion();
            }
            return stored;
        } catch (GiniCryptoException ignored) {
        }
        return false;
    }

    @Override
    public UserCredentials getUserCredentials() {
        final UserCredentials encryptedUserCredentials =
                mSharedPreferencesCredentialsStore.getUserCredentials();
        if (encryptedUserCredentials != null) {
            try {
                return new UserCredentials(mCrypto.decrypt(encryptedUserCredentials.getUsername()),
                        mCrypto.decrypt(encryptedUserCredentials.getPassword()));
            } catch (GiniCryptoException ignored) {
            }
        }
        return null;
    }

    @Override
    public boolean deleteUserCredentials() {
        removeEncryptionVersion();
        return mSharedPreferencesCredentialsStore.deleteUserCredentials();
    }
}
