package net.gini.android;


import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.test.AndroidTestCase;

import net.gini.android.DocumentTaskManager.DocumentUploadBuilder;
import net.gini.android.models.Document;
import net.gini.android.models.SpecificExtraction;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import bolts.Continuation;
import bolts.Task;

public class SdkIntegrationTest extends AndroidTestCase{

    private Gini gini;

    @Override
    protected void setUp() throws Exception {
        final AssetManager assetManager = getContext().getResources().getAssets();
        final InputStream testPropertiesInput = assetManager.open("test.properties");
        assertNotNull("test.properties not found", testPropertiesInput);
        final Properties testProperties = new Properties();
        testProperties.load(testPropertiesInput);
        final String clientId = getProperty(testProperties, "testClientId");
        final String clientSecret = getProperty(testProperties, "testClientSecret");
        final String apiUrl = getProperty(testProperties, "testApiUri");
        final String userCenterUrl = getProperty(testProperties, "testUserCenterUri");

        gini = new SdkBuilder(getContext(), clientId, clientSecret, "example.com").
                setApiBaseUrl(apiUrl).
                setUserCenterApiBaseUrl(userCenterUrl).
                build();
    }

    public static String getProperty(Properties properties, String propertyName){
        Object value = properties.get(propertyName);
        assertNotNull(propertyName +  " not set!", value);
        return value.toString();
    }

    public void testProcessDocument() throws IOException, InterruptedException, JSONException {
        final AssetManager assetManager = getContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull(testDocumentAsStream);

        final Bitmap testDocument = BitmapFactory.decodeStream(testDocumentAsStream);
        final DocumentTaskManager documentTaskManager = gini.getDocumentTaskManager();
        final DocumentUploadBuilder uploadBuilder = new DocumentUploadBuilder(testDocument).setDocumentType("RemittanceSlip");

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
        assertTrue("extractions should be returned", retrieveExtractions.isCompleted());
        final Map<String, SpecificExtraction> extractions = retrieveExtractions.getResult();
        assertEquals("IBAN should be found", "DE92760700120750007700", extractions.get("iban").getValue());
        assertEquals("Amount to pay should be found", "29.00:EUR", extractions.get("amountToPay").getValue());
        assertEquals("BIC should be found", "DEUTDEMM760", extractions.get("bic").getValue());
        assertEquals("Payee should be found", "Hetzner Online AG", extractions.get("senderName").getValue());

        // all extractions are correct, that means we have nothing to correct and will only send positive feedback
        final Task<Document> sendFeedback = documentTaskManager.sendFeedbackForExtractions(upload.getResult(), retrieveExtractions.getResult());
        sendFeedback.waitForCompletion();
        assertTrue("Sending feedback should be successful", sendFeedback.isCompleted());
    }
}
