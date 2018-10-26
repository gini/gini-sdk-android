package net.gini.android;

import android.support.test.filters.SmallTest;
import android.test.InstrumentationTestCase;

import java.util.Map;

/**
 * Created by Alpar Szotyori on 25.10.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

@SmallTest
public class DocumentMetadataTest extends InstrumentationTestCase {

    private DocumentMetadata mDocumentMetadata;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDocumentMetadata = new DocumentMetadata();
    }

    public void testBranchIdFieldName() {
        final String branchId = "4321";
        mDocumentMetadata.setBranchId(branchId);

        assertFalse(mDocumentMetadata.getMetadata().isEmpty());
        boolean containsBranchId = false;
        for (final Map.Entry<String, String> entry : mDocumentMetadata.getMetadata().entrySet()) {
            if (entry.getValue().equals(branchId)) {
                containsBranchId = true;
                final String fieldName = entry.getKey();
                assertTrue(fieldName.startsWith(DocumentMetadata.HEADER_FIELD_NAME_PREFIX));
                assertEquals(DocumentMetadata.BRANCH_ID_HEADER_FIELD_NAME ,fieldName);
            }
        }
        assertTrue(containsBranchId);
    }

    public void testBranchIdMustBeASCIIEncodable() {
        IllegalArgumentException exception = null;
        try {
            mDocumentMetadata.setBranchId("Bräustüberl");
        } catch (IllegalArgumentException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    public void testCustomFieldNameContainsHeaderPrefix() {
        final String name = "App";
        final String value = "DieBank";
        mDocumentMetadata.add(name, value);

        assertFalse(mDocumentMetadata.getMetadata().isEmpty());
        boolean containsBranchId = false;
        for (final Map.Entry<String, String> entry : mDocumentMetadata.getMetadata().entrySet()) {
            if (entry.getValue().equals(value)) {
                containsBranchId = true;
                assertEquals(DocumentMetadata.HEADER_FIELD_NAME_PREFIX + name, entry.getKey());
            }
        }
        assertTrue(containsBranchId);
    }

    public void testCustomFieldNameHeaderPrefixIsNotAddedTwice() {
        final String name = DocumentMetadata.HEADER_FIELD_NAME_PREFIX + "App";
        final String value = "DieBank";
        mDocumentMetadata.add(name, value);

        assertFalse(mDocumentMetadata.getMetadata().isEmpty());
        boolean containsBranchId = false;
        for (final Map.Entry<String, String> entry : mDocumentMetadata.getMetadata().entrySet()) {
            if (entry.getValue().equals(value)) {
                containsBranchId = true;
                assertEquals(name, entry.getKey());
            }
        }
        assertTrue(containsBranchId);
    }

    public void testCustomFieldNameMustBeASCIIEncodable() {
        IllegalArgumentException exception = null;
        try {
            mDocumentMetadata.add("Übung", "Rumpfdrehen");
        } catch (IllegalArgumentException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    public void testCustomFieldValueMustBeASCIIEncodable() {
        IllegalArgumentException exception = null;
        try {
            mDocumentMetadata.add("App", "Fotoüberweisung");
        } catch (IllegalArgumentException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    public void testAcceptsAllStringsWhenASCIIEncoderIsNotAvailable() {
        final DocumentMetadata documentMetadata = new DocumentMetadata(null);
        assertTrue(documentMetadata.isASCIIEncodable("Bräustüberl"));
    }

}