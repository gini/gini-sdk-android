package net.gini.android.authorization;


import java.util.concurrent.Future;


public interface SessionManager {
    public Future<Session> getSession();
}
