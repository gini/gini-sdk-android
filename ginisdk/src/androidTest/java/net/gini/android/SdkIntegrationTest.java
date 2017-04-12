package net.gini.android;


import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.test.AndroidTestCase;
import android.util.Log;

import com.android.volley.toolbox.NoCache;

import net.gini.android.DocumentTaskManager.DocumentUploadBuilder;
import net.gini.android.authorization.SharedPreferencesCredentialsStore;
import net.gini.android.authorization.UserCredentials;
import net.gini.android.helpers.TestUtils;
import net.gini.android.models.Document;
import net.gini.android.models.SpecificExtraction;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import bolts.Continuation;
import bolts.Task;

public class SdkIntegrationTest extends AndroidTestCase {

    private Gini gini;
    private String clientId;
    private String clientSecret;
    private String apiUri;
    private String userCenterUri;

    @Override
    protected void setUp() throws Exception {
        final AssetManager assetManager = getContext().getResources().getAssets();
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
        Log.d("TEST", "testUserCenterUri" + userCenterUri);

        gini = new SdkBuilder(getContext(), clientId, clientSecret, "example.com").
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                build();
    }

    public static String getProperty(Properties properties, String propertyName) {
        Object value = properties.get(propertyName);
        assertNotNull(propertyName + " not set!", value);
        return value.toString();
    }

