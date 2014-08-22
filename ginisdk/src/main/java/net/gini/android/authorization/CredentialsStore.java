package net.gini.android.authorization;


public interface CredentialsStore {

    public void storeUserCredentials(UserCredentials userCredentials);

    public UserCredentials getUserCredentials();
}
