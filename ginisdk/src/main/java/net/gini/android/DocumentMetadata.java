package net.gini.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alpar Szotyori on 25.10.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */
public class DocumentMetadata {

    @VisibleForTesting
    static final String HEADER_FIELD_NAME_PREFIX = "X-Document-Metadata-";
    @VisibleForTesting
    static final String BRANCH_ID_HEADER_FIELD_NAME = HEADER_FIELD_NAME_PREFIX + "BranchId";


    private final Map<String, String> mMetadataMap = new HashMap<>();
    private CharsetEncoder mAsciiCharsetEncoder;

    public DocumentMetadata() {
        try {
            final Charset asciiCharset = Charset.forName("ASCII");
            mAsciiCharsetEncoder = asciiCharset.newEncoder();
        } catch (IllegalArgumentException ignore) {
            // Shouldn't happen
            mAsciiCharsetEncoder = null;
        }
    }

    @VisibleForTesting
    DocumentMetadata(@Nullable CharsetEncoder charsetEncoder) {
        mAsciiCharsetEncoder = charsetEncoder;
    }

    public void setBranchId(@NonNull final String branchId) throws IllegalArgumentException {
        if (isASCIIEncodable(branchId)) {
            mMetadataMap.put(BRANCH_ID_HEADER_FIELD_NAME, branchId);
        } else {
            throw new IllegalArgumentException("Metadata is not encodable as ASCII: " + branchId);
        }
    }

    @VisibleForTesting
    boolean isASCIIEncodable(@NonNull final String string) {
        if (mAsciiCharsetEncoder != null) {
            return mAsciiCharsetEncoder.canEncode(string);
        }
        // If no ASCII encoder (should never happen) then accept everything to not block metadata
        return true;
    }

    public void add(@NonNull final String name, @NonNull final String value)
            throws IllegalArgumentException {
        if (!isASCIIEncodable(name)) {
            throw new IllegalArgumentException("Metadata name is not encodable as ASCII: " + name);
        }
        if (!isASCIIEncodable(value)) {
            throw new IllegalArgumentException(
                    "Metadata value is not encodable as ASCII: " + value);
        }
        mMetadataMap.put(HEADER_FIELD_NAME_PREFIX + name, value);
    }

    @NonNull
    Map<String, String> getMetadata() {
        return mMetadataMap;
    }
}
