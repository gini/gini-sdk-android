package net.gini.android.models;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import static net.gini.android.Utils.checkNotNull;

public class Document {

    /**
     * The possible processing states of a document.
     */
    public enum ProcessingState {
        /**
         * Gini is currently processing the document. The document exists, but some resources (e.g. extractions) may not
         * exist.
         */
        PENDING,
        /**
         * Gini has processed the document.
         */
        COMPLETED,
        /**
         * Gini has processed the document, but there was an error during processing.
         */
        ERROR
    }

    public enum SourceClassification {
        SCANNED,
        NATIVE
    }


    private final String mId;
    private final ProcessingState mState;
    private final Integer mPageCount;
    private final String mFilename;
    private final Date mCreationDate;
    private final SourceClassification mSourceClassification;

    public Document(final String id, final ProcessingState state, final String filename, final Integer pageCount,
                    final Date creationDate, final SourceClassification sourceClassification) {
        mId = checkNotNull(id);
        mState = checkNotNull(state);
        mPageCount = pageCount;
        mFilename = filename;
        mCreationDate = creationDate;
        mSourceClassification = sourceClassification;
    }

    /**
     * The document's unique identifier.
     */
    public String getId() {
        return mId;
    }

    /**
     * The document's processing state.
     */
    public ProcessingState getState() {
        return mState;
    }

    /**
     * The number of pages.
     */
    public int getPageCount() {
        return mPageCount;
    }

    /**
     * The document's filename (as stated on upload).
     */
    public String getFilename() {
        return mFilename;
    }

    /**
     * The document's creation date.
     */
    public Date getCreationDate() {
        return mCreationDate;
    }

    /**
     * Classification of the source file.
     */
    public SourceClassification getSourceClassification() {
        return mSourceClassification;
    }

    /**
     * Creates a new document instance from the JSON data usually returned by the Gini API.
     *
     * @param responseData The response data. Should be a valid response.
     * @return The created document instance.
     */
    public static Document fromApiResponse(JSONObject responseData) throws JSONException {
        final String documentId = responseData.getString("id");
        final ProcessingState processingState = ProcessingState.valueOf(responseData.getString("progress"));
        final Integer pageCount = responseData.getInt("pageCount");
        final String fileName = responseData.getString("name");
        final Date creationDate = new Date(responseData.getLong("creationDate"));
        final SourceClassification sourceClassification =
                SourceClassification.valueOf(responseData.getString("sourceClassification"));

        return new Document(documentId, processingState, fileName, pageCount, creationDate, sourceClassification);
    }
}
