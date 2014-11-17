package net.gini.android;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.test.InstrumentationTestCase;

import net.gini.android.authorization.Session;
import net.gini.android.authorization.SessionManager;
import net.gini.android.models.Document;
import net.gini.android.models.SpecificExtraction;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import bolts.Task;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DocumentTaskManagerTests extends InstrumentationTestCase {

    private DocumentTaskManager mDocumentTaskManager;
    private SessionManager mSessionManager;
    private ApiCommunicator mApiCommunicator;
    private Session mSession;

    public void setUp() {
        // https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());

        mApiCommunicator = Mockito.mock(ApiCommunicator.class);
        mSessionManager = Mockito.mock(SessionManager.class);
        mDocumentTaskManager = new DocumentTaskManager(mApiCommunicator, mSessionManager);

        // Always mock the session away since it is not what is tested here.
        mSession = new Session("1234-5678-9012", new Date(new Date().getTime() + 10000));
        when(mSessionManager.getSession()).thenReturn(Task.forResult(mSession));
    }

    private Bitmap createBitmap() throws IOException {
        AssetManager assetManager = getInstrumentation().getContext().getResources().getAssets();

        InputStream inputStream;
        inputStream = assetManager.open("yoda.jpg");
        return BitmapFactory.decodeStream(inputStream);
    }

    private Task<JSONObject> createDocumentJSONTask(final String documentId) throws IOException, JSONException {
        InputStream inputStream = getInstrumentation().getContext().getResources().getAssets().open("document.json");
        int size = inputStream.available();
        byte[] buffer = new byte[size];
        inputStream.read(buffer);
        inputStream.close();

        final JSONObject responseData = new JSONObject(new String(buffer));
        responseData.put("id", documentId);
        return Task.forResult(responseData);
    }

    private Task<JSONObject> createExtractionsJSONTask() throws IOException, JSONException {
        InputStream inputStream = getInstrumentation().getContext().getResources().getAssets().open("extractions.json");
        int size = inputStream.available();
        byte[] buffer = new byte[size];
        inputStream.read(buffer);
        inputStream.close();

        final JSONObject responseData = new JSONObject(new String(buffer));
        return Task.forResult(responseData);
    }

    public void testThatConstructorChecksForNull() {
        try {
            new DocumentTaskManager(null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            new DocumentTaskManager(mApiCommunicator, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    public void testThatCreateDocumentReturnsTask() throws IOException {
        Bitmap bitmap = createBitmap();

        assertNotNull(mDocumentTaskManager.createDocument(bitmap, "foobar.jpg", "invoice", 85));
    }

    public void testThatCreateDocumentResolvesToDocument() throws IOException, JSONException, InterruptedException {
        when(mApiCommunicator.uploadDocument(any(byte[].class), any(String.class), any(String.class), any(String.class),
                                             any(Session.class)))
                .thenReturn(createDocumentJSONTask("1234"));
        Bitmap bitmap = createBitmap();

        Task<Document> documentTask = mDocumentTaskManager.createDocument(bitmap, "foobar.jpg", "invoice", 95);
        documentTask.waitForCompletion();

        assertNotNull(documentTask.getResult());

    }

    public void testThatCreateDocumentsSubmitsTheFileNameAndDocumentType()
            throws IOException, JSONException, InterruptedException {
        when(mApiCommunicator.uploadDocument(any(byte[].class), any(String.class), any(String.class), any(String.class),
                                             any(Session.class)))
                .thenReturn(createDocumentJSONTask("1234"));

        Bitmap bitmap = createBitmap();
        mDocumentTaskManager.createDocument(bitmap, "foobar.jpg", "invoice", 90).waitForCompletion();

        verify(mApiCommunicator)
                .uploadDocument(any(byte[].class), eq(MediaTypes.IMAGE_JPEG), eq("foobar.jpg"), eq("invoice"),
                                eq(mSession));
    }

    public void testDocumentBuilderPassesThroughArguments() throws IOException {
        final DocumentTaskManager documentTaskManager = Mockito.mock(DocumentTaskManager.class);

        Bitmap bitmap = createBitmap();
        DocumentTaskManager.DocumentUploadBuilder documentUploadBuilder =
                new DocumentTaskManager.DocumentUploadBuilder(bitmap);
        documentUploadBuilder.setDocumentType("invoice");
        documentUploadBuilder.setFilename("foobar.jpg");
        documentUploadBuilder.setCompressionRate(12);
        documentUploadBuilder.upload(documentTaskManager);

        verify(documentTaskManager).createDocument(bitmap, "foobar.jpg", "invoice", 12);
    }

    public void testDocumentBuilderHasDefaultValues() throws IOException {
        final DocumentTaskManager documentTaskManager = Mockito.mock(DocumentTaskManager.class);

        Bitmap bitmap = createBitmap();
        new DocumentTaskManager.DocumentUploadBuilder(bitmap).upload(documentTaskManager);

        verify(documentTaskManager).createDocument(bitmap, null, null, DocumentTaskManager.DEFAULT_COMPRESSION);
    }

    public void testGetExtractionsReturnsTask() throws IOException, JSONException {
        when(mApiCommunicator.getExtractions(eq("1234"), any(Session.class))).thenReturn(createExtractionsJSONTask());
        Document document = new Document("1234", Document.ProcessingState.COMPLETED, "foobar", 1, new Date(),
                                         Document.SourceClassification.NATIVE);

        assertNotNull(mDocumentTaskManager.getExtractions(document));
    }

    public void testGetExtractionsResolvesToHashMap() throws Exception {
        when(mApiCommunicator.getExtractions(eq("1234"), any(Session.class))).thenReturn(createExtractionsJSONTask());
        Document document = new Document("1234", Document.ProcessingState.COMPLETED, "foobar", 1, new Date(),
                                         Document.SourceClassification.NATIVE);

        Task<Map<String, SpecificExtraction>> extractionsTask = mDocumentTaskManager.getExtractions(document);
        extractionsTask.waitForCompletion();
        if (extractionsTask.isFaulted()) {
            throw extractionsTask.getError();
        }
        assertNotNull(extractionsTask.getResult());
    }
}
