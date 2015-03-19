package net.gini.android;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.test.InstrumentationTestCase;

import net.gini.android.authorization.Session;
import net.gini.android.authorization.SessionManager;
import net.gini.android.models.Document;
import net.gini.android.models.Extraction;
import net.gini.android.models.SpecificExtraction;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

    private JSONObject readJSONFile(final String filename) throws IOException, JSONException{
        InputStream inputStream = getInstrumentation().getContext().getResources().getAssets().open(filename);
        int size = inputStream.available();
        byte[] buffer = new byte[size];
        @SuppressWarnings("unused")
        int read = inputStream.read(buffer);
        inputStream.close();
        return new JSONObject(new String(buffer));
    }

    private Task<JSONObject> createDocumentJSONTask(final String documentId) throws IOException, JSONException {
        final JSONObject responseData = readJSONFile("document.json");
        responseData.put("id", documentId);
        return Task.forResult(responseData);
    }

    private Task<JSONObject> createDocumentJSONTask(final String documentId, final String processingState)
            throws IOException, JSONException {
        final JSONObject responseData = readJSONFile("document.json");
        responseData.put("id", documentId);
        responseData.put("progress", processingState);
        return Task.forResult(responseData);
    }

    private Task<JSONObject> createExtractionsJSONTask() throws IOException, JSONException {
        return Task.forResult(readJSONFile("extractions.json"));
    }

    private Task<JSONObject> createErrorReportJSONTask(final String errorId) throws JSONException {
        final JSONObject responseData = new JSONObject();
        responseData.put("errorId", errorId);
        responseData.put("message", "error was reported, please refer to the given error id");
        return Task.forResult(responseData);
    }

    private Task<JSONObject> createLayoutJSONTask() throws IOException, JSONException {
        return Task.forResult(readJSONFile("layout.json"));
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
        final Uri createdDocumentUri = Uri.parse("https://api.gini.net/documents/1234");
        when(mApiCommunicator.uploadDocument(any(byte[].class), any(String.class), any(String.class), any(String.class),
                                             any(Session.class)))
                .thenReturn(Task.forResult(createdDocumentUri));
        when(mApiCommunicator.getDocument(eq(createdDocumentUri), any(Session.class))).thenReturn(
                createDocumentJSONTask("1234"));
        Bitmap bitmap = createBitmap();

        Task<Document> documentTask = mDocumentTaskManager.createDocument(bitmap, "foobar.jpg", "invoice", 95);
        documentTask.waitForCompletion();

        assertNotNull(documentTask.getResult());

    }

    public void testThatCreateDocumentsSubmitsTheFileNameAndDocumentType()
            throws IOException, JSONException, InterruptedException {
        final Uri createdDocumentUri = Uri.parse("https://api.gini.net/documents/1234");
        when(mApiCommunicator.uploadDocument(any(byte[].class), any(String.class), any(String.class), any(String.class),
                                             any(Session.class)))
                .thenReturn(Task.forResult(Uri.parse("https://api.gini.net/documents/1234")));
        when(mApiCommunicator.getDocument(eq(createdDocumentUri), any(Session.class))).thenReturn(
                createDocumentJSONTask("1234"));

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

    @SuppressWarnings("ConstantConditions")
    public void testGetDocumentThrowsWithNullArgument() {
        final String documentId = null;
        try {
            mDocumentTaskManager.getDocument(documentId);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {}

        final Uri documentUri = null;
        try {
            mDocumentTaskManager.getDocument(documentUri);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {}
    }

    public void testGetDocumentReturnsTask() throws IOException, JSONException {
        when(mApiCommunicator.getDocument(eq("1234"), any(Session.class))).thenReturn(createDocumentJSONTask("1234"));

        Task<Document> documentTask = mDocumentTaskManager.getDocument("1234");

        assertNotNull(documentTask);
    }

    public void testGetDocumentResolvesToDocument() throws IOException, JSONException, InterruptedException {
        when(mApiCommunicator.getDocument(eq("1234"), any(Session.class))).thenReturn(createDocumentJSONTask("1234"));

        Task<Document> documentTask = mDocumentTaskManager.getDocument("1234");
        documentTask.waitForCompletion();
        Document document = documentTask.getResult();

        assertNotNull(document);
        assertEquals("1234", document.getId());
    }

    public void testPollDocumentThrowsWithNullArgument() {
        try {
            mDocumentTaskManager.pollDocument(null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {}
    }

    @SuppressWarnings("unchecked")
    public void testPollDocument() throws IOException, JSONException, InterruptedException {
        when(mApiCommunicator.getDocument(eq("1234"), any(Session.class))).thenReturn(
                createDocumentJSONTask("1234", "PENDING"), createDocumentJSONTask("1234", "COMPLETED"));
        Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                                         Document.SourceClassification.NATIVE);

        Task<Document> documentTask = mDocumentTaskManager.pollDocument(document);
        documentTask.waitForCompletion();

        Document polledDocument = documentTask.getResult();
        assertNotNull(polledDocument);
        assertEquals("1234", polledDocument.getId());
        assertEquals(Document.ProcessingState.COMPLETED, polledDocument.getState());

    }

    @SuppressWarnings("unchecked")
    public void testPollDocumentProcessingStateErrorCompletesTask()
            throws IOException, JSONException, InterruptedException {
        when(mApiCommunicator.getDocument(eq("1234"), any(Session.class))).thenReturn(
                createDocumentJSONTask("1234", "PENDING"), createDocumentJSONTask("1234", "ERROR"));
        Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                                         Document.SourceClassification.NATIVE);

        Task<Document> documentTask = mDocumentTaskManager.pollDocument(document);
        documentTask.waitForCompletion();

        Document polledDocument = documentTask.getResult();
        assertNotNull(polledDocument);
        assertEquals("1234", polledDocument.getId());
        assertEquals(Document.ProcessingState.ERROR, polledDocument.getState());
    }

    public void testSaveDocumentUpdatesThrowsWithNullArguments() throws JSONException {
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                                               Document.SourceClassification.NATIVE);

        try {
            mDocumentTaskManager.saveDocumentUpdates(null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {}

        try {
            mDocumentTaskManager.saveDocumentUpdates(document, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {}

        try {
            mDocumentTaskManager.saveDocumentUpdates(null, new HashMap<String, SpecificExtraction>());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {}
    }

    public void testSaveDocumentReturnsTask() throws JSONException {
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                                               Document.SourceClassification.NATIVE);
        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();

        assertNotNull(mDocumentTaskManager.saveDocumentUpdates(document, extractions));
    }

    public void testSaveDocumentResolvesToDocumentInstance() throws JSONException, InterruptedException {
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                                               Document.SourceClassification.NATIVE);
        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();
        when(mApiCommunicator.sendFeedback(eq("1234"), any(JSONObject.class), any(Session.class))).thenReturn(
                Task.forResult(new JSONObject()));

        Task<Document> updateTask = mDocumentTaskManager.saveDocumentUpdates(document, extractions);
        updateTask.waitForCompletion();
        assertNotNull(updateTask.getResult());
    }

    public void testSaveDocumentSavesExtractions() throws JSONException, InterruptedException {
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                                               Document.SourceClassification.NATIVE);
        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();
        extractions.put("amountToPay",
                        new SpecificExtraction("amountToPay", "42:EUR", "amount", null, new ArrayList<Extraction>()));
        extractions.put("senderName",
                        new SpecificExtraction("senderName", "blah", "senderName", null, new ArrayList<Extraction>()));

        extractions.get("amountToPay").setValue("23:EUR");
        mDocumentTaskManager.saveDocumentUpdates(document, extractions).waitForCompletion();

        ArgumentCaptor<JSONObject> dataCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(mApiCommunicator).sendFeedback(eq("1234"), dataCaptor.capture(), any(Session.class));
        final JSONObject updateData = dataCaptor.getValue();
        // Should update the amountToPay
        assertTrue(updateData.has("amountToPay"));
        final JSONObject amountToPay = updateData.getJSONObject("amountToPay");
        assertEquals("23:EUR", amountToPay.getString("value"));
        // Should not update the senderName, since isDirty() is false.
        assertFalse(updateData.has("senderName"));
    }

    public void testSaveDocumentMarksExtractionsAsNotDirty() throws JSONException, InterruptedException {
        when(mApiCommunicator.sendFeedback(eq("1234"), any(JSONObject.class), any(Session.class))).thenReturn(
                Task.forResult(new JSONObject()));
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                                               Document.SourceClassification.NATIVE);
        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();
        extractions.put("amountToPay",
                        new SpecificExtraction("amountToPay", "42:EUR", "amount", null, new ArrayList<Extraction>()));
        extractions.put("senderName",
                        new SpecificExtraction("senderName", "blah", "senderName", null, new ArrayList<Extraction>()));

        extractions.get("amountToPay").setValue("23:EUR");
        Task<Document> updateTask = mDocumentTaskManager.saveDocumentUpdates(document, extractions);

        updateTask.waitForCompletion();
        assertFalse(extractions.get("amountToPay").isDirty());
    }

    public void testReportDocumentThrowsWithNullArgument() {
        try {
            mDocumentTaskManager.reportDocument(null, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {}
    }

    public void testReportDocumentReturnsTask() throws JSONException {
        when(mApiCommunicator.errorReportForDocument(eq("1234"), eq("short summary"), eq("detailed description"),
                                                     any(Session.class)))
                .thenReturn(createErrorReportJSONTask("4444-3333"));
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                                               Document.SourceClassification.NATIVE);

        assertNotNull(mDocumentTaskManager.reportDocument(document, "short summary", "detailed description"));
    }

    public void testReportDocumentTaskResolvesToErrorId() throws JSONException, InterruptedException {
        when(mApiCommunicator.errorReportForDocument(eq("1234"), eq("short summary"), eq("detailed description"),
                                                     any(Session.class)))
                .thenReturn(createErrorReportJSONTask("4444-3333"));
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                                               Document.SourceClassification.NATIVE);

        Task<String> reportTask = mDocumentTaskManager.reportDocument(document, "short summary", "detailed description");
        reportTask.waitForCompletion();

        assertEquals("4444-3333", reportTask.getResult());
    }

    public void testGetLayoutThrowsWithNullArgument() {
        try {
            mDocumentTaskManager.getLayout(null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {}
    }

    public void testGetLayoutReturnsTask() {
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                                               Document.SourceClassification.NATIVE);

        assertNotNull(mDocumentTaskManager.getLayout(document));
    }

    public void testGetLayoutResolvesToJSON() throws IOException, JSONException, InterruptedException {
        when(mApiCommunicator.getLayoutForDocument(eq("1234"), any(Session.class))).thenReturn(createLayoutJSONTask());
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                                               Document.SourceClassification.NATIVE);

        final Task<JSONObject> layoutTask = mDocumentTaskManager.getLayout(document);
        layoutTask.waitForCompletion();
        final JSONObject responseData = layoutTask.getResult();


        assertNotNull(responseData);
    }
}
