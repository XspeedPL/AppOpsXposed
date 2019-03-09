package at.jclehner.appopsxposed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.annotation.LayoutRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import at.jclehner.appopsxposed.re.R;

public class IconPreference extends Preference implements AdapterView.OnItemSelectedListener {
    private final LayoutInflater mInflater;
    private Spinner mSpinner;
    private int[] mIcons;

    private int mValue;
    private boolean mWasValueSet = false;

    public IconPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.spinner);
        mInflater = LayoutInflater.from(context);
    }

    public void setIcons(int[] icons) {
        mIcons = icons;
        updateSpinner();
    }

    @Override
    protected void onClick() {
        super.onClick();

        if (mSpinner != null)
            mSpinner.performClick();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (callChangeListener(position))
            setValue(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mValue) : (int) defaultValue);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mSpinner = (Spinner) view.findViewById(R.id.spinnerWidget);

        ViewParent parent = mSpinner.getParent();
        if (parent instanceof View) {
            Rect rect = new Rect(0, 0, ((View) parent).getWidth(),
                    ((View) parent).getHeight());
            ((View) parent).setTouchDelegate(new TouchDelegate(rect, (View) parent));

        }

        updateSpinner();
        mSpinner.setOnItemSelectedListener(this);
    }

    private void setValue(int value) {
        boolean changed = mValue != value;
        if (changed || !mWasValueSet) {
            mValue = value;
            mWasValueSet = true;
            persistInt(value);
            if (changed)
                notifyChanged();
        }
    }

    private void updateSpinner() {
        if (mSpinner == null)
            return;

        mSpinner.setAdapter(null);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setSelection(mValue);
    }

    private final SpinnerAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mIcons != null ? mIcons.length : 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getView(position, R.layout.icon_spinner);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View v = getView(position, R.layout.icon_dropdown);
            v.setBackgroundResource(position != mValue ? R.drawable.checkerboard
                    : R.drawable.checkerboard_framed);
            return v;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return mIcons[position];
        }

        @SuppressLint("InflateParams")
        private ImageView getView(int position, @LayoutRes int layoutResId) {
            ImageView view = (ImageView) mInflater.inflate(layoutResId, null, false);
            view.setImageResource(mIcons[position]);
            return view;
        }
    };
}
