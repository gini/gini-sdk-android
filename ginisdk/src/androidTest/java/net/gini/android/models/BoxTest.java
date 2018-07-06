package net.gini.android.models;

import static net.gini.android.helpers.ParcelHelper.doRoundTrip;

import android.support.test.filters.SmallTest;
import android.test.AndroidTestCase;

@SmallTest
public class BoxTest extends AndroidTestCase {

    public void testBoxIsParcelable() {
        final Box originalBox = new Box(1, 2, 3, 4, 5);
        final Box restoredBox = doRoundTrip(originalBox, Box.CREATOR);
        assertEquals(1, restoredBox.getPageNumber());
        assertEquals(2., restoredBox.getLeft());
        assertEquals(3., restoredBox.getTop());
        assertEquals(4., restoredBox.getWidth());
        assertEquals(5., restoredBox.getHeight());
    }

}
