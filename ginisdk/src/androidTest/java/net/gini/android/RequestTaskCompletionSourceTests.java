package net.gini.android;

import android.test.AndroidTestCase;

import com.android.volley.VolleyError;

import bolts.Task;


public class RequestTaskCompletionSourceTests extends AndroidTestCase {

    public void testNewRequestTaskCompletionSource() {
        assertNotNull(RequestTaskCompletionSource.newCompletionSource());
    }

    public void testGetTaskShouldReturnTask() {
        RequestTaskCompletionSource<String> requestTaskCompletionSource = RequestTaskCompletionSource.newCompletionSource();
        assertNotNull(requestTaskCompletionSource.getTask());
    }

    public void testCompletesTask() {
        RequestTaskCompletionSource<String> requestTaskCompletionSource = RequestTaskCompletionSource.newCompletionSource();
        Task<String> task= requestTaskCompletionSource.getTask();

        requestTaskCompletionSource.onResponse("foobar");

        assertEquals("foobar", task.getResult());
        assertFalse(task.isFaulted());
        assertNull(task.getError());
    }

    public void testResolvesError() {
        RequestTaskCompletionSource<String> requestTaskCompletionSource = RequestTaskCompletionSource.newCompletionSource();
        Task<String> task = requestTaskCompletionSource.getTask();
        VolleyError error = new VolleyError();
        requestTaskCompletionSource.onErrorResponse(error);

        assertEquals(error, task.getError());
        assertTrue(task.isFaulted());
        assertNull(task.getResult());
    }
}
