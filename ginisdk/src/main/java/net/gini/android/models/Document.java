package net.gini.android.models;


import static net.gini.android.Utils.checkNotNull;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Document implements Parcelable {

    /**
     * The possible processing states of a document.
     */
    public enum ProcessingState {
        /**
         * Gini is currently processing the document. The document exists, but some resources (e.g.
         * extractions) may not exist.
         */
        PENDING,
        /**
         * Gini has processed the document.
         */
        COMPLETED,
        /**
         * Gini has processed the document, but there was an error during processing.
         */
        ERROR,
        /**
         * Processing state cannot be identified.
         */
        UNKNOWN
    }

    public enum SourceClassification {
        SCANNED,
        SANDWICH,
        NATIVE,
        TEXT,
        UNKNOWN
    }


    private final String mId;
    private final ProcessingState mState;
    private final Integer mPageCount;
    private final String mFilename;
    private final Date mCreationDate;
    private final SourceClassification mSourceClassification;
    private final Uri mUri;
    private final List<Uri> mParents;
    private final List<Uri> mSubdocuments;

    public Document(final String id, final ProcessingState state, final String filename,
            final Integer pageCount,
            final Date creationDate, final SourceClassification sourceClassification,
            final Uri uri, final List<Uri> parents,
            final List<Uri> subdocuments) {
        mId = checkNotNull(id);
        mState = checkNotNull(state);
        mPageCount = pageCount;
        mFilename = filename;
        mCreationDate = creationDate;
        mSourceClassification = sourceClassification;
        mUri = uri;
        mParents = parents;
        mSubdocuments = subdocuments;
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

    public Uri getUri() {
        return mUri;
    }

    public List<Uri> getParents() {
        return mParents;
    }

    public List<Uri> getSubdocuments() {
        return mSubdocuments;
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
        ProcessingState processingState;
        try {
            processingState =
                    ProcessingState.valueOf(responseData.getString("progress"));
        } catch (IllegalArgumentException e) {
            processingState = ProcessingState.UNKNOWN;
        }
        final Integer pageCount = responseData.getInt("pageCount");
        final String fileName = responseData.getString("name");
        final Date creationDate = new Date(responseData.getLong("creationDate"));
        SourceClassification sourceClassification;
        try {
            sourceClassification =
                    SourceClassification.valueOf(responseData.getString("sourceClassification"));
        } catch (IllegalArgumentException e) {
            sourceClassification = SourceClassification.UNKNOWN;
        }
        final JSONObject links = responseData.getJSONObject("_links");
        final Uri documentUri = Uri.parse(links.getString("document"));
        final List<Uri> parentUris = parseOptionalLinkArray(links.optJSONArray("parents"));
        final List<Uri> subdocumentUris = parseOptionalLinkArray(links.optJSONArray("subdocuments"));
        return new Document(documentId, processingState, fileName, pageCount, creationDate,
                sourceClassification, documentUri, parentUris, subdocumentUris);
    }

    private static List<Uri> parseOptionalLinkArray(@Nullable final JSONArray links) {
        final List<Uri> uris = new ArrayList<>();
        if (links != null) {
            for (int i = 0; i < links.length(); i++) {
                final String uriString = links.optString(i);
                if (!TextUtils.isEmpty(uriString)) {
                    uris.add(Uri.parse(uriString));
                }
            }
        }
        return uris;
    }

    private static Document fromParcel(final Parcel in) {
        final String documentId = in.readString();
        final ProcessingState processingState = ProcessingState.valueOf(in.readString());
        final int pageCount = in.readInt();
        final String fileName = in.readString();
        final Date creationDate = (Date) in.readSerializable();
        final SourceClassification sourceClassification = SourceClassification.valueOf(
                in.readString());
        final Uri uri = in.readParcelable(Document.class.getClassLoader());
        final List<Uri> parents = new ArrayList<>();
        in.readTypedList(parents, Uri.CREATOR);
        //noinspection unchecked
        final List<Uri> subdocuments = new ArrayList<>();
        in.readTypedList(subdocuments, Uri.CREATOR);
        return new Document(documentId, processingState, fileName, pageCount, creationDate,
                sourceClassification, uri, subdocuments, subdocuments);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(getId());
        dest.writeString(getState().toString());
        dest.writeInt(getPageCount());
        dest.writeString(getFilename());
        dest.writeSerializable(getCreationDate());
        dest.writeString(getSourceClassification().toString());
        dest.writeParcelable(getUri(), flags);
        dest.writeTypedList(getParents());
        dest.writeTypedList(getSubdocuments());
    }

    public static final Parcelable.Creator<Document> CREATOR = new Parcelable.Creator<Document>() {

        public Document createFromParcel(final Parcel in) {
            return Document.fromParcel(in);
        }

        public Document[] newArray(int size) {
            return new Document[size];
        }
    };
}
