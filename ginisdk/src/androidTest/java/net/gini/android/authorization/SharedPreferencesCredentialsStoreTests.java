package net.gini.android.authorization;

import android.content.SharedPreferences;
import android.test.AndroidTestCase;

import static android.content.Context.MODE_PRIVATE;
import static android.content.SharedPreferences.Editor;


public class SharedPreferencesCredentialsStoreTests extends AndroidTestCase {

    private SharedPreferencesCredentialsStore mCredentialsStore;
    private SharedPreferences mSharedPreferences;

    @Override
    public void setUp() {
        mSharedPreferences = getContext().getSharedPreferences("GiniTests", MODE_PRIVATE);
        // Clear preferences from previous tests
        final Editor preferencesEditor = mSharedPreferences.edit();
        preferencesEditor.clear();
        preferencesEditor.commit();

        mCredentialsStore = new SharedPreferencesCredentialsStore(mSharedPreferences);
    }

    public void testConstructionThrowsNullPointerExceptionForNullArgument() {
        try {
            new SharedPreferencesCredentialsStore(null);
            fail("NullPointerException not raised");
        } catch (NullPointerException ignored){}
    }

    public void testStoreCredentialsStoresCredentials() {
        final UserCredentials userCredentials = new UserCredentials("foo@example.com", "1234");

        assertTrue(mCredentialsStore.storeUserCredentials(userCredentials));

        assertEquals("foo@example.com",
                     mSharedPreferences.getString(SharedPreferencesCredentialsStore.USERNAME_KEY, null));
        assertEquals("1234",
                     mSharedPreferences.getString(SharedPreferencesCredentialsStore.PASSWORD_KEY, null));
    }

    public void testGetCredentialsReturnsNullIfNoCredentialsAreStored() {
        assertNull(mCredentialsStore.getUserCredentials());
    }

    public void testGetCredentialsReturnsUserCredentials() {
        final UserCredentials storedUserCredentials = new UserCredentials("foo@example.com", "1234");
        mCredentialsStore.storeUserCredentials(storedUserCredentials);

        final UserCredentials userCredentials = mCredentialsStore.getUserCredentials();
        assertEquals("foo@example.com", userCredentials.getUsername());
        assertEquals("1234", userCredentials.getPassword());
    }
}
