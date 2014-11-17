package net.gini.android.models;

import android.test.AndroidTestCase;

public class ExtractionTest extends AndroidTestCase {
    public Box createEmptyBox() {
        return new Box(1, 0, 0, 0, 0);
    }

    public void testExtractionIsInitiallyNotMarkedAsDirty() {
        Extraction extraction = new Extraction("bar", "amount", null);

        assertFalse(extraction.isDirty());
    }

    public void testGetValue() {
        Extraction extraction = new Extraction("bar", "amount", null);

        assertEquals("bar", extraction.getValue());
    }

    public void testGetEntity() {
        Extraction extraction = new Extraction("bar", "amount", null);

        assertEquals("amount", extraction.getEntity());
    }

    public void testThatSetValueMarksAsDirty() {
        Extraction extraction = new Extraction("bar", "amount", null);

        extraction.setValue("raboof");

        assertEquals("raboof", extraction.getValue());
        assertTrue(extraction.isDirty());
    }

    public void testThatSetBoxMarksAsDirty() {
        Extraction extraction = new Extraction("bar", "amount", null);
        Box newBox = createEmptyBox();

        extraction.setBox(newBox);

        assertEquals(newBox, extraction.getBox());
        assertTrue(extraction.isDirty());
    }
}
