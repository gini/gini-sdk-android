package net.gini.android;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import bolts.Task;


/**
 * Handy class to wrap Bolt's tasks around a volley request. Can be used as both the response
 * listener and error listener of a volley request.
 *
 * @param <T> The response type of the request.
 */
public class RequestTaskCompletionSource<T> implements Response.Listener<T>, Response.ErrorListener {
    private Request<?> mRequest;
    private boolean mResultReceived = false;
    private VolleyError mError;
    private Task<T>.TaskCompletionSource mCompletionSource;

    public static <E> RequestTaskCompletionSource<E> newTask() {
        return new RequestTaskCompletionSource<E>();
    }

    private RequestTaskCompletionSource() {
        mCompletionSource = Task.create();
    }

    public void setRequest(Request<?> request) {
        mRequest = request;
    }


    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (mRequest == null) {
            return false;
        }

        if (!isDone()) {
            mRequest.cancel();
            return true;
        } else {
            return false;
        }
    }

    public boolean isCancelled() {
        if (mRequest == null) {
            return false;
        }
        return mRequest.isCanceled();
    }

    public synchronized boolean isDone() {
        return mResultReceived || mError != null || isCancelled();
    }

    public synchronized Task<T> getTask() {
        return mCompletionSource.getTask();
    }

    @Override
    public synchronized void onResponse(T response) {
        mResultReceived = true;
        mCompletionSource.setResult(response);
        notifyAll();
    }

    @Override
    public synchronized void onErrorResponse(VolleyError error) {
        mError = error;
        mCompletionSource.setError(error);
        notifyAll();
    }
}

