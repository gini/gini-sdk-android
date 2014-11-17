package net.gini.android.models;

import org.jetbrains.annotations.Nullable;

import static net.gini.android.Utils.checkNotNull;

public class Extraction {
    private String mValue;
    private final String mEntity;
    private Box mBox;
    private boolean mIsDirty;

    /**
     * Value object for an extraction from the Gini API.
     *
     * @param value         The extraction's value. Changing this value marks the extraction as dirty.
     * @param entity        The extraction's entity.
     * @param box           Optional the box where the extraction is found. Only available on some extractions. Changing
     *                      this value marks the extraction as dirty.
     */
    public Extraction(final String value, final String entity, @Nullable Box box) {
        mValue = checkNotNull(value);
        mEntity = checkNotNull(entity);
        mBox = box;
        mIsDirty = false;
    }

    public synchronized String getValue() {
        return mValue;
    }

    public synchronized void setValue(final String newValue) {
        mValue = newValue;
        mIsDirty = true;
    }

    public synchronized String getEntity() {
        return mEntity;
    }

    public synchronized Box getBox() {
        return mBox;
    }

    public synchronized void setBox(Box newBox) {
        mBox = newBox;
        mIsDirty = true;
    }

    public synchronized boolean isDirty() {
        return mIsDirty;
    }
}
