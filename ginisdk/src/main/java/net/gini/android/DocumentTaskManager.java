package net.gini.android;

import static android.graphics.Bitmap.CompressFormat.JPEG;

import static net.gini.android.Utils.CHARSET_UTF8;
import static net.gini.android.Utils.checkNotNull;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.gini.android.authorization.Session;
import net.gini.android.authorization.SessionManager;
import net.gini.android.models.Box;
import net.gini.android.models.Document;
import net.gini.android.models.Extraction;
import net.gini.android.models.SpecificExtraction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import bolts.Continuation;
import bolts.Task;

/**
 * The DocumentTaskManager is a high level API on top of the Gini API, which is used via the ApiCommunicator. It
 * provides high level methods to handle document related tasks easily.
 */
public class DocumentTaskManager {

    /**
     * The available document type hints. See the documentation for more information.
     */
    public enum DocumentType {
        BANK_STATEMENT("BankStatement"),
        CONTRACT("Contract"),
        INVOICE("Invoice"),
        REMINDER("Reminder"),
        REMITTANCE_SLIP("RemittanceSlip"),
        TRAVEL_EXPENSE_REPORT("TravelExpenseReport"),
        OTHER("Other");

        private final String apiDoctypeHint;

        DocumentType(String apiDoctypeHint) {
            this.apiDoctypeHint = apiDoctypeHint;
        }

        public String getApiDoctypeHint() {
            return apiDoctypeHint;
        }
    }

    /**
     * The time in milliseconds between HTTP requests when a document is polled.
     */
    public static long POLLING_INTERVAL = 1000;

    /**
     * The default compression rate which is used for JPEG compression in per cent.
     */
    public final static int DEFAULT_COMPRESSION = 50;

    /**
     * The ApiCommunicator instance which is used to communicate with the Gini API.
     */
    final ApiCommunicator mApiCommunicator;  // Visible for testing
    /**
     * The SessionManager instance which is used to create the documents.
     */
    private final SessionManager mSessionManager;

    public DocumentTaskManager(final ApiCommunicator apiCommunicator, final SessionManager sessionManager) {
        mApiCommunicator = checkNotNull(apiCommunicator);
        mSessionManager = checkNotNull(sessionManager);
    }

    /**
     * A Continuation that uses the JSON response from the Gini API and returns a new Document instance from the JSON.
     */
    private static final Continuation<JSONObject, Document> DOCUMENT_FROM_RESPONSE =
            new Continuation<JSONObject, Document>() {
                @Override
                public Document then(Task<JSONObject> task) throws Exception {
                    return Document.fromApiResponse(task.getResult());
                }
            };

