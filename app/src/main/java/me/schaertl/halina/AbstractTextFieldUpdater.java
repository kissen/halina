package me.schaertl.halina;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * Abstract TextFieldUpdater for easy inheriting. All provided standard implementations
 * do nothing.
 */
public class AbstractTextFieldUpdater implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }
}
