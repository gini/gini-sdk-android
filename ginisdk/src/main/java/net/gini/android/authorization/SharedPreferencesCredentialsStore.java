package net.gini.android.authorization;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import static net.gini.android.Utils.checkNotNull;


public class SharedPreferencesCredentialsStore implements CredentialsStore {

    protected static final String USERNAME_KEY = "GiniUsername";
    protected static final String PASSWORD_KEY = "GiniPassword";

    private final SharedPreferences mSharedPreferences;

    public SharedPreferencesCredentialsStore(final SharedPreferences mSharedPreferences) {
        this.mSharedPreferences = checkNotNull(mSharedPreferences);
    }

    @Override
    public boolean storeUserCredentials(UserCredentials userCredentials) {
        final Editor preferencesEditor = mSharedPreferences.edit();
        preferencesEditor.putString(USERNAME_KEY, userCredentials.getUsername());
        preferencesEditor.putString(PASSWORD_KEY, userCredentials.getPassword());
        return preferencesEditor.commit();
    }

    @Override
    public UserCredentials getUserCredentials() {
        final String username = mSharedPreferences.getString(USERNAME_KEY, null);
        final String password = mSharedPreferences.getString(PASSWORD_KEY, null);
        if (username != null && password != null) {
            return new UserCredentials(username, password);
        }
        return null;
    }
}