    /**
     * Deletes a document.
     *
     * @param documentId The id of an existing document
     *
     * @return A Task which will resolve to an empty string.
     */
    public Task<String> deleteDocument(@NonNull final String documentId) {
        return getDocument(documentId).onSuccessTask(new Continuation<Document, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Document> documentTask) throws Exception {
                final Document document = documentTask.getResult();
                return deleteDocuments(document.getParents());
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccessTask(new Continuation<Void, Task<Session>>() {
            @Override
            public Task<Session> then(final Task<Void> task) throws Exception {
                return mSessionManager.getSession();
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccessTask(new Continuation<Session, Task<String>>() {
            @Override
            public Task<String> then(final Task<Session> task) throws Exception {
                final Session session = task.getResult();
                return mApiCommunicator.deleteDocument(documentId, session);
            }
        });
    }

    private Task<Void> deleteDocuments(@NonNull final List<Uri> documentUris) {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Session> sessionTask) throws Exception {
                final Session session = sessionTask.getResult();
                final List<Task<String>> deleteTasks = new ArrayList<>();
                for (final Uri parentUri : documentUris) {
                    deleteTasks.add(mApiCommunicator.deleteDocument(parentUri, session));
                }
                return Task.whenAll(deleteTasks);
            }
        }, Task.BACKGROUND_EXECUTOR);
    }



    /**
     * Uploads raw data and creates a new Gini partial document.
     *
     * @param document     A byte array representing an image, a pdf or UTF-8 encoded text
     * @param contentType  The media type of the uploaded data
     * @param filename     Optional the filename of the given document
     * @param documentType Optional a document type hint. See the documentation for the document type hints for
     *                     possible values
     *
     * @return A Task which will resolve to the Document instance of the freshly created document.
     */
    public Task<Document> createPartialDocument(@NonNull final byte[] document, @NonNull final String contentType,
            @Nullable final String filename, @Nullable final DocumentType documentType) {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<Uri>>() {
            @Override
            public Task<Uri> then(Task<Session> sessionTask) throws Exception {
                String apiDoctypeHint = null;
                if (documentType != null) {
                    apiDoctypeHint = documentType.getApiDoctypeHint();
                }
                final Session session = sessionTask.getResult();
                final String partialDocumentMediaType = MediaTypes
                        .forPartialDocument(checkNotNull(contentType));
                return mApiCommunicator
                        .uploadDocument(document, partialDocumentMediaType, filename, apiDoctypeHint, session);
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccessTask(new Continuation<Uri, Task<Document>>() {
            @Override
            public Task<Document> then(Task<Uri> uploadTask) throws Exception {
                return getDocument(uploadTask.getResult());
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Creates a new Gini composite document.
     *
     * @param documents    A list of partial documents which should be part of a multi-page document
     * @param documentType Optional a document type hint. See the documentation for the document type hints for
     *                     possible values
     *
     * @return A Task which will resolve to the Document instance of the freshly created document.
     */
    public Task<Document> createCompositeDocument(@NonNull final List<Document> documents, @Nullable final DocumentType documentType) {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<Uri>>() {
            @Override
            public Task<Uri> then(Task<Session> sessionTask) throws Exception {
                String apiDoctypeHint = null;
                if (documentType != null) {
                    apiDoctypeHint = documentType.getApiDoctypeHint();
                }
                final Session session = sessionTask.getResult();
                final byte[] compositeJson = createCompositeJson(documents);
                return mApiCommunicator
                        .uploadDocument(compositeJson, MediaTypes.GINI_DOCUMENT_JSON_V2, null, apiDoctypeHint, session);
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccessTask(new Continuation<Uri, Task<Document>>() {
            @Override
            public Task<Document> then(Task<Uri> uploadTask) throws Exception {
                return getDocument(uploadTask.getResult());
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Creates a new Gini composite document. The input Map must contain the partial documents as keys. These will be
     * part of the multi-page document. The value for each partial document key is the amount in degrees the document
     * has been rotated by the user.
     *
     * @param documentRotationMap A map of partial documents and their rotation in degrees
     * @param documentType        Optional a document type hint. See the documentation for the document type hints for
     *                            possible values
     *
     * @return A Task which will resolve to the Document instance of the freshly created document.
     */
    public Task<Document> createCompositeDocument(@NonNull final Map<Document, Integer> documentRotationMap, @Nullable final DocumentType documentType) {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<Uri>>() {
            @Override
            public Task<Uri> then(Task<Session> sessionTask) throws Exception {
                String apiDoctypeHint = null;
                if (documentType != null) {
                    apiDoctypeHint = documentType.getApiDoctypeHint();
                }
                final Session session = sessionTask.getResult();
                final byte[] compositeJson = createCompositeJson(documentRotationMap);
                return mApiCommunicator
                        .uploadDocument(compositeJson, MediaTypes.GINI_DOCUMENT_JSON_V2, null, apiDoctypeHint, session);
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccessTask(new Continuation<Uri, Task<Document>>() {
            @Override
            public Task<Document> then(Task<Uri> uploadTask) throws Exception {
                return getDocument(uploadTask.getResult());
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    private byte[] createCompositeJson(@NonNull final List<Document> documents)
            throws JSONException {
        final Map<Document, Integer> documentRotationMap = new HashMap<>();
        for (final Document document : documents) {
            documentRotationMap.put(document, 0);
        }
        return createCompositeJson(documentRotationMap);
    }

    private byte[] createCompositeJson(@NonNull final Map<Document, Integer> documentRotationMap)
            throws JSONException {
        final JSONObject jsonObject = new JSONObject();
        final JSONArray subdocuments = new JSONArray();
        for (final Map.Entry<Document, Integer> entry : documentRotationMap.entrySet()) {
            final Document document = entry.getKey();
            int rotation = entry.getValue();
            // Converts input degrees to degrees between [0,360)
            rotation = ((rotation % 360) + 360) % 360;
            final JSONObject partialDoc = new JSONObject();
            partialDoc.put("document", document.getUri());
            partialDoc.put("rotationDelta", rotation);
            subdocuments.put(partialDoc);
        }
        jsonObject.put("subdocuments", subdocuments);
        return jsonObject.toString().getBytes(CHARSET_UTF8);
    }

    /**
     * Uploads raw data and creates a new Gini document.
     *
     * @param document     A byte array representing an image, a pdf or UTF-8 encoded text
     * @param filename     Optional the filename of the given document.
     * @param documentType Optional a document type hint. See the documentation for the document type hints for
     *                     possible values.
     *
     * @return A Task which will resolve to the Document instance of the freshly created document.
     */
    public Task<Document> createDocument(final byte[] document, @Nullable final String filename,
            @Nullable final DocumentType documentType) {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<Uri>>() {
            @Override
            public Task<Uri> then(Task<Session> sessionTask) throws Exception {
                String apiDoctypeHint = null;
                if (documentType != null) {
                    apiDoctypeHint = documentType.getApiDoctypeHint();
                }
                final Session session = sessionTask.getResult();
                return mApiCommunicator
                        .uploadDocument(document, MediaTypes.IMAGE_JPEG, filename, apiDoctypeHint, session);
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccessTask(new Continuation<Uri, Task<Document>>() {
            @Override
            public Task<Document> then(Task<Uri> uploadTask) throws Exception {
                return getDocument(uploadTask.getResult());
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Uploads the given photo of a document and creates a new Gini document.
     *
     * @deprecated Use {@link #createDocument(Bitmap, String, DocumentType)} instead.
     *
     * @param document        A Bitmap representing the image
     * @param filename        Optional the filename of the given document.
     * @param documentType    Optional a document type hint. See the documentation for the document type hints for
     *                        possible values.
     * @param compressionRate Optional the compression rate of the created JPEG representation of the document.
     *                        Between 0 and 90.
     *
     * @return A Task which will resolve to the Document instance of the freshly created document.
     */
    @Deprecated
    public Task<Document> createDocument(final Bitmap document, @Nullable final String filename,
                                         @Nullable final String documentType, final int compressionRate) {
        return createDocumentInternal(document, filename, documentType, compressionRate);
    }

    /**
     * Uploads the given photo of a document and creates a new Gini document.
     *
     * @param document        A Bitmap representing the image
     * @param filename        Optional the filename of the given document.
     * @param documentType    Optional a document type hint.
     *
     * @return A Task which will resolve to the Document instance of the freshly created document.
     */
    public Task<Document> createDocument(final Bitmap document, @Nullable final String filename,
                                          @Nullable final DocumentType documentType) {
        String apiDoctypeHint = null;
        if (documentType != null) {
            apiDoctypeHint = documentType.getApiDoctypeHint();
        }
        return createDocumentInternal(document, filename, apiDoctypeHint, DEFAULT_COMPRESSION);
    }

    private Task<Document> createDocumentInternal(final Bitmap document, @Nullable final String filename,
                                         @Nullable final String apiDoctypeHint, final int compressionRate) {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<Uri>>() {
            @Override
            public Task<Uri> then(Task<Session> sessionTask) throws Exception {
                final Session session = sessionTask.getResult();
                final ByteArrayOutputStream documentOutputStream = new ByteArrayOutputStream();
                document.compress(JPEG, compressionRate, documentOutputStream);
                final byte[] uploadData = documentOutputStream.toByteArray();
                return mApiCommunicator
                        .uploadDocument(uploadData, MediaTypes.IMAGE_JPEG, filename, apiDoctypeHint, session);
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccessTask(new Continuation<Uri, Task<Document>>() {
            @Override
            public Task<Document> then(Task<Uri> uploadTask) throws Exception {
                return getDocument(uploadTask.getResult());
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Get the extractions for the given document.
     *
     * @param document The Document instance for whose document the extractions are returned.
     *
     * @return A Task which will resolve to a mapping, where the key is a String with the name of the
     * specific. See the
     * <a href="http://developer.gini.net/gini-api/html/document_extractions.html">Gini API documentation</a>
     * for a list of the names of the specific extractions.
     */
    public Task<Map<String, SpecificExtraction>> getExtractions(final Document document) {
        final String documentId = document.getId();
        return mSessionManager.getSession()
                .onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
                    @Override
                    public Task<JSONObject> then(Task<Session> sessionTask) {
                        final Session session = sessionTask.getResult();
                        return mApiCommunicator.getExtractions(documentId, session);
                    }
                }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(new Continuation<JSONObject, Map<String, SpecificExtraction>>() {
                    @Override
                    public Map<String, SpecificExtraction> then(Task<JSONObject> task) throws Exception {
                        final JSONObject responseData = task.getResult();
                        final JSONObject candidatesData = responseData.getJSONObject("candidates");
                        HashMap<String, List<Extraction>> candidates =
                                extractionCandidatesFromApiResponse(candidatesData);

                        final HashMap<String, SpecificExtraction> extractionsByName =
                                new HashMap<String, SpecificExtraction>();
                        final JSONObject extractionsData = responseData.getJSONObject("extractions");
                        @SuppressWarnings("unchecked")
                        // Quote Android Source: "/* Return a raw type for API compatibility */"
                        final Iterator<String> extractionsNameIterator = extractionsData.keys();
                        while (extractionsNameIterator.hasNext()) {
                            final String extractionName = extractionsNameIterator.next();
                            final JSONObject extractionData = extractionsData.getJSONObject(extractionName);
                            final Extraction extraction = extractionFromApiResponse(extractionData);
                            List<Extraction> candidatesForExtraction = new ArrayList<Extraction>();
                            if (extractionData.has("candidates")) {
                                final String candidatesName = extractionData.getString("candidates");
                                if (candidates.containsKey(candidatesName)) {
                                    candidatesForExtraction = candidates.get(candidatesName);
                                }
                            }
                            final SpecificExtraction specificExtraction =
                                    new SpecificExtraction(extractionName, extraction.getValue(),
                                            extraction.getEntity(), extraction.getBox(),
                                            candidatesForExtraction);
                            extractionsByName.put(extractionName, specificExtraction);
                        }

                        return extractionsByName;
                    }
                }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Get the document with the given unique identifier.
     *
     * @param documentId The unique identifier of the document.
     *
     * @return A document instance representing all the document's metadata.
     */
    public Task<Document> getDocument(final String documentId) {
        checkNotNull(documentId);
        return mSessionManager.getSession()
                .onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
                    @Override
                    public Task<JSONObject> then(Task<Session> sessionTask) throws Exception {
                        final Session session = sessionTask.getResult();
                        return mApiCommunicator.getDocument(documentId, session);
                    }
                }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(DOCUMENT_FROM_RESPONSE, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Get the document with the given unique identifier.
     *
     * <b>Please note that this method may use a slightly corrected URI from which it gets the document (e.g. if the
     * URI's host does not conform to the base URL of the Gini API). Therefore it is not possibly to use this method to
     * get a document from an arbitrary URI.</b>
     *
     * @param documentUri The URI of the document.
     *
     * @return A document instance representing all the document's metadata.
     */
    public Task<Document> getDocument(final Uri documentUri) {
        checkNotNull(documentUri);
        return mSessionManager.getSession()
                .onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
                    @Override
                    public Task<JSONObject> then(Task<Session> sessionTask) throws Exception {
                        final Session session = sessionTask.getResult();
                        return mApiCommunicator.getDocument(documentUri, session);
                    }
                }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(DOCUMENT_FROM_RESPONSE, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Continually checks the document status (via the Gini API) until the document is fully processed. To avoid
     * flooding the network, there is a pause of at least the number of seconds that is set in the POLLING_INTERVAL
     * constant of this class.
     *
     * <b>This method returns a Task which will resolve to a new document instance. It does not update the given
     * document instance.</b>
     *
     * @param document The document which will be polled.
     */
    public Task<Document> pollDocument(final Document document) {
        if (document.getState() != Document.ProcessingState.PENDING) {
            return Task.forResult(document);
        }
        final String documentId = document.getId();
        return getDocument(documentId).continueWithTask(new Continuation<Document, Task<Document>>() {
            @Override
            public Task<Document> then(Task<Document> task) throws Exception {
                if (task.isFaulted() || task.isCancelled()
                        || task.getResult().getState() != Document.ProcessingState.PENDING) {
                    return task;
                } else {
                    // The continuation is executed in a background thread by Bolts, so it does not block the UI
                    // when we sleep here. Infinite recursions are also prevented by Bolts (the task will then resolve
                    // to a failure).
                    Thread.sleep(POLLING_INTERVAL);
                    return pollDocument(document);
                }
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Sends approved and conceivably corrected extractions for the given document. This is called "submitting feedback
     * on extractions" in
     * the Gini API documentation.
     *
     * @param document    The document for which the extractions should be updated.
     * @param extractions A Map where the key is the name of the specific extraction and the value is the
     *                    SpecificExtraction object. This is the same structure as returned by the getExtractions
     *                    method of this manager.
     *
     * @return A Task which will resolve to the same document instance when storing the updated
     * extractions was successful.
     *
     * @throws JSONException When a value of an extraction is not JSON serializable.
     */
    public Task<Document> sendFeedbackForExtractions(final Document document,
                                                     final Map<String, SpecificExtraction> extractions)
            throws JSONException {
        final String documentId = document.getId();
        final JSONObject feedbackForExtractions = new JSONObject();
        for (Map.Entry<String, SpecificExtraction> entry : extractions.entrySet()) {
            final Extraction extraction = entry.getValue();
            final JSONObject extractionData = new JSONObject();
            extractionData.put("value", extraction.getValue());
            extractionData.put("entity", extraction.getEntity());
            feedbackForExtractions.put(entry.getKey(), extractionData);
        }

        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) throws Exception {
                final Session session = task.getResult();
                return mApiCommunicator.sendFeedback(documentId, feedbackForExtractions, session);
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccess(new Continuation<JSONObject, Document>() {
            @Override
            public Document then(Task<JSONObject> task) throws Exception {
                for (Map.Entry<String, SpecificExtraction> entry : extractions.entrySet()) {
                    entry.getValue().setIsDirty(false);
                }
                return document;
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Sends an error report for the given document to Gini. If the processing result for a document was not
     * satisfactory (e.g. extractions where empty or incorrect), you can create an error report for a document. This
     * allows Gini to analyze and correct the problem that was found.
     *
     * <b>The owner of this document must agree that Gini can use this document for debugging and error analysis.</b>
     *
     * @param document    The erroneous document.
     * @param summary     Optional a short summary of the occurred error.
     * @param description Optional a more detailed description of the occurred error.
     *
     * @return A Task which will resolve to an error ID. This is a unique identifier for your error report
     * and can be used to refer to the reported error towards the Gini support.
     */
    public Task<String> reportDocument(final Document document, final @Nullable String summary,
                                       final @Nullable String description) {
        final String documentId = document.getId();
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) throws Exception {
                final Session session = task.getResult();
                return mApiCommunicator.errorReportForDocument(documentId, summary, description, session);
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccess(new Continuation<JSONObject, String>() {
            @Override
            public String then(Task<JSONObject> task) throws Exception {
                final JSONObject responseData = task.getResult();
                return responseData.getString("errorId");
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Gets the layout of a document. The layout of the document describes the textual content of a document with
     * positional information, based on the processed document.
     *
     * @param document The document for which the layouts is requested.
     *
     * @return A task which will resolve to a string containing the layout xml.
     */
    public Task<JSONObject> getLayout(final Document document) {
        final String documentId = document.getId();
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) throws Exception {
                final Session session = task.getResult();
                return mApiCommunicator.getLayoutForDocument(documentId, session);
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Helper method which takes the JSON response of the Gini API as input and returns a mapping where the key is the
     * name of the candidates list (e.g. "amounts" or "dates") and the value is a list of extraction instances.
     *
     * @param responseData The JSON data of the key candidates from the response of the Gini API.
     *
     * @return The created mapping as described above.
     *
     * @throws JSONException If the JSON data does not have the expected structure or if there is invalid data.
     */
    protected HashMap<String, List<Extraction>> extractionCandidatesFromApiResponse(final JSONObject responseData)
            throws JSONException {
        final HashMap<String, List<Extraction>> candidatesByEntity = new HashMap<String, List<Extraction>>();

        @SuppressWarnings("unchecked") // Quote Android Source: "/* Return a raw type for API compatibility */"
        final Iterator<String> entityNameIterator = responseData.keys();
        while (entityNameIterator.hasNext()) {
            final String entityName = entityNameIterator.next();
            final JSONArray candidatesListData = responseData.getJSONArray(entityName);
            final ArrayList<Extraction> candidates = new ArrayList<Extraction>();
            for (int i = 0, length = candidatesListData.length(); i < length; i += 1) {
                final JSONObject extractionData = candidatesListData.getJSONObject(i);
                candidates.add(extractionFromApiResponse(extractionData));
            }
            candidatesByEntity.put(entityName, candidates);
        }
        return candidatesByEntity;
    }

    /**
     * Helper method which creates an Extraction instance from the JSON data which is returned by the Gini API.
     *
     * @param responseData The JSON data.
     *
     * @return The created Extraction instance.
     *
     * @throws JSONException If the JSON data does not have the expected structure or if there is invalid data.
     */
    protected Extraction extractionFromApiResponse(final JSONObject responseData) throws JSONException {
        final String entity = responseData.getString("entity");
        final String value = responseData.getString("value");
        // The box is optional for some extractions.
        Box box = null;
        if (responseData.has("box")) {
            box = Box.fromApiResponse(responseData.getJSONObject("box"));
        }
        return new Extraction(value, entity, box);
    }


    /**
     * A builder to configure the upload of a bitmap.
     */
    public static class DocumentUploadBuilder {

        private byte[] mDocumentBytes;
        private Bitmap mDocumentBitmap;
        private String mFilename;
        private String mDocumentType;
        private DocumentType mDocumentTypeHint;
        private int mCompressionRate;

        public DocumentUploadBuilder() {
            mCompressionRate = DocumentTaskManager.DEFAULT_COMPRESSION;
        }

        /**
         * @deprecated Use {@link #DocumentUploadBuilder()} instead.
         *
         * @param documentBitmap A Bitmap representing the image.
         */
        @Deprecated
        public DocumentUploadBuilder(final Bitmap documentBitmap) {
            mDocumentBitmap = documentBitmap;
            mCompressionRate = DocumentTaskManager.DEFAULT_COMPRESSION;
        }

        /**
         * Set the document as a byte array. If a {@link Bitmap} was also set, the bitmap will be used.
         */
        public DocumentUploadBuilder setDocumentBytes(byte[] documentBytes) {
            this.mDocumentBytes = documentBytes;
            return this;
        }

        /**
         * Set the document as a {@link Bitmap}. This bitmap will be used instead of the byte array, if both were set.
         */
        public DocumentUploadBuilder setDocumentBitmap(Bitmap documentBitmap) {
            this.mDocumentBitmap = documentBitmap;
            return this;
        }

        /**
         * Set the document' s filename.
         */
        public DocumentUploadBuilder setFilename(final String filename) {
            mFilename = filename;
            return this;
        }

        /**
         * Set the document's type. (This feature is called document type hint in the Gini API documentation). By
         * providing the doctype, Gini’s document processing is optimized in many ways.
         *
         * @deprecated Use {@link #setDocumentType(DocumentType)} instead.
         */
        @Deprecated
        public DocumentUploadBuilder setDocumentType(final String documentType) {
            mDocumentType = documentType;
            return this;
        }

        /**
         * Set the document's type. (This feature is called document type hint in the Gini API documentation). By
         * providing the doctype, Gini’s document processing is optimized in many ways.
         */
        public DocumentUploadBuilder setDocumentType(final DocumentType documentType) {
            mDocumentTypeHint = documentType;
            return this;
        }

        /**
         * The bitmap (if set) will be converted into a JPEG representation. Set the compression rate for the JPEG
         * representation.
         *
         * @deprecated The default compression rate is set to get the best extractions for the smallest image byte size.
         */
        @Deprecated
        public DocumentUploadBuilder setCompressionRate(final int compressionRate) {
            mCompressionRate = compressionRate;
            return this;
        }

        /**
         * Use the given DocumentTaskManager instance to upload the document with all the features which were set with
         * this builder.
         *
         * @param documentTaskManager The instance of a DocumentTaskManager whill will be used to upload the document.
         *
         * @return A task which will resolve to a Document instance.
         */
        public Task<Document> upload(final DocumentTaskManager documentTaskManager) {
            if (mDocumentBitmap != null) {
                if (mDocumentTypeHint != null) {
                    return documentTaskManager.createDocument(mDocumentBitmap, mFilename, mDocumentTypeHint);
                } else {
                    return documentTaskManager.createDocument(mDocumentBitmap, mFilename, mDocumentType, mCompressionRate);
                }
            } else {
                return documentTaskManager.createDocument(mDocumentBytes, mFilename, mDocumentTypeHint);
            }
        }
    }
}
