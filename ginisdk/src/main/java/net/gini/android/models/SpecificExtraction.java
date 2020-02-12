package net.gini.android.models;

import static net.gini.android.Utils.checkNotNull;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpecificExtraction extends Extraction {

    private final String mName;
    private final List<Extraction> mCandidates;
    private final List<SpecificExtraction> mSpecificExtractions;

    /**
     * Value object for a specific extraction from the Gini API.
     *
     * @param name       The specific extraction's name, e.g. "amountToPay".
     * @param value      The extraction's value. Changing this value marks the extraction as dirty.
     * @param entity     The extraction's entity.
     * @param box        Optional the box where the extraction is found. Only available on some
     *                   extractions.
     * @param candidates A list containing other candidates for this specific extraction. Candidates
     *                   are of the same entity as the found extraction.
     */
    public SpecificExtraction(final String name, final String value, final String entity,
                              @Nullable final Box box, final List<Extraction> candidates) {
        this(name, value, entity, box, candidates, Collections.<SpecificExtraction>emptyList());
    }

    /**
     * Value object for a specific extraction from the Gini API.
     *
     * @param name                  The specific extraction's name, e.g. "amountToPay".
     * @param value                 The extraction's value. Changing this value marks the extraction as dirty.
     * @param entity                The extraction's entity.
     * @param box                   Optional the box where the extraction is found. Only available on some
     *                              extractions.
     * @param candidates            A list containing other candidates for this specific extraction. Candidates
     *                              are of the same entity as the found extraction.
     * @param specificExtractions   A list of other specific extractions which are part of this one.
     */
    public SpecificExtraction(final String name, final String value, final String entity,
            @Nullable final Box box, final List<Extraction> candidates, final List<SpecificExtraction> specificExtractions) {
        super(value, entity, box);

        mName = checkNotNull(name);
        mCandidates = checkNotNull(candidates);
        mSpecificExtractions = specificExtractions;
    }

    /**
     * Private constructor to create an extraction from a parceled extraction.
     */
    private SpecificExtraction(final Parcel in) {
        super(in);
        mName = in.readString();
        final List<Extraction> candidates = new ArrayList<Extraction>();
        in.readTypedList(candidates, Extraction.CREATOR);
        mCandidates = candidates;
        final List<SpecificExtraction> specificExtractions = new ArrayList<>();
        in.readTypedList(specificExtractions, SpecificExtraction.CREATOR);
        mSpecificExtractions = specificExtractions;
    }

    public String getName() {
        return mName;
    }

    public List<Extraction> getCandidate() {
        return mCandidates;
    }

    public List<SpecificExtraction> getSpecificExtractions() {
        return mSpecificExtractions;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mName);
        dest.writeTypedList(mCandidates);
        dest.writeTypedList(mSpecificExtractions);
    }

    public static final Parcelable.Creator<SpecificExtraction> CREATOR =
            new Parcelable.Creator<SpecificExtraction>() {

                public SpecificExtraction createFromParcel(final Parcel in) {
                    return new SpecificExtraction(in);
                }

                public SpecificExtraction[] newArray(int size) {
                    return new SpecificExtraction[size];
                }

            };
}
