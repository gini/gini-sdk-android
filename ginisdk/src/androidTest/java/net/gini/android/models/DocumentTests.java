package net.gini.android.models;

import android.test.AndroidTestCase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;


public class DocumentTests extends AndroidTestCase {

    private JSONObject createDocumentJSON() throws IOException, JSONException {
        InputStream is = getContext().getResources().getAssets().open("document.json");

        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();

        return new JSONObject(new String(buffer));
    }

    public void testDocumentIdGetter() {
        Document document = new Document("1234-5678-9012-3456", Document.ProcessingState.COMPLETED, "foobar.jpg", 1, new Date(), Document.SourceClassification.NATIVE);

        assertEquals("1234-5678-9012-3456", document.getId());
    }

    public void testDocumentState() {
        Document document = new Document("1234-5678-9012-3456", Document.ProcessingState.COMPLETED, "foobar.jpg", 1, new Date(), Document.SourceClassification.NATIVE);

        assertEquals(Document.ProcessingState.COMPLETED, document.getState());
    }

    public void testDocumentFactory() throws IOException, JSONException {
        JSONObject responseData = createDocumentJSON();

        Document document = Document.fromApiResponse(responseData);

        assertEquals("626626a0-749f-11e2-bfd6-000000000000", document.getId());
        assertEquals(Document.ProcessingState.COMPLETED, document.getState());
        assertEquals("scanned.jpg", document.getFilename());
        assertEquals(1, document.getPageCount());
        assertEquals(Document.SourceClassification.SCANNED, document.getSourceClassification());
        // Tue Feb 12 2013 00:04:27 GMT+0100 (CET)
        assertEquals(1360623867402L, document.getCreationDate().getTime());
    }
}
