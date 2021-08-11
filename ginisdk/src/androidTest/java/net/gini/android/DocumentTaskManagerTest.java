package net.gini.android;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static net.gini.android.Utils.CHARSET_UTF8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.test.filters.MediumTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.gini.android.DocumentTaskManager.DocumentType;
import net.gini.android.authorization.Session;
import net.gini.android.authorization.SessionManager;
import net.gini.android.helpers.TestUtils;
import net.gini.android.models.CompoundExtraction;
import net.gini.android.models.Document;
import net.gini.android.models.Extraction;
import net.gini.android.models.ExtractionsContainer;
import net.gini.android.models.ReturnReason;
import net.gini.android.models.SpecificExtraction;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import bolts.Task;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class DocumentTaskManagerTest {

    private DocumentTaskManager mDocumentTaskManager;
    private SessionManager mSessionManager;
    private ApiCommunicator mApiCommunicator;
    private Session mSession;

    @Before
    public void setUp() {
        // https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getApplicationContext().getCacheDir().getPath());

        mApiCommunicator = Mockito.mock(ApiCommunicator.class);
        mSessionManager = Mockito.mock(SessionManager.class);
        mDocumentTaskManager = new DocumentTaskManager(mApiCommunicator, mSessionManager, GiniApiType.DEFAULT);

        // Always mock the session away since it is not what is tested here.
        mSession = new Session("1234-5678-9012", new Date(new Date().getTime() + 10000));
        when(mSessionManager.getSession()).thenReturn(Task.forResult(mSession));
    }

    private Bitmap createBitmap() throws IOException {
        AssetManager assetManager = getApplicationContext().getResources().getAssets();

        InputStream inputStream;
        inputStream = assetManager.open("yoda.jpg");
        return BitmapFactory.decodeStream(inputStream);
    }

    private byte[] createByteArray() throws IOException {
        AssetManager assetManager = getApplicationContext().getResources().getAssets();

        InputStream inputStream = assetManager.open("yoda.jpg");
        return TestUtils.createByteArray(inputStream);
    }

    private JSONObject readJSONFile(final String filename) throws IOException, JSONException {
        InputStream inputStream = getApplicationContext().getResources().getAssets().open(filename);
        int size = inputStream.available();
        byte[] buffer = new byte[size];
        @SuppressWarnings("unused")
        int read = inputStream.read(buffer);
        inputStream.close();
        return new JSONObject(new String(buffer));
    }

    private JSONObject createDocumentJSON(final String documentId) throws IOException,
            JSONException {
        BufferedReader inputStreamReader = null;
        try {
            final AssetManager assetManager =
                    getApplicationContext().getResources().getAssets();
            inputStreamReader = new BufferedReader(
                    new InputStreamReader(assetManager.open("document-template.json"), CHARSET_UTF8));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = inputStreamReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            String jsonString = stringBuilder.toString();
            jsonString = jsonString.replaceAll("\\$\\{id\\}", documentId);
            return new JSONObject(jsonString);
        } finally {
            if (inputStreamReader != null) {
                inputStreamReader.close();
            }
        }
    }

    private Task<JSONObject> createDocumentJSONTask(final String documentId) throws IOException, JSONException {
        final JSONObject responseData = createDocumentJSON(documentId);
        return Task.forResult(responseData);
    }

    private Task<JSONObject> createDocumentJSONTask() throws IOException, JSONException {
        final JSONObject responseData = readJSONFile("document.json");
        return Task.forResult(responseData);
    }

    private Task<JSONObject> createDocumentJSONTask(final String documentId, final String processingState)
            throws IOException, JSONException {
        final JSONObject responseData = createDocumentJSON(documentId);
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

    private Document createDocument(final String documentId) throws IOException, JSONException {
        final JSONObject responseData = createDocumentJSON(documentId);
        return Document.fromApiResponse(responseData);
    }

    private Document createDocument() throws IOException, JSONException {
        final JSONObject responseData = readJSONFile("document.json");
        return Document.fromApiResponse(responseData);
    }

    @Test
    public void testThatConstructorChecksForNull() {
        try {
            new DocumentTaskManager(null, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            new DocumentTaskManager(mApiCommunicator, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testThatCreateDocumentReturnsTask() throws IOException {
        Bitmap bitmap = createBitmap();

        assertNotNull(mDocumentTaskManager.createDocument(bitmap, "foobar.jpg", "invoice", 85));
    }

    @Test
    public void testThatCreateDocumentResolvesToDocument() throws IOException, JSONException, InterruptedException {
        final Uri createdDocumentUri = Uri.parse("https://api.gini.net/documents/1234");
        when(mApiCommunicator.uploadDocument(any(byte[].class), any(String.class), any(String.class), any(String.class),
                any(Session.class), nullable(DocumentMetadata.class)))
                .thenReturn(Task.forResult(createdDocumentUri));
        when(mApiCommunicator.getDocument(eq(createdDocumentUri), any(Session.class))).thenReturn(
                createDocumentJSONTask("1234"));
        Bitmap bitmap = createBitmap();

        Task<Document> documentTask = mDocumentTaskManager.createDocument(bitmap, "foobar.jpg", "invoice", 95);
        documentTask.waitForCompletion();

        assertNotNull(documentTask.getResult());
    }

    @Test
    public void testThatCreateDocumentsSubmitsTheFileNameAndDocumentType()
            throws IOException, JSONException, InterruptedException {
        final Uri createdDocumentUri = Uri.parse("https://api.gini.net/documents/1234");
        when(mApiCommunicator.uploadDocument(any(byte[].class), any(String.class), any(String.class), any(String.class),
                any(Session.class), any(DocumentMetadata.class)))
                .thenReturn(Task.forResult(Uri.parse("https://api.gini.net/documents/1234")));
        when(mApiCommunicator.getDocument(eq(createdDocumentUri), any(Session.class))).thenReturn(
                createDocumentJSONTask("1234"));

        Bitmap bitmap = createBitmap();
        mDocumentTaskManager.createDocument(bitmap, "foobar.jpg", "invoice", 90).waitForCompletion();

        verify(mApiCommunicator)
                .uploadDocument(any(byte[].class), eq(MediaTypes.IMAGE_JPEG), eq("foobar.jpg"), eq("invoice"),
                        eq(mSession), nullable(DocumentMetadata.class));
    }

    @Test
    public void testThatCreatePartialDocumentSetsTheCorrectContentType() throws Exception {
        final Uri createdDocumentUri = Uri.parse("https://api.gini.net/documents/1234");
        when(mApiCommunicator.uploadDocument(any(byte[].class), any(String.class),
                any(String.class), any(String.class),
                any(Session.class), any(DocumentMetadata.class)))
                .thenReturn(Task.forResult(Uri.parse("https://api.gini.net/documents/1234")));
        when(mApiCommunicator.getDocument(eq(createdDocumentUri), any(Session.class))).thenReturn(
                createDocumentJSONTask("1234"));

        final byte[] document = new byte[]{0x01, 0x02};
        mDocumentTaskManager.createPartialDocument(document, MediaTypes.IMAGE_JPEG, "foobar.jpg",
                DocumentType.INVOICE).waitForCompletion();

        verify(mApiCommunicator)
                .uploadDocument(eq(document),
                        eq("application/vnd.gini.v2.partial+jpeg"), eq("foobar.jpg"),
                        eq("Invoice"),
                        eq(mSession), nullable(DocumentMetadata.class));
    }

    @Test
    public void testThatCreatePartialDocumentThrowsExceptionWhenUsingAccountingApiType() throws Exception {
        final DocumentTaskManager documentTaskManager =
                new DocumentTaskManager(mApiCommunicator, mSessionManager, GiniApiType.ACCOUNTING);

        final byte[] document = new byte[]{0x01, 0x02};

        UnsupportedOperationException exception = null;
        try {
            documentTaskManager.createPartialDocument(document, MediaTypes.IMAGE_JPEG, "foobar.jpg",
                    DocumentType.INVOICE).waitForCompletion();
        } catch (UnsupportedOperationException e) {
            exception = e;
        }

        assertNotNull(exception);
        assertEquals("Partial documents may be used only with the default Gini API. Use GiniApiType.DEFAULT.", exception.getMessage());
    }

    @Test
    public void testThatCreateCompositeDocumentSetsTheCorrectContentType() throws Exception {
        final Uri createdDocumentUri = Uri.parse("https://api.gini.net/documents/1234");
        when(mApiCommunicator.uploadDocument(any(byte[].class), any(String.class),
                any(String.class), any(String.class),
                any(Session.class), any(DocumentMetadata.class)))
                .thenReturn(Task.forResult(Uri.parse("https://api.gini.net/documents/1234")));
        when(mApiCommunicator.getDocument(eq(createdDocumentUri), any(Session.class))).thenReturn(
                createDocumentJSONTask("1234"));

        final List<Document> partialDocuments = new ArrayList<>();
        partialDocuments.add(createDocument("1111"));
        partialDocuments.add(createDocument("2222"));

        mDocumentTaskManager.createCompositeDocument(partialDocuments, DocumentType.INVOICE).waitForCompletion();

        verify(mApiCommunicator)
                .uploadDocument(any(byte[].class),
                        eq("application/vnd.gini.v2.composite+json"), eq((String) null),
                        eq("Invoice"),
                        eq(mSession), nullable(DocumentMetadata.class));
    }

    @Test
    public void testThatCreateCompositeDocumentUploadsCorrectJson() throws Exception {
        final Uri createdDocumentUri = Uri.parse("https://api.gini.net/documents/1234");
        when(mApiCommunicator.uploadDocument(any(byte[].class), any(String.class),
                any(String.class), any(String.class),
                any(Session.class), any(DocumentMetadata.class)))
                .thenReturn(Task.forResult(Uri.parse("https://api.gini.net/documents/1234")));
        when(mApiCommunicator.getDocument(eq(createdDocumentUri), any(Session.class))).thenReturn(
                createDocumentJSONTask("1234"));

        final List<Document> partialDocuments = new ArrayList<>();
        partialDocuments.add(createDocument("1111"));
        partialDocuments.add(createDocument("2222"));

        final String jsonString = "{ \"partialDocuments\": [ "
                + "{ \"document\": \"https://api.gini.net/documents/1111\", \"rotationDelta\": 0 }, "
                + "{ \"document\": \"https://api.gini.net/documents/2222\", \"rotationDelta\": 0 } "
                + "] }";
        final JSONObject jsonObject = new JSONObject(jsonString);
        final byte[] jsonBytes = jsonObject.toString().getBytes(CHARSET_UTF8);

        mDocumentTaskManager.createCompositeDocument(partialDocuments, DocumentType.INVOICE).waitForCompletion();

        verify(mApiCommunicator)
                .uploadDocument(eq(jsonBytes),
                        eq("application/vnd.gini.v2.composite+json"), eq((String) null),
                        eq("Invoice"),
                        eq(mSession), nullable(DocumentMetadata.class));
    }

    @Test
    public void testThatCreateCompositeDocumentUploadsJsonWithRotation() throws Exception {
        final Uri createdDocumentUri = Uri.parse("https://api.gini.net/documents/1234");
        when(mApiCommunicator.uploadDocument(any(byte[].class), any(String.class),
                any(String.class), any(String.class),
                any(Session.class), any(DocumentMetadata.class)))
                .thenReturn(Task.forResult(Uri.parse("https://api.gini.net/documents/1234")));
        when(mApiCommunicator.getDocument(eq(createdDocumentUri), any(Session.class))).thenReturn(
                createDocumentJSONTask("1234"));

        final LinkedHashMap<Document, Integer> partialDocuments = new LinkedHashMap<>();
        partialDocuments.put(createDocument("1111"), 90);
        partialDocuments.put(createDocument("2222"), 180);

        final String jsonString = "{ \"partialDocuments\": [ "
                + "{ \"document\": \"https://api.gini.net/documents/1111\", \"rotationDelta\": 90 }, "
                + "{ \"document\": \"https://api.gini.net/documents/2222\", \"rotationDelta\": 180 } "
                + "] }";
        final JSONObject jsonObject = new JSONObject(jsonString);
        final byte[] jsonBytes = jsonObject.toString().getBytes(CHARSET_UTF8);

        mDocumentTaskManager.createCompositeDocument(partialDocuments, DocumentType.INVOICE).waitForCompletion();

        verify(mApiCommunicator)
                .uploadDocument(eq(jsonBytes),
                        eq("application/vnd.gini.v2.composite+json"), eq((String) null),
                        eq("Invoice"),
                        eq(mSession), nullable(DocumentMetadata.class));
    }

    @Test
    public void testThatCreateCompositeDocumentUploadsJsonWithNormalizedRotation() throws Exception {
        final Uri createdDocumentUri = Uri.parse("https://api.gini.net/documents/1234");
        when(mApiCommunicator.uploadDocument(any(byte[].class), any(String.class),
                any(String.class), any(String.class),
                any(Session.class), any(DocumentMetadata.class)))
                .thenReturn(Task.forResult(Uri.parse("https://api.gini.net/documents/1234")));
        when(mApiCommunicator.getDocument(eq(createdDocumentUri), any(Session.class))).thenReturn(
                createDocumentJSONTask("1234"));

        final LinkedHashMap<Document, Integer> partialDocuments = new LinkedHashMap<>();
        partialDocuments.put(createDocument("1111"), -90);
        partialDocuments.put(createDocument("2222"), 450);

        final String jsonString = "{ \"partialDocuments\": [ "
                + "{ \"document\": \"https://api.gini.net/documents/1111\", \"rotationDelta\": 270 }, "
                + "{ \"document\": \"https://api.gini.net/documents/2222\", \"rotationDelta\": 90 } "
                + "] }";
        final JSONObject jsonObject = new JSONObject(jsonString);
        final byte[] jsonBytes = jsonObject.toString().getBytes(CHARSET_UTF8);

        mDocumentTaskManager.createCompositeDocument(partialDocuments, DocumentType.INVOICE).waitForCompletion();

        verify(mApiCommunicator)
                .uploadDocument(eq(jsonBytes),
                        eq("application/vnd.gini.v2.composite+json"), eq((String) null),
                        eq("Invoice"),
                        eq(mSession), nullable(DocumentMetadata.class));
    }

    @Test
    public void testThatCreateCompositeDocumentThrowsExceptionWhenUsingAccountingApiType() throws Exception {
        final DocumentTaskManager documentTaskManager =
                new DocumentTaskManager(mApiCommunicator, mSessionManager, GiniApiType.ACCOUNTING);

        final LinkedHashMap<Document, Integer> partialDocuments = new LinkedHashMap<>();
        partialDocuments.put(createDocument("1111"), -90);
        partialDocuments.put(createDocument("2222"), 450);

        UnsupportedOperationException exception = null;
        try {
            documentTaskManager.createCompositeDocument(partialDocuments, DocumentType.INVOICE).waitForCompletion();
        } catch (UnsupportedOperationException e) {
            exception = e;
        }

        assertNotNull(exception);
        assertEquals("Composite documents may be used only with the default Gini API. Use GiniApiType.DEFAULT.", exception.getMessage());
    }

    @Test
    public void testDeleteDocument() throws Exception {
        final Document document = createDocument();

        when(mApiCommunicator.getDocument(eq(document.getId()), any(Session.class)))
                .thenReturn(createDocumentJSONTask());
        when(mApiCommunicator.deleteDocument(eq(document.getId()), any(Session.class)))
                .thenReturn(Task.forResult(""));

        mDocumentTaskManager.deletePartialDocumentAndParents(document.getId()).waitForCompletion();

        // No parent uris to delete
        verify(mApiCommunicator, never())
                .deleteDocument(any(Uri.class), eq(mSession));
        verify(mApiCommunicator, times(1))
                .deleteDocument(eq(document.getId()), eq(mSession));
    }

    @Test
    public void testDeleteDocumentDeletesParentsFirst() throws Exception {
        final String documentId = "1234";
        when(mApiCommunicator.getDocument(eq(documentId), any(Session.class)))
                .thenReturn(createDocumentJSONTask(documentId));
        when(mApiCommunicator.deleteDocument(any(Uri.class), any(Session.class)))
                .thenReturn(Task.forResult(""));
        when(mApiCommunicator.deleteDocument(any(String.class), any(Session.class)))
                .thenReturn(Task.forResult(""));

        final Document document = createDocument(documentId);

        mDocumentTaskManager.deletePartialDocumentAndParents(documentId).waitForCompletion();

        final InOrder inOrder = Mockito.inOrder(mApiCommunicator);

        inOrder.verify(mApiCommunicator, times(1))
                .deleteDocument(eq(document.getCompositeDocuments().get(0)), eq(mSession));
        inOrder.verify(mApiCommunicator, times(1))
                .deleteDocument(eq(document.getCompositeDocuments().get(1)), eq(mSession));
        inOrder.verify(mApiCommunicator, times(1))
                .deleteDocument(eq(document.getId()), eq(mSession));
    }

    @Test
    public void testDeprecatedDocumentBuilderPassesThroughArguments() throws IOException {
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

    @Test
    public void testDocumentBuilderPassesThroughArguments() throws IOException {
        final DocumentTaskManager documentTaskManager = Mockito.mock(DocumentTaskManager.class);

        Bitmap bitmap = createBitmap();
        DocumentTaskManager.DocumentUploadBuilder documentUploadBuilder =
                new DocumentTaskManager.DocumentUploadBuilder()
                        .setDocumentBitmap(bitmap)
                        .setDocumentType(DocumentType.INVOICE)
                        .setFilename("foobar.jpg");
        documentUploadBuilder.upload(documentTaskManager);

        verify(documentTaskManager).createDocument(bitmap, "foobar.jpg", DocumentType.INVOICE);
    }

    @Test
    public void testDocumentBuilderPassesThroughByteArray() throws IOException {
        final DocumentTaskManager documentTaskManager = Mockito.mock(DocumentTaskManager.class);

        byte[] byteArray = createByteArray();
        DocumentTaskManager.DocumentUploadBuilder documentUploadBuilder =
                new DocumentTaskManager.DocumentUploadBuilder()
                        .setDocumentBytes(byteArray)
                        .setDocumentType(DocumentType.INVOICE)
                        .setFilename("foobar.jpg");
        documentUploadBuilder.upload(documentTaskManager);

        verify(documentTaskManager).createDocument(byteArray, "foobar.jpg", DocumentType.INVOICE);
    }

    @Test
    public void testDocumentBuilderPassesBitmapInsteadOfByteArray() throws IOException {
        final DocumentTaskManager documentTaskManager = Mockito.mock(DocumentTaskManager.class);

        byte[] byteArray = createByteArray();
        Bitmap bitmap = createBitmap();
        DocumentTaskManager.DocumentUploadBuilder documentUploadBuilder =
                new DocumentTaskManager.DocumentUploadBuilder()
                        .setDocumentBytes(byteArray)
                        .setDocumentBitmap(bitmap)
                        .setDocumentType(DocumentType.INVOICE)
                        .setFilename("foobar.jpg");
        documentUploadBuilder.upload(documentTaskManager);

        verify(documentTaskManager).createDocument(bitmap, "foobar.jpg", DocumentType.INVOICE);
    }

    @Test
    public void testDocumentBuilderHasDefaultValues() throws IOException {
        final DocumentTaskManager documentTaskManager = Mockito.mock(DocumentTaskManager.class);

        Bitmap bitmap = createBitmap();
        new DocumentTaskManager.DocumentUploadBuilder().setDocumentBitmap(bitmap).upload(documentTaskManager);

        verify(documentTaskManager).createDocument(bitmap, null, null, DocumentTaskManager.DEFAULT_COMPRESSION);
    }

    @Test
    public void testGetExtractionsReturnsTask() throws IOException, JSONException {
        when(mApiCommunicator.getExtractions(eq("1234"), any(Session.class))).thenReturn(createExtractionsJSONTask());
        Document document = new Document("1234", Document.ProcessingState.COMPLETED, "foobar", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        assertNotNull(mDocumentTaskManager.getExtractions(document));
    }

    @Test
    public void testGetExtractionsResolvesToHashMap() throws Exception {
        when(mApiCommunicator.getExtractions(eq("1234"), any(Session.class))).thenReturn(createExtractionsJSONTask());
        Document document = new Document("1234", Document.ProcessingState.COMPLETED, "foobar", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        Task<Map<String, SpecificExtraction>> extractionsTask = mDocumentTaskManager.getExtractions(document);
        extractionsTask.waitForCompletion();
        if (extractionsTask.isFaulted()) {
            throw extractionsTask.getError();
        }
        final Map<String, SpecificExtraction> extractions = extractionsTask.getResult();
        assertNotNull(extractions);
        final SpecificExtraction amountToPay = extractions.get("amountToPay");
        assertNotNull(amountToPay);
        assertEquals(2, amountToPay.getCandidate().size());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testGetDocumentThrowsWithNullArgument() {
        final String documentId = null;
        try {
            mDocumentTaskManager.getDocument(documentId);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        final Uri documentUri = null;
        try {
            mDocumentTaskManager.getDocument(documentUri);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testGetDocumentReturnsTask() throws IOException, JSONException {
        when(mApiCommunicator.getDocument(eq("1234"), any(Session.class))).thenReturn(createDocumentJSONTask("1234"));

        Task<Document> documentTask = mDocumentTaskManager.getDocument("1234");

        assertNotNull(documentTask);
    }

    @Test
    public void testGetDocumentResolvesToDocument() throws IOException, JSONException, InterruptedException {
        when(mApiCommunicator.getDocument(eq("1234"), any(Session.class))).thenReturn(createDocumentJSONTask("1234"));

        Task<Document> documentTask = mDocumentTaskManager.getDocument("1234");
        documentTask.waitForCompletion();
        Document document = documentTask.getResult();

        assertNotNull(document);
        assertEquals("1234", document.getId());
    }

    @Test
    public void testPollDocumentThrowsWithNullArgument() {
        try {
            mDocumentTaskManager.pollDocument(null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPollDocument() throws IOException, JSONException, InterruptedException {
        when(mApiCommunicator.getDocument(eq("1234"), any(Session.class))).thenReturn(
                createDocumentJSONTask("1234", "PENDING"), createDocumentJSONTask("1234", "COMPLETED"));
        Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        Task<Document> documentTask = mDocumentTaskManager.pollDocument(document);
        documentTask.waitForCompletion();

        Document polledDocument = documentTask.getResult();
        assertNotNull(polledDocument);
        assertEquals("1234", polledDocument.getId());
        assertEquals(Document.ProcessingState.COMPLETED, polledDocument.getState());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPollDocumentProcessingStateErrorCompletesTask()
            throws IOException, JSONException, InterruptedException {
        when(mApiCommunicator.getDocument(eq("1234"), any(Session.class))).thenReturn(
                createDocumentJSONTask("1234", "PENDING"), createDocumentJSONTask("1234", "ERROR"));
        Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        Task<Document> documentTask = mDocumentTaskManager.pollDocument(document);
        documentTask.waitForCompletion();

        Document polledDocument = documentTask.getResult();
        assertNotNull(polledDocument);
        assertEquals("1234", polledDocument.getId());
        assertEquals(Document.ProcessingState.ERROR, polledDocument.getState());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPollDocumentCancellation() throws IOException, JSONException, InterruptedException {
        when(mApiCommunicator.getDocument(eq("1234"), any(Session.class))).thenReturn(
                createDocumentJSONTask("1234", "PENDING"), createDocumentJSONTask("1234", "PENDING"));
        Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        Task<Document> documentTask = mDocumentTaskManager.pollDocument(document);
        mDocumentTaskManager.cancelDocumentPolling(document);
        documentTask.waitForCompletion();

        assertTrue(documentTask.isCancelled());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPollDocumentCancellationAffectsSpecifiedDocumentOnly() throws IOException, JSONException, InterruptedException {
        Document completedDocument = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());
        when(mApiCommunicator.getDocument(eq(completedDocument.getId()), any(Session.class))).thenReturn(
                createDocumentJSONTask(completedDocument.getId(), "PENDING"),
                createDocumentJSONTask(completedDocument.getId(), "COMPLETED"));

        Document cancelledDocument = new Document("5678", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());
        when(mApiCommunicator.getDocument(eq(cancelledDocument.getId()), any(Session.class))).thenReturn(
                createDocumentJSONTask(cancelledDocument.getId(), "PENDING"), createDocumentJSONTask(cancelledDocument.getId(), "PENDING"));


        Task<Document> completedDocumentTask = mDocumentTaskManager.pollDocument(completedDocument);
        Task<Document> cancelledDocumenTask = mDocumentTaskManager.pollDocument(cancelledDocument);
        mDocumentTaskManager.cancelDocumentPolling(cancelledDocument);
        completedDocumentTask.waitForCompletion();
        cancelledDocumenTask.waitForCompletion();

        assertTrue(cancelledDocumenTask.isCancelled());
        Document completedPolledDocument = completedDocumentTask.getResult();
        assertNotNull(completedPolledDocument);
        assertEquals("1234", completedPolledDocument.getId());
        assertEquals(Document.ProcessingState.COMPLETED, completedPolledDocument.getState());
    }

    @Test
    public void testSendFeedbackThrowsWithNullArguments() throws JSONException {
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        try {
            mDocumentTaskManager.sendFeedbackForExtractions(null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mDocumentTaskManager.sendFeedbackForExtractions(document, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mDocumentTaskManager.sendFeedbackForExtractions(null, new HashMap<String, SpecificExtraction>());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testSendFeedbackReturnsTask() throws JSONException {
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());
        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();

        assertNotNull(mDocumentTaskManager.sendFeedbackForExtractions(document, extractions));
    }

    @Test
    public void testSendFeedbackResolvesToDocumentInstance() throws JSONException, InterruptedException {
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());
        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();
        when(mApiCommunicator.sendFeedback(eq("1234"), any(JSONObject.class), any(Session.class))).thenReturn(
                Task.forResult(new JSONObject()));

        Task<Document> updateTask = mDocumentTaskManager.sendFeedbackForExtractions(document, extractions);
        updateTask.waitForCompletion();
        assertNotNull(updateTask.getResult());
    }

    @Test
    public void testSendFeedbackSavesExtractions() throws JSONException, InterruptedException {
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());
        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();
        extractions.put("amountToPay",
                new SpecificExtraction("amountToPay", "42:EUR", "amount", null, new ArrayList<Extraction>()));
        extractions.put("senderName",
                new SpecificExtraction("senderName", "blah", "senderName", null, new ArrayList<Extraction>()));

        extractions.get("amountToPay").setValue("23:EUR");
        mDocumentTaskManager.sendFeedbackForExtractions(document, extractions).waitForCompletion();

        ArgumentCaptor<JSONObject> dataCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(mApiCommunicator).sendFeedback(eq("1234"), dataCaptor.capture(), any(Session.class));
        final JSONObject updateData = dataCaptor.getValue();
        // Should update the amountToPay
        assertTrue(updateData.has("amountToPay"));
        final JSONObject amountToPay = updateData.getJSONObject("amountToPay");
        assertEquals("23:EUR", amountToPay.getString("value"));
        assertTrue(updateData.has("senderName"));
    }

    @Test
    public void testSendFeedbackMarksExtractionsAsNotDirty() throws JSONException, InterruptedException {
        when(mApiCommunicator.sendFeedback(eq("1234"), any(JSONObject.class), any(Session.class))).thenReturn(
                Task.forResult(new JSONObject()));
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());
        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();
        extractions.put("amountToPay",
                new SpecificExtraction("amountToPay", "42:EUR", "amount", null, new ArrayList<Extraction>()));
        extractions.put("senderName",
                new SpecificExtraction("senderName", "blah", "senderName", null, new ArrayList<Extraction>()));

        extractions.get("amountToPay").setValue("23:EUR");
        Task<Document> updateTask = mDocumentTaskManager.sendFeedbackForExtractions(document, extractions);

        updateTask.waitForCompletion();
        assertFalse(extractions.get("amountToPay").isDirty());
    }

    @Test
    public void testReportDocumentThrowsWithNullArgument() {
        try {
            mDocumentTaskManager.reportDocument(null, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testReportDocumentReturnsTask() throws JSONException {
        when(mApiCommunicator.errorReportForDocument(eq("1234"), eq("short summary"), eq("detailed description"),
                any(Session.class)))
                .thenReturn(createErrorReportJSONTask("4444-3333"));
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        assertNotNull(mDocumentTaskManager.reportDocument(document, "short summary", "detailed description"));
    }

    @Test
    public void testReportDocumentTaskResolvesToErrorId() throws JSONException, InterruptedException {
        when(mApiCommunicator.errorReportForDocument(eq("1234"), eq("short summary"), eq("detailed description"),
                any(Session.class)))
                .thenReturn(createErrorReportJSONTask("4444-3333"));
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        Task<String> reportTask = mDocumentTaskManager.reportDocument(document, "short summary", "detailed description");
        reportTask.waitForCompletion();

        assertEquals("4444-3333", reportTask.getResult());
    }

    @Test
    public void testGetLayoutThrowsWithNullArgument() {
        try {
            mDocumentTaskManager.getLayout(null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testGetLayoutReturnsTask() {
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        assertNotNull(mDocumentTaskManager.getLayout(document));
    }

    @Test
    public void testGetLayoutResolvesToJSON() throws IOException, JSONException, InterruptedException {
        when(mApiCommunicator.getLayoutForDocument(eq("1234"), any(Session.class))).thenReturn(createLayoutJSONTask());
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        final Task<JSONObject> layoutTask = mDocumentTaskManager.getLayout(document);
        layoutTask.waitForCompletion();
        final JSONObject responseData = layoutTask.getResult();


        assertNotNull(responseData);
    }

    @Test
    public void testGetExtractionsParsesCompoundExtractions() throws Exception {
        when(mApiCommunicator.getExtractions(eq("1234"), any(Session.class))).thenReturn(createExtractionsJSONTask());
        Document document = new Document("1234", Document.ProcessingState.COMPLETED, "foobar", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        Task<ExtractionsContainer> extractionsTask = mDocumentTaskManager.getAllExtractions(document);
        extractionsTask.waitForCompletion();
        if (extractionsTask.isFaulted()) {
            throw extractionsTask.getError();
        }
        final ExtractionsContainer extractions = extractionsTask.getResult();
        assertNotNull(extractions);

        final CompoundExtraction lineItems = extractions.getCompoundExtractions().get("lineItems");
        assertNotNull(lineItems);

        final List<Map<String, SpecificExtraction>> lineItemExtractions = lineItems.getSpecificExtractionMaps();
        assertEquals(3, lineItemExtractions.size());

        final Map<String, SpecificExtraction> lineItemExtractionColumns = lineItemExtractions.get(0);
        assertEquals(4, lineItemExtractionColumns.size());

        final List<String> columnNames = Arrays.asList("artNumber", "description", "grossPrice", "quantity");
        for (final String columnName : columnNames) {
            assertNotNull(lineItemExtractionColumns.get(columnName));
        }

        SpecificExtraction artNumberColumn = lineItemExtractionColumns.get("artNumber");

        assertEquals("H0422S039-M11000L000", artNumberColumn.getValue());
        assertEquals("idnumber", artNumberColumn.getEntity());
    }

    @Test
    public void testGetAllExtractionsResolvesToExtractionResult() throws Exception {
        when(mApiCommunicator.getExtractions(eq("1234"), any(Session.class))).thenReturn(createExtractionsJSONTask());
        Document document = new Document("1234", Document.ProcessingState.COMPLETED, "foobar", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        Task<ExtractionsContainer> extractionsTask = mDocumentTaskManager.getAllExtractions(document);
        extractionsTask.waitForCompletion();
        if (extractionsTask.isFaulted()) {
            throw extractionsTask.getError();
        }
        final ExtractionsContainer extractions = extractionsTask.getResult();
        assertNotNull(extractions);
        final SpecificExtraction amountToPay = extractions.getSpecificExtractions().get("amountToPay");
        assertNotNull(amountToPay);
        assertEquals(2, amountToPay.getCandidate().size());
    }

    @Test
    public void testGetExtractionsParsesReturnReasons() throws Exception {
        when(mApiCommunicator.getExtractions(eq("1234"), any(Session.class))).thenReturn(createExtractionsJSONTask());
        Document document = new Document("1234", Document.ProcessingState.COMPLETED, "foobar", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        Task<ExtractionsContainer> extractionsTask = mDocumentTaskManager.getAllExtractions(document);
        extractionsTask.waitForCompletion();
        if (extractionsTask.isFaulted()) {
            throw extractionsTask.getError();
        }
        final ExtractionsContainer extractions = extractionsTask.getResult();
        assertNotNull(extractions);

        assertEquals(4, extractions.getReturnReasons().size());

        final ReturnReason returnReason = extractions.getReturnReasons().get(0);

        assertEquals("r1", returnReason.getId());
        assertEquals("Anderes Aussehen als angeboten", returnReason.getLocalizedLabels().get("de"));
    }
}
