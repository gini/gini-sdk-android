package net.gini.android.models;

import static net.gini.android.helpers.ParcelHelper.doRoundTrip;

import android.support.test.filters.SmallTest;
import android.test.AndroidTestCase;

@SmallTest
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

    public void testIsParcelable() {
        final Box box = new Box(1, 2, 3, 4, 5);
        final Extraction originalExtraction = new Extraction("42:EUR", "amount", box);

        final Extraction restoredExtraction = doRoundTrip(originalExtraction, Extraction.CREATOR);

        assertEquals("42:EUR", restoredExtraction.getValue());
        assertEquals("amount", restoredExtraction.getEntity());
        final Box restoredBox = restoredExtraction.getBox();
        // TODO: custom equals on the box model.
        assertEquals(1, restoredBox.getPageNumber());
        assertEquals(2., restoredBox.getLeft());
        assertEquals(3., restoredBox.getTop());
        assertEquals(4., restoredBox.getWidth());
        assertEquals(5., restoredBox.getHeight());
    }
}
