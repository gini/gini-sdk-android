package net.gini.android.models;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.gini.android.Utils.checkNotNull;

public class SpecificExtraction extends Extraction {
    private final String mName;
    private final List<Extraction> mCandidates;

    /**
     * Value object for a specific extraction from the Gini API.
     *
     * @param value  The extraction's value. Changing this value marks the extraction as dirty.
     * @param entity The extraction's entity.
     * @param box    Optional the box where the extraction is found. Only available on some extractions.
     */
    public SpecificExtraction(final String name, final String value, final String entity, @Nullable final Box box, final
                              List<Extraction> candidates) {
        super(value, entity, box);

        mName = checkNotNull(name);
        mCandidates = checkNotNull(candidates);
    }

    public String getName() {
        return mName;
    }

    public List<Extraction> getCandidate() {
        return mCandidates;
    }
}
