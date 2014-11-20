package net.gini.android.authorization;

import org.json.JSONObject;

import bolts.Continuation;
import bolts.Task;


/**
 * The UserCenterManager is responsible for managing Gini users.
 */
public class UserCenterManager {
    final private UserCenterAPICommunicator mUserCenterAPICommunicator;

    // An active session for the User Center API.
    private Session mCurrentSession;

    /**
     * @param userCenterAPICommunicator An implementation of the UserCenterAPIManager which handles the
     *                                  communication with the Gini User Center API for this manager
     *                                  instance.
     */
    public UserCenterManager(final UserCenterAPICommunicator userCenterAPICommunicator) {
        mUserCenterAPICommunicator = userCenterAPICommunicator;
    }

    /**
     * Creates a new user which has the given client credentials.
     *
     * @param userCredentials           The user's credentials.
     * @return                          A (Bolts) task which will resolve to the freshly created user.
     */
    public Task<User> createUser(final UserCredentials userCredentials) {
        // The request needs a valid user session. Get the user session.
        return getUserCenterSession()
                // Next step: Do the user creation request.
                .onSuccessTask(new Continuation<Session, Task<String>>() {
                    @Override
                    public Task<String> then(Task<Session> sessionTask) throws Exception {
                        return mUserCenterAPICommunicator.createUser(userCredentials, sessionTask.getResult());
                    }
                })
                // And then create the user object from the API response.
                .onSuccess(new Continuation<String, User>() {
                    @Override
                    public User then(Task<String> task) throws Exception {
                        final String[] components = task.getResult().split("/");
                        final String userId = components[components.length - 1];
                        return new User(userId, userCredentials.getUsername());
                    }
                });
    }

    /**
     * Log-in the user which is identified with the given credentials.
     *
     * @param userCredentials           The user's credentials.
     * @return                          A (Bolts) task which will resolve to a session that can be used to do
     *                                  requests to the Gini API.
     */
    public Task<Session> loginUser(final UserCredentials userCredentials) {
        Task<JSONObject> loginTask = mUserCenterAPICommunicator.loginUser(userCredentials);
        return loginTask.onSuccess(new Continuation<JSONObject, Session>() {
            @Override
            public Session then(Task<JSONObject> task) throws Exception {
                return Session.fromAPIResponse(task.getResult());
            }
        });
    }

    /**
     * Returns a future that will resolve to a valid session (for the User Center API!).
     */
    protected synchronized Task<Session> getUserCenterSession() {
        // Reuse the current session if possible.
        if (mCurrentSession != null && !mCurrentSession.hasExpired()) {
            return Task.forResult(mCurrentSession);
        }
        // Or do a login.
        return loginClient();
    }

    protected Task<Session> loginClient() {
        final Task<JSONObject> loginClient = mUserCenterAPICommunicator.loginClient();
        final UserCenterManager userCenterManager = this;

        return loginClient.onSuccess(new Continuation<JSONObject, Session>() {
            @Override
            public Session then(Task<JSONObject> task) throws Exception {
                Session session = Session.fromAPIResponse(task.getResult());
                synchronized (userCenterManager) {
                    mCurrentSession = session;
                }
                return session;
            }
        });
    }
}