    public void testDeprecatedProcessDocumentBitmap() throws IOException, InterruptedException, JSONException {
        final AssetManager assetManager = getContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final Bitmap testDocument = BitmapFactory.decodeStream(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder(testDocument).setDocumentType("RemittanceSlip");
        uploadDocument(uploadBuilder);
    }

    public void testProcessDocumentBitmap() throws IOException, InterruptedException, JSONException {
        final AssetManager assetManager = getContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final Bitmap testDocument = BitmapFactory.decodeStream(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder().setDocumentBitmap(testDocument).setDocumentType(DocumentTaskManager.DocumentType.INVOICE);
        uploadDocument(uploadBuilder);
    }

    public void testProcessDocumentByteArray() throws IOException, InterruptedException, JSONException {
        final AssetManager assetManager = getContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder().setDocumentBytes(testDocument).setDocumentType(DocumentTaskManager.DocumentType.INVOICE);
        uploadDocument(uploadBuilder);
    }

    public void testProcessDocumentWithCustomCache() throws IOException, JSONException, InterruptedException {
        gini = new SdkBuilder(getContext(), clientId, clientSecret, "example.com").
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setCache(new NoCache()).
                build();

        final AssetManager assetManager = getContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder().setDocumentBytes(testDocument).setDocumentType(DocumentTaskManager.DocumentType.INVOICE);
        uploadDocument(uploadBuilder);
    }

    public void testDocumentUploadWorksAfterNewUserWasCreatedIfUserWasInvalid() throws IOException, JSONException, InterruptedException {
        SharedPreferencesCredentialsStore credentialsStore = new SharedPreferencesCredentialsStore(getContext().getSharedPreferences("GiniTests", Context.MODE_PRIVATE));
        gini = new SdkBuilder(getContext(), clientId, clientSecret, "example.com").
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setCredentialsStore(credentialsStore).
                build();

        // Create invalid user credentials
        UserCredentials invalidUserCredentials = new UserCredentials("invalid@example.com", "1234");
        credentialsStore.storeUserCredentials(invalidUserCredentials);

        final AssetManager assetManager = getContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final Bitmap testDocument = BitmapFactory.decodeStream(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder().setDocumentBitmap(testDocument).setDocumentType(DocumentTaskManager.DocumentType.INVOICE);
        uploadDocument(uploadBuilder);

        // Verify that a new user was created
        assertNotSame(invalidUserCredentials.getUsername(), credentialsStore.getUserCredentials().getUsername());
    }

    public void testEmailDomainIsUpdatedForExistingUserIfEmailDomainWasChanged() throws IOException, JSONException, InterruptedException {
        // Upload a document to make sure we have a valid user
        SharedPreferencesCredentialsStore credentialsStore = new SharedPreferencesCredentialsStore(getContext().getSharedPreferences("GiniTests", Context.MODE_PRIVATE));
        gini = new SdkBuilder(getContext(), clientId, clientSecret, "example.com").
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setCredentialsStore(credentialsStore).
                build();

        final AssetManager assetManager = getContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final Bitmap testDocument = BitmapFactory.decodeStream(testDocumentAsStream);
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder().setDocumentBitmap(testDocument).setDocumentType(DocumentTaskManager.DocumentType.INVOICE);
        uploadDocument(uploadBuilder);

        // Create another sdk instance with a new email domain (to simulate an app update)
        // and verify that the new email domain is used
        String newEmailDomain = "beispiel.com";
        gini = new SdkBuilder(getContext(), clientId, clientSecret, newEmailDomain).
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setCredentialsStore(credentialsStore).
                build();

        uploadDocument(uploadBuilder);

        UserCredentials newUserCredentials = credentialsStore.getUserCredentials();
        assertEquals(newEmailDomain, extractEmailDomain(newUserCredentials.getUsername()));
    }

    private String extractEmailDomain(String email) {
        String[] components = email.split("@");
        if (components.length > 1) {
            return components[1];
        }
        return "";
    }

    private void uploadDocument(DocumentUploadBuilder uploadBuilder) throws InterruptedException, JSONException {
        final DocumentTaskManager documentTaskManager = gini.getDocumentTaskManager();

        final Task<Document> upload = uploadBuilder.upload(documentTaskManager);
        final Task<Document> processDocument = upload.onSuccessTask(new Continuation<Document, Task<Document>>() {
            @Override
            public Task<Document> then(Task<Document> task) throws Exception {
                Document document = task.getResult();
                return documentTaskManager.pollDocument(document);
            }
        });

        final Task<Map<String, SpecificExtraction>> retrieveExtractions = processDocument.onSuccessTask(new Continuation<Document, Task<Map<String, SpecificExtraction>>>() {
            @Override
            public Task<Map<String, SpecificExtraction>> then(Task<Document> task) throws Exception {
                return documentTaskManager.getExtractions(task.getResult());
            }
        });

        retrieveExtractions.waitForCompletion();
        if (retrieveExtractions.isFaulted()) {
            Log.e("TEST", Log.getStackTraceString(retrieveExtractions.getError()));
        }

        assertFalse("extractions should have succeeded", retrieveExtractions.isFaulted());

        final Map<String, SpecificExtraction> extractions = retrieveExtractions.getResult();

        assertEquals("IBAN should be found", "DE78370501980020008850", extractions.get("iban").getValue());
        assertEquals("Amount to pay should be found", "1.00:EUR", extractions.get("amountToPay").getValue());
        assertEquals("BIC should be found", "COLSDE33", extractions.get("bic").getValue());
        assertEquals("Payee should be found", "Uno Flüchtlingshilfe", extractions.get("senderName").getValue());

        // all extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        Map<String, SpecificExtraction> feedback = new HashMap<String, SpecificExtraction>();
        feedback.put("iban", extractions.get("iban"));
        feedback.put("amountToPay", extractions.get("amountToPay"));
        feedback.put("bic", extractions.get("bic"));
        feedback.put("senderName", extractions.get("senderName"));

        final Task<Document> sendFeedback = documentTaskManager.sendFeedbackForExtractions(upload.getResult(), feedback);
        sendFeedback.waitForCompletion();
        if (sendFeedback.isFaulted()) {
            Log.e("TEST", Log.getStackTraceString(sendFeedback.getError()));
        }
        assertTrue("Sending feedback should be completed", sendFeedback.isCompleted());
        assertFalse("Sending feedback should be successful", sendFeedback.isFaulted());
    }
}
