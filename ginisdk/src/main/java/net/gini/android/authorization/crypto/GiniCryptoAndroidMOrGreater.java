package net.gini.android.authorization.crypto;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Base64;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Created by Alpar Szotyori on 08.10.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class GiniCryptoAndroidMOrGreater extends GiniCrypto {

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String SECRET_KEY_ALIAS = "GiniCryptoKey";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final byte[] FIXED_IV = new byte[12];

    @Override
    public String encrypt(@NonNull final String text) throws GiniCryptoException {
        try {
            Cipher cipher = Cipher.getInstance(AES_MODE);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), new GCMParameterSpec(128, FIXED_IV));
            byte[] encodedBytes = cipher.doFinal(text.getBytes());
            return Base64.encodeToString(encodedBytes, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | KeyStoreException
                | InvalidKeyException | InvalidAlgorithmParameterException
                | UnrecoverableKeyException | BadPaddingException | NoSuchProviderException
                | IllegalBlockSizeException | CertificateException | IOException e) {
            throw new GiniCryptoException(e);
        }
    }

    @Override
    public String decrypt(@NonNull final String encrypted) throws GiniCryptoException {
        try {
            Cipher cipher = Cipher.getInstance(AES_MODE);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(128, FIXED_IV));
            final byte[] encryptedBytes = Base64.decode(encrypted, Base64.DEFAULT);
            final byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | KeyStoreException
                | InvalidKeyException | InvalidAlgorithmParameterException
                | UnrecoverableKeyException | BadPaddingException | NoSuchProviderException
                | IllegalBlockSizeException | CertificateException | IOException e) {
            throw new GiniCryptoException(e);
        }
    }

    private Key getSecretKey()
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException,
            NoSuchProviderException, InvalidAlgorithmParameterException, UnrecoverableKeyException {
        if (!hasSecretKey()) {
            generateSecretKey();
        }
        return getKeyStore().getKey(SECRET_KEY_ALIAS, null);
    }

    private boolean hasSecretKey()
            throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        return getKeyStore().containsAlias(SECRET_KEY_ALIAS);
    }

    private KeyStore getKeyStore()
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        return keyStore;
    }

    private void generateSecretKey() throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE);
        final KeyGenParameterSpec spec = new KeyGenParameterSpec
                .Builder(SECRET_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(false)
                .build();
        keyGenerator.init(spec);
        keyGenerator.generateKey();
    }
}
