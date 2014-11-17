package net.gini.android.models;

import android.test.AndroidTestCase;

import java.util.ArrayList;

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
}
