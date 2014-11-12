package net.gini.android;

import android.graphics.Bitmap;

import net.gini.android.authorization.Session;
import net.gini.android.authorization.SessionManager;
import net.gini.android.models.Document;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import bolts.Continuation;
import bolts.Task;

import static android.graphics.Bitmap.CompressFormat.JPEG;
import static net.gini.android.Utils.checkNotNull;

/**
 * The DocumentTaskManager is a high level API on top of the Gini API, which is used via the ApiCommunicator. It
 * provides high level methods to handle document related tasks easily.
 */
public class DocumentTaskManager {

    /** The default compression rate which is used for JPEG compression in per cent. */
    public final static int DEFAULT_COMPRESSION = 90;

    /** The ApiCommunicator instance which is used to communicate with the Gini API. */
    private final ApiCommunicator mApiCommunicator;
    /** The SessionManager instance which is used to create the documents. */
    private final SessionManager mSessionManager;


    public DocumentTaskManager(final ApiCommunicator apiCommunicator, final SessionManager sessionManager) {
        mApiCommunicator = checkNotNull(apiCommunicator);
        mSessionManager = checkNotNull(sessionManager);
    }

    /**
     * Uploads the given photo of a document and creates a new Gini document.
     *
     * @param document          A Bitmap representing the image
     * @param filename          Optional the filename of the given document.
     * @param documentType      Optional a document type hint. See the documentation for the document type hints for
     *                          possible values.
     * @param compressionRate   Optional the compression rate of the created JPEG representation of the document.
     *                          Between 0 and 90.
     * @return                  A Task which will resolve to the Document instance of the freshly created document.
     */
    public Task<Document> createDocument(final Bitmap document, @Nullable final String filename,
                                         @Nullable final String documentType, final int compressionRate) {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> sessionTask) throws Exception {
                final Session session = sessionTask.getResult();
                final ByteArrayOutputStream documentOutputStream = new ByteArrayOutputStream();
                document.compress(JPEG, compressionRate, documentOutputStream);
                final byte[] uploadData = documentOutputStream.toByteArray();
                return mApiCommunicator
                        .uploadDocument(uploadData, MediaTypes.IMAGE_JPEG, filename, documentType, session);
            }
        }).onSuccess(new Continuation<JSONObject, Document>() {
            @Override
            public Document then(Task<JSONObject> uploadTask) throws Exception {
                return Document.fromApiResponse(uploadTask.getResult());
            }
        });
    }


    /**
     * A builder to configure the upload of a bitmap.
     */
    public static class DocumentUploadBuilder {
        private final Bitmap mDocumentBitmap;
        private String mFilename;
        private String mDocumentType;
        private int mCompressionRate;

        public DocumentUploadBuilder(final Bitmap documentBitmap) {
            mDocumentBitmap = documentBitmap;
            mCompressionRate = DocumentTaskManager.DEFAULT_COMPRESSION;
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
         */
        public DocumentUploadBuilder setDocumentType(final String documentType) {
            mDocumentType = documentType;
            return this;
        }

        /**
         * The bitmap will be converted into a JPEG representation. Set the compression rate for the JPEG
         * representation.
         */
        public DocumentUploadBuilder setCompressionRate(final int compressionRate) {
            mCompressionRate = compressionRate;
            return this;
        }

        /**
         * Use the given DocumentTaskManager instance to upload the document with all the features which were set with
         * this builder.
         *
         * @param documentTaskManager   The instance of a DocumentTaskManager whill will be used to upload the document.
         * @return                      A task which will resolve to a Document instance.
         */
        public Task<Document> upload(final DocumentTaskManager documentTaskManager) {
            return documentTaskManager.createDocument(mDocumentBitmap, mFilename, mDocumentType, mCompressionRate);
        }
    }
}
