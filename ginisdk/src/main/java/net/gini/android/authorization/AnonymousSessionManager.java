package net.gini.android.authorization;


import java.util.concurrent.Future;


/**
 * The AnonymousSessionManager is a SessionManager implementation that uses anonymous Gini users.
 */
public class AnonymousSessionManager implements SessionManager {
    /** The UserCenterManager instance which is used to create and log in the anonymous users. */
    final private UserCenterManager mUserCenterManager;
    /** The credentials store which is used to store the user credentials. */
    final private CredentialsStore mCredentialsStore;

    /** The user's current session. */
    private Session mCurrentSession;

    public AnonymousSessionManager(UserCenterManager userCenterManager, CredentialsStore credentialsStore) {
        mUserCenterManager = userCenterManager;
        mCredentialsStore = credentialsStore;
    }

    @Override
    public Future<Session> getSession() {
        return null;
    }
}
