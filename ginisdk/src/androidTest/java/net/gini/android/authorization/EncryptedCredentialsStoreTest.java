package net.gini.android.authorization;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.test.AndroidTestCase;

import net.gini.android.authorization.crypto.GiniCryptoHelper;

/**
 * Created by Alpar Szotyori on 08.10.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */
public class EncryptedCredentialsStoreTest extends AndroidTestCase {

    private SharedPreferences mSharedPreferences;
    private EncryptedCredentialsStore mCredentialsStore;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mSharedPreferences = getContext().getSharedPreferences("GiniTests", MODE_PRIVATE);
        mSharedPreferences.edit().clear().commit();

        mCredentialsStore = new EncryptedCredentialsStore(mSharedPreferences, getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mSharedPreferences.edit().clear().commit();
    }

    public void testEncryptsExistingPlaintextCredentials() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentialsWithoutEncryption(userCredentials);
        // When
        mCredentialsStore.encryptExistingPlaintextCredentials();
        // Then
        final UserCredentials encryptedUserCredentials =
                mCredentialsStore.getEncryptedUserCredentials();
        assertTrue(!userCredentials.getUsername().equals(encryptedUserCredentials.getUsername()));
        assertTrue(!userCredentials.getPassword().equals(encryptedUserCredentials.getPassword()));
    }

    public void testDecryptsEncryptedExistingPlaintextCredentials() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentialsWithoutEncryption(userCredentials);
        // When
        mCredentialsStore.encryptExistingPlaintextCredentials();
        // Then
        final UserCredentials decryptedUserCredentials = mCredentialsStore.getUserCredentials();
        assertEquals(userCredentials.getUsername(), decryptedUserCredentials.getUsername());
        assertEquals(userCredentials.getPassword(), decryptedUserCredentials.getPassword());
    }

    public void testDoesNotEncryptAlreadyEncryptedCredentials() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentials(userCredentials);
        final UserCredentials encryptedUserCredentialsBefore =
                mCredentialsStore.getEncryptedUserCredentials();
        // When
        mCredentialsStore.encryptExistingPlaintextCredentials();
        // Then
        final UserCredentials encryptedUserCredentialsAfter =
                mCredentialsStore.getEncryptedUserCredentials();
        assertEquals(encryptedUserCredentialsBefore.getUsername(),
                encryptedUserCredentialsAfter.getUsername());
        assertEquals(encryptedUserCredentialsBefore.getPassword(),
                encryptedUserCredentialsAfter.getPassword());
    }

    public void testEncryptsCredentials() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentials(userCredentials);
        // Then
        final UserCredentials encryptedUserCredentials =
                mCredentialsStore.getEncryptedUserCredentials();
        assertTrue(!userCredentials.getUsername().equals(encryptedUserCredentials.getUsername()));
        assertTrue(!userCredentials.getPassword().equals(encryptedUserCredentials.getPassword()));
    }

    public void testDecryptsCredentials() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentials(userCredentials);
        // When
        final UserCredentials decryptedUserCredentials = mCredentialsStore.getUserCredentials();
        // Then
        assertEquals(userCredentials.getUsername(), decryptedUserCredentials.getUsername());
        assertEquals(userCredentials.getPassword(), decryptedUserCredentials.getPassword());
    }

    public void testDeleteCredentials() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentials(userCredentials);
        // When
        mCredentialsStore.deleteUserCredentials();
        // Then
        assertNull(mCredentialsStore.getUserCredentials());
    }

    public void testSetsEncryptionVersion() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentials(userCredentials);
        // Then
        final int encryptionVersion = mCredentialsStore.getEncryptionVersion();
        assertEquals(encryptionVersion, EncryptedCredentialsStore.ENCRYPTION_VERSION);
    }

    public void testEncryptionIsDifferentForSameCredentials() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentials(userCredentials);
        final UserCredentials encryptedCredentials1 = mCredentialsStore.getEncryptedUserCredentials();
        // When
        mCredentialsStore.storeUserCredentials(userCredentials);
        final UserCredentials encryptedCredentials2 = mCredentialsStore.getEncryptedUserCredentials();
        // Then
        assertTrue(!encryptedCredentials1.getUsername().equals(encryptedCredentials2.getUsername()));
        assertTrue(!encryptedCredentials1.getPassword().equals(encryptedCredentials2.getPassword()));
    }

    public void testReturnsNullCredentialsIfTheEncryptionKeyChanged() throws Exception {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentials(userCredentials);
        // When
        GiniCryptoHelper.deleteSecretKey(mCredentialsStore.getGiniCrypto());
        // Then
        final UserCredentials encryptedUserCredentials = mCredentialsStore.getUserCredentials();
        assertNull(encryptedUserCredentials);
    }
}