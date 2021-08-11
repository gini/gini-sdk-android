package net.gini.android;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static net.gini.android.helpers.TrustKitHelper.resetTrustKit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.util.Log;
import android.util.Pair;

import com.android.volley.toolbox.NoCache;

import net.gini.android.DocumentTaskManager.DocumentUploadBuilder;
import net.gini.android.authorization.EncryptedCredentialsStore;
import net.gini.android.authorization.UserCredentials;
import net.gini.android.helpers.TestUtils;
import net.gini.android.models.Document;
import net.gini.android.models.SpecificExtraction;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import bolts.Continuation;
import bolts.Task;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SdkIntegrationTest {

    private Gini gini;
    private String clientId;
    private String clientSecret;
    private String apiUri;
    private String userCenterUri;

    @Before
    public void setUp() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testPropertiesInput = assetManager.open("test.properties");
        assertNotNull("test.properties not found", testPropertiesInput);
        final Properties testProperties = new Properties();
        testProperties.load(testPropertiesInput);
        clientId = getProperty(testProperties, "testClientId");
        clientSecret = getProperty(testProperties, "testClientSecret");
        apiUri = getProperty(testProperties, "testApiUri");
        userCenterUri = getProperty(testProperties, "testUserCenterUri");

        Log.d("TEST", "testClientId " + clientId);
        Log.d("TEST", "testClientSecret " + clientSecret);
        Log.d("TEST", "testApiUri " + apiUri);
        Log.d("TEST", "testUserCenterUri " + userCenterUri);

        resetTrustKit();

        gini = new SdkBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                build();
    }

    private static String getProperty(Properties properties, String propertyName) {
        Object value = properties.get(propertyName);
        assertNotNull(propertyName + " not set!", value);
        return value.toString();
    }

    @Test
    public void deprecatedProcessDocumentBitmap() throws IOException, InterruptedException, JSONException {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final Bitmap testDocument = BitmapFactory.decodeStream(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder(testDocument).setDocumentType("RemittanceSlip");
        processDocument(uploadBuilder);
    }

    @Test
    public void processDocumentBitmap() throws IOException, InterruptedException, JSONException {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final Bitmap testDocument = BitmapFactory.decodeStream(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder().setDocumentBitmap(testDocument).setDocumentType(
                DocumentTaskManager.DocumentType.INVOICE);
        processDocument(uploadBuilder);
    }

    @Test
    public void processDocumentByteArray() throws IOException, InterruptedException, JSONException {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder().setDocumentBytes(testDocument).setDocumentType(
                DocumentTaskManager.DocumentType.INVOICE);
        processDocument(uploadBuilder);
    }

    @Test
    public void processDocumentWithCustomCache() throws IOException, JSONException, InterruptedException {
        gini = new SdkBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setCache(new NoCache()).
                build();

        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder().setDocumentBytes(testDocument).setDocumentType(
                DocumentTaskManager.DocumentType.INVOICE);
        processDocument(uploadBuilder);
    }

    @Test
    public void sendFeedback() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder().setDocumentBytes(testDocument).setDocumentType(
                DocumentTaskManager.DocumentType.INVOICE);
        final Map<Document, Map<String, SpecificExtraction>> documentExtractions = processDocument(
                uploadBuilder);
        final Document document = documentExtractions.keySet().iterator().next();
        final Map<String, SpecificExtraction> extractions = documentExtractions.values().iterator().next();

        // All extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        final Map<String, SpecificExtraction> feedback = new HashMap<>();
        feedback.put("iban", extractions.get("iban"));
        feedback.put("amountToPay", extractions.get("amountToPay"));
        feedback.put("bic", extractions.get("bic"));
        feedback.put("senderName", extractions.get("senderName"));

        final Task<Document> sendFeedback = gini.getDocumentTaskManager().sendFeedbackForExtractions(document, feedback);
        sendFeedback.waitForCompletion();
        if (sendFeedback.isFaulted()) {
            Log.e("TEST", Log.getStackTraceString(sendFeedback.getError()));
        }
        assertTrue("Sending feedback should be completed", sendFeedback.isCompleted());
        assertFalse("Sending feedback should be successful", sendFeedback.isFaulted());
    }

    @Test
    public void documentUploadWorksAfterNewUserWasCreatedIfUserWasInvalid() throws IOException, JSONException, InterruptedException {
        EncryptedCredentialsStore credentialsStore = new EncryptedCredentialsStore(
                getApplicationContext().getSharedPreferences("GiniTests", Context.MODE_PRIVATE), getApplicationContext());
        gini = new SdkBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setCredentialsStore(credentialsStore).
                build();

        // Create invalid user credentials
        UserCredentials invalidUserCredentials = new UserCredentials("invalid@example.com", "1234");
        credentialsStore.storeUserCredentials(invalidUserCredentials);

        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final Bitmap testDocument = BitmapFactory.decodeStream(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder().setDocumentBitmap(testDocument).setDocumentType(
                DocumentTaskManager.DocumentType.INVOICE);
        processDocument(uploadBuilder);

        // Verify that a new user was created
        assertNotSame(invalidUserCredentials.getUsername(), credentialsStore.getUserCredentials().getUsername());
    }

    @Test
    public void emailDomainIsUpdatedForExistingUserIfEmailDomainWasChanged() throws IOException, JSONException, InterruptedException {
        // Upload a document to make sure we have a valid user
        EncryptedCredentialsStore credentialsStore = new EncryptedCredentialsStore(
                getApplicationContext().getSharedPreferences("GiniTests", Context.MODE_PRIVATE), getApplicationContext());
        gini = new SdkBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setCredentialsStore(credentialsStore).
                build();

        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final Bitmap testDocument = BitmapFactory.decodeStream(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder().setDocumentBitmap(testDocument).setDocumentType(
                DocumentTaskManager.DocumentType.INVOICE);
        processDocument(uploadBuilder);

        // Create another sdk instance with a new email domain (to simulate an app update)
        // and verify that the new email domain is used
        String newEmailDomain = "beispiel.com";
        gini = new SdkBuilder(getApplicationContext(), clientId, clientSecret, newEmailDomain).
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setCredentialsStore(credentialsStore).
                build();

        processDocument(uploadBuilder);

        UserCredentials newUserCredentials = credentialsStore.getUserCredentials();
        assertEquals(newEmailDomain, extractEmailDomain(newUserCredentials.getUsername()));
    }

    @Test
    public void publicKeyPinningWithMatchingPublicKey() throws Exception {
        resetTrustKit();
        gini = new SdkBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setNetworkSecurityConfigResId(net.gini.android.test.R.xml.network_security_config).
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                build();
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder().setDocumentBytes(testDocument).setDocumentType(
                DocumentTaskManager.DocumentType.INVOICE);
        processDocument(uploadBuilder);
    }

    @Test
    public void publicKeyPinningWithCustomCache() throws Exception {
        resetTrustKit();
        gini = new SdkBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setNetworkSecurityConfigResId(net.gini.android.test.R.xml.network_security_config).
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setCache(new NoCache()).
                build();

        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder().setDocumentBytes(testDocument).setDocumentType(
                DocumentTaskManager.DocumentType.INVOICE);
        processDocument(uploadBuilder);
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void publicKeyPinningWithWrongPublicKey() throws Exception {
        resetTrustKit();
        gini = new SdkBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setNetworkSecurityConfigResId(net.gini.android.test.R.xml.wrong_network_security_config).
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                build();

        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder().setDocumentBytes(testDocument).setDocumentType(
                DocumentTaskManager.DocumentType.INVOICE);
        final DocumentTaskManager documentTaskManager = gini.getDocumentTaskManager();

        final Task<Document> upload = uploadBuilder.upload(documentTaskManager);
        final Task<Document> processDocument = upload.onSuccessTask(new Continuation<Document, Task<Document>>() {
            @Override
            public Task<Document> then(Task<Document> task) throws Exception {
                Document document = task.getResult();
                return documentTaskManager.pollDocument(document);
            }
        });

        final Task<Map<String, SpecificExtraction>> retrieveExtractions = processDocument.onSuccessTask(
                new Continuation<Document, Task<Map<String, SpecificExtraction>>>() {
                    @Override
                    public Task<Map<String, SpecificExtraction>> then(Task<Document> task) throws Exception {
                        return documentTaskManager.getExtractions(task.getResult());
                    }
                });

        retrieveExtractions.waitForCompletion();
        if (retrieveExtractions.isFaulted()) {
            Log.e("TEST", Log.getStackTraceString(retrieveExtractions.getError()));
        }

        assertTrue("extractions shouldn't have succeeded", retrieveExtractions.isFaulted());
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void publicKeyPinningWithMultiplePublicKeys() throws Exception {
        resetTrustKit();
        gini = new SdkBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setNetworkSecurityConfigResId(net.gini.android.test.R.xml.multiple_keys_network_security_config).
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                build();

        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder().setDocumentBytes(testDocument).setDocumentType(
                DocumentTaskManager.DocumentType.INVOICE);
        processDocument(uploadBuilder);
    }

    @Test
    public void createPartialDocument() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("multi-page-p1.png");
        assertNotNull("test image multi-page-p1.png could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);

        final Task<Document> task = gini.getDocumentTaskManager()
                .createPartialDocument(testDocument, "image/png", null, null);
        task.waitForCompletion();

        final Document partialDocument = task.getResult();
        assertNotNull(partialDocument);
    }

    @Test
    public void deletePartialDocumentWithoutParents() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("multi-page-p1.png");
        assertNotNull("test image multi-page-p1.png could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);

        final Task<String> task = gini.getDocumentTaskManager()
                .createPartialDocument(testDocument, "image/png", null, null)
                .onSuccessTask(new Continuation<Document, Task<String>>() {
                    @Override
                    public Task<String> then(final Task<Document> task) throws Exception {
                        return gini.getDocumentTaskManager().deleteDocument(task.getResult().getId());
                    }
                });
        task.waitForCompletion();

        assertNotNull(task.getResult());
    }

    @Test
    public void deletePartialDocumentWithParents() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream page1Stream = assetManager.open("multi-page-p1.png");
        assertNotNull("test image multi-page-p1.png could not be loaded", page1Stream);

        final byte[] page1 = TestUtils.createByteArray(page1Stream);

        final AtomicReference<Document> partialDocument = new AtomicReference<>();
        final Task<String> task = gini.getDocumentTaskManager()
                .createPartialDocument(page1, "image/png", null, null)
                .onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        final Document document = task.getResult();
                        partialDocument.set(document);
                        final LinkedHashMap<Document, Integer> documentRotationDeltaMap = new LinkedHashMap<>();
                        documentRotationDeltaMap.put(document, 0);
                        return gini.getDocumentTaskManager().createCompositeDocument(documentRotationDeltaMap, null);
                    }
                }).onSuccessTask(new Continuation<Document, Task<String>>() {
                    @Override
                    public Task<String> then(final Task<Document> task) throws Exception {
                        return gini.getDocumentTaskManager().deletePartialDocumentAndParents(partialDocument.get().getId());
                    }
                });
        task.waitForCompletion();

        assertNotNull(task.getResult());
    }

    @Test
    public void deletePartialDocumentFailsWhenNotDeletingParents() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream page1Stream = assetManager.open("multi-page-p1.png");
        assertNotNull("test image multi-page-p1.png could not be loaded", page1Stream);

        final byte[] page1 = TestUtils.createByteArray(page1Stream);

        final AtomicReference<Document> partialDocument = new AtomicReference<>();
        final Task<String> task = gini.getDocumentTaskManager()
                .createPartialDocument(page1, "image/png", null, null)
                .onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        final Document document = task.getResult();
                        partialDocument.set(document);
                        final LinkedHashMap<Document, Integer> documentRotationDeltaMap = new LinkedHashMap<>();
                        documentRotationDeltaMap.put(document, 0);
                        return gini.getDocumentTaskManager().createCompositeDocument(documentRotationDeltaMap, null);
                    }
                }).onSuccessTask(new Continuation<Document, Task<String>>() {
                    @Override
                    public Task<String> then(final Task<Document> task) throws Exception {
                        return gini.getDocumentTaskManager().deleteDocument(partialDocument.get().getId());
                    }
                });
        task.waitForCompletion();

        assertTrue(task.isFaulted());
    }

    @Test
    public void processCompositeDocument() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream page1Stream = assetManager.open("multi-page-p1.png");
        assertNotNull("test image multi-page-p1.png could not be loaded", page1Stream);
        final InputStream page2Stream = assetManager.open("multi-page-p2.png");
        assertNotNull("test image multi-page-p2.png could not be loaded", page2Stream);
        final InputStream page3Stream = assetManager.open("multi-page-p3.png");
        assertNotNull("test image multi-page-p3.png could not be loaded", page3Stream);

        final byte[] page1 = TestUtils.createByteArray(page1Stream);
        final byte[] page2 = TestUtils.createByteArray(page2Stream);
        final byte[] page3 = TestUtils.createByteArray(page3Stream);

        final List<Document> partialDocuments = new ArrayList<>();
        final AtomicReference<Document> compositeDocument = new AtomicReference<>();
        final DocumentTaskManager documentTaskManager = gini.getDocumentTaskManager();
        final Task<Map<String, SpecificExtraction>> task = documentTaskManager
                .createPartialDocument(page1, "image/png", null, null)
                .onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        partialDocuments.add(task.getResult());
                        return documentTaskManager.createPartialDocument(page2, "image/png", null, null);
                    }
                })
                .onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        partialDocuments.add(task.getResult());
                        return documentTaskManager.createPartialDocument(page3, "image/png", null, null);
                    }
                })
                .onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        partialDocuments.add(task.getResult());
                        final LinkedHashMap<Document, Integer> documentRotationDeltaMap = new LinkedHashMap<>();
                        for (final Document partialDocument : partialDocuments) {
                            documentRotationDeltaMap.put(partialDocument, 0);
                        }
                        return documentTaskManager.createCompositeDocument(documentRotationDeltaMap, null);
                    }
                }).onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        compositeDocument.set(task.getResult());
                        return documentTaskManager.pollDocument(task.getResult());
                    }
                }).onSuccessTask(new Continuation<Document, Task<Map<String, SpecificExtraction>>>() {
                    @Override
                    public Task<Map<String, SpecificExtraction>> then(final Task<Document> task) throws Exception {
                        return documentTaskManager.getExtractions(task.getResult());
                    }
                });
        task.waitForCompletion();

        assertEquals(3, partialDocuments.size());
        final Map<String, SpecificExtraction> extractions = task.getResult();
        assertNotNull(extractions);

        assertEquals("IBAN should be found", "DE96490501010082009697", extractions.get("iban").getValue());
        final String amountToPay = extractions.get("amountToPay").getValue();
        assertTrue("Amount to pay should be found: "
                        + "expected one of <[145.00:EUR, 77.00:EUR, 588.60:EUR, 700.43:EUR]> but was:<["
                        + amountToPay + "]>",
                amountToPay.equals("145.00:EUR")
                        || amountToPay.equals("77.00:EUR")
                        || amountToPay.equals("588.60:EUR")
                        || amountToPay.equals("700.43:EUR")
                        || amountToPay.equals("26.42:EUR"));
        assertEquals("BIC should be found", "WELADED1MIN", extractions.get("bic").getValue());
        assertTrue("Payement recipient should be found", extractions.get("paymentRecipient").getValue().startsWith("Mindener Stadtwerke"));
        assertTrue("Payment reference should be found", extractions.get("paymentReference").getValue().contains(
                "ReNr TST-00019, KdNr 765432"));

        // all extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        Map<String, SpecificExtraction> feedback = new HashMap<String, SpecificExtraction>();
        feedback.put("iban", extractions.get("iban"));
        feedback.put("amountToPay", extractions.get("amountToPay"));
        feedback.put("bic", extractions.get("bic"));
        feedback.put("paymentRecipient", extractions.get("senderName"));
        feedback.put("paymentReference", extractions.get("paymentReference"));

        final Task<Document> sendFeedback = documentTaskManager.sendFeedbackForExtractions(compositeDocument.get(), feedback);
        sendFeedback.waitForCompletion();
        if (sendFeedback.isFaulted()) {
            Log.e("TEST", Log.getStackTraceString(sendFeedback.getError()));
        }
        assertTrue("Sending feedback should be completed", sendFeedback.isCompleted());
        assertFalse("Sending feedback should be successful", sendFeedback.isFaulted());
    }

    @Test
    public void testDeleteCompositeDocument() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream page1Stream = assetManager.open("multi-page-p1.png");
        assertNotNull("test image multi-page-p1.png could not be loaded", page1Stream);

        final byte[] page1 = TestUtils.createByteArray(page1Stream);

        final AtomicReference<Document> partialDocument = new AtomicReference<>();
        final Task<String> task = gini.getDocumentTaskManager()
                .createPartialDocument(page1, "image/png", null, null)
                .onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        final Document document = task.getResult();
                        partialDocument.set(document);
                        final LinkedHashMap<Document, Integer> documentRotationDeltaMap = new LinkedHashMap<>();
                        documentRotationDeltaMap.put(document, 0);
                        return gini.getDocumentTaskManager().createCompositeDocument(documentRotationDeltaMap, null);
                    }
                }).onSuccessTask(new Continuation<Document, Task<String>>() {
                    @Override
                    public Task<String> then(final Task<Document> task) throws Exception {
                        return gini.getDocumentTaskManager().deleteDocument(task.getResult().getId());
                    }
                });
        task.waitForCompletion();

        assertNotNull(task.getResult());
    }

    private String extractEmailDomain(String email) {
        String[] components = email.split("@");
        if (components.length > 1) {
            return components[1];
        }
        return "";
    }

    private Map<Document, Map<String, SpecificExtraction>> processDocument(DocumentUploadBuilder uploadBuilder)
            throws InterruptedException {
        final Pair<Document, Map<String, SpecificExtraction>> retrieveExtractions =
                analyzeDocument(uploadBuilder, 10, 0);

        final Map<String, SpecificExtraction> extractions = retrieveExtractions.second;

        assertEquals("IBAN should be found", "DE78370501980020008850", extractions.get("iban").getValue());
        assertEquals("Amount to pay should be found", "1.00:EUR", extractions.get("amountToPay").getValue());
        assertEquals("BIC should be found", "COLSDE33", extractions.get("bic").getValue());
        assertEquals("Payee should be found", "Uno Flüchtlingshilfe", extractions.get("senderName").getValue());

        return Collections.singletonMap(retrieveExtractions.first, extractions);
    }

    private Pair<Document, Map<String, SpecificExtraction>> analyzeDocument(DocumentUploadBuilder uploadBuilder, int maxRetries, int retries)
            throws InterruptedException {
        final DocumentTaskManager documentTaskManager = gini.getDocumentTaskManager();

        final Task<Document> upload = uploadBuilder.upload(documentTaskManager);
        final Task<Document> processDocument = upload.onSuccessTask(task -> {
            Document document = task.getResult();
            return documentTaskManager.pollDocument(document);
        });

        final Task<Map<String, SpecificExtraction>> retrieveExtractions = processDocument
                .onSuccessTask(task -> documentTaskManager.getExtractions(task.getResult()));

        retrieveExtractions.waitForCompletion();

        if (retrieveExtractions.isFaulted() && retries < maxRetries) {
            Log.e("TEST", Log.getStackTraceString(retrieveExtractions.getError()));
            Thread.sleep(1000);
            return analyzeDocument(uploadBuilder, maxRetries, retries + 1);
        }

        assertFalse("extractions should have succeeded", retrieveExtractions.isFaulted());

        return new Pair<>(upload.getResult(), retrieveExtractions.getResult());
    }
}
