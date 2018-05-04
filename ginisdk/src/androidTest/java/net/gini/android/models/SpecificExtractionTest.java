package net.gini.android.models;

import static net.gini.android.helpers.ParcelHelper.doRoundTrip;

import android.support.test.filters.SmallTest;
import android.test.AndroidTestCase;

import java.util.ArrayList;

@SmallTest
public class SpecificExtractionTest extends AndroidTestCase {

    public void testGetName() {
        SpecificExtraction extraction =
                new SpecificExtraction("foo", "bar", "amount", null, new ArrayList<Extraction>());

        assertEquals("foo", extraction.getName());
    }

    public void testGetCandidates() {
        ArrayList<Extraction> candidates = new ArrayList<Extraction>();
        candidates.add(new Extraction("0:EUR", "amount", null));
        candidates.add(new Extraction("12.99:EUR", "amount", null));

        SpecificExtraction specificExtraction =
                new SpecificExtraction("amount", "23.23:EUR", "amount", null, candidates);

        assertEquals(candidates, specificExtraction.getCandidate());
    }

    public void testIsParcelable() {
        final ArrayList<Extraction> candidates = new ArrayList<Extraction>();
        candidates.add(new Extraction("0:EUR", "amount", null));
        candidates.add(new Extraction("12.99:EUR", "amount", null));
        final Box box = new Box(1, 2, 3, 4, 5);
        SpecificExtraction specificExtraction =
                new SpecificExtraction("amountToPay", "23.23:EUR", "amount", box, candidates);

        final SpecificExtraction restoredExtraction =
                doRoundTrip(specificExtraction, SpecificExtraction.CREATOR);

        assertEquals("amountToPay", restoredExtraction.getName());
        assertEquals("23.23:EUR", restoredExtraction.getValue());
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
