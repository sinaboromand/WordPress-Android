package org.wordpress.android.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: comment header explaining how to use this
 */

public class WPPrefView extends LinearLayout implements
        View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    public enum PrefType {
        TEXT,
        TOGGLE,
        CHECKLIST,
        RADIOLIST;

        public static PrefType fromInt(int value) {
            switch (value) {
                case 1:
                    return TOGGLE;
                case 2:
                    return CHECKLIST;
                case 3:
                    return RADIOLIST;
                default:
                    return TEXT;
            }
        }
    }

    public interface OnPrefChangedListener {
        public void onPrefChanged(@NonNull WPPrefView prefView);
    }

    public static class PrefListItem {
        private String mItemName;
        private String mItemValue;
        private boolean mIsChecked;

        public PrefListItem(@NonNull String itemName, @NonNull String itemValue, boolean isChecked) {
            mItemName = itemName;
            mItemValue = itemValue;
            mIsChecked = isChecked;
        }
    }

    public static class PrefListItems extends ArrayList<PrefListItem> {
        public PrefListItem getSelectedItem() {
            for (PrefListItem item: this) {
                if (item.mIsChecked) {
                    return item;
                }
            }
            return null;
        }
        public void setSelectedName(@NonNull String selectedName) {
            for (PrefListItem item: this) {
                item.mIsChecked = StringUtils.equals(selectedName, item.mItemName);
            }
        }
    }

    private PrefType mPrefType = PrefType.TEXT;
    private final PrefListItems mListItems = new PrefListItems();

    private ViewGroup mContainer;
    private TextView mHeadingTextView;
    private TextView mTitleTextView;
    private TextView mSummaryTextView;
    private View mDivider;
    private Switch mSwitch;

    private String mTextEntry;

    private OnPrefChangedListener mListener;

    public WPPrefView(Context context) {
        super(context);
        initView(context, null);
    }

    public WPPrefView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public WPPrefView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WPPrefView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    public void setOnPrefChangedListener(OnPrefChangedListener listener) {
        mListener = listener;
    }

    private void doPrefChanged() {
        if (mListener != null) {
            mListener.onPrefChanged(this);
        }
    }

    private void initView(Context context, AttributeSet attrs) {
        ViewGroup view = (ViewGroup) inflate(context, R.layout.wppref_view, this);

        mContainer = (ViewGroup) view.findViewById(R.id.container);
        mHeadingTextView = (TextView) view.findViewById(R.id.text_heading);
        mTitleTextView = (TextView) view.findViewById(R.id.text_title);
        mSummaryTextView = (TextView) view.findViewById(R.id.text_summary);
        mSwitch = (Switch) view.findViewById(R.id.switch_view);
        mDivider = view.findViewById(R.id.divider);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.wpPrefView,
                    0, 0);
            try {
                int prefTypeInt = a.getInteger(R.styleable.wpPrefView_wpPrefType, 0);
                String heading = a.getString(R.styleable.wpPrefView_wpHeading);
                String title = a.getString(R.styleable.wpPrefView_wpTitle);
                String summary = a.getString(R.styleable.wpPrefView_wpSummary);
                boolean showDivider = a.getBoolean(R.styleable.wpPrefView_wpShowDivider, true);

                setPrefType(PrefType.fromInt(prefTypeInt));
                setHeading(heading);
                setTitle(title);
                setSummary(summary);
                setShowDivider(showDivider);
            } finally {
                a.recycle();
            }
        }
    }

    public PrefType getPrefType() {
        return mPrefType;
    }

    public void setPrefType(@NonNull PrefType prefType) {
        mPrefType = prefType;

        boolean isToggle = mPrefType == PrefType.TOGGLE;
        mTitleTextView.setVisibility(isToggle ? View.GONE : View.VISIBLE);
        mSwitch.setVisibility(isToggle ? View.VISIBLE : View.GONE);

        if (isToggle) {
            mContainer.setOnClickListener(null);
            mSwitch.setOnCheckedChangeListener(this);
        } else {
            mContainer.setOnClickListener(this);
            mSwitch.setOnCheckedChangeListener(null);
        }
    }

    public void setHeading(String heading) {
        mHeadingTextView.setText(heading);
        mHeadingTextView.setVisibility(TextUtils.isEmpty(heading) ? GONE : VISIBLE);
    }

    public void setTitle(String title) {
        mTitleTextView.setText(title);
        mSwitch.setText(title);
    }

    public void setSummary(String summary) {
        mSummaryTextView.setText(summary);
        mSummaryTextView.setVisibility(TextUtils.isEmpty(summary) ? View.GONE : View.VISIBLE);
    }

    public String getTextEntry() {
        return mTextEntry;
    }

    public void setTextEntry(String entry) {
        mTextEntry = entry;
        setSummary(entry);
    }

    public boolean isChecked() {
        return mPrefType == PrefType.TOGGLE && mSwitch.isChecked();
    }

    public void setChecked(boolean checked) {
        mSwitch.setChecked(checked);
    }

    public void setShowDivider(boolean show) {
        mDivider.setVisibility(show ? VISIBLE : GONE);
    }

    public void setListItems(@NonNull PrefListItems items) {
        mListItems.clear();
        mListItems.addAll(items);
    }

    @Override
    public void onClick(View v) {
        switch (mPrefType) {
            case CHECKLIST:
                showCheckListDialog();
                break;
            case RADIOLIST:
                showRadioListDialog();
                break;
            case TEXT:
                showTextDialog();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        doPrefChanged();
    }

    private static CharSequence[] listToArray(@NonNull List<String> list) {
        CharSequence[] array = new CharSequence[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private void showTextDialog() {
        final EditText editText = new EditText(getContext());
        editText.setText(mSummaryTextView.getText());

        new AlertDialog.Builder(getContext())
                .setTitle(mTitleTextView.getText())
                .setView(editText)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doPrefChanged();
                    }
                })
        .show();
    }

    private void showCheckListDialog() {
        CharSequence[] items = new CharSequence[mListItems.size()];
        boolean[] checkedItems = new boolean[mListItems.size()];
        for (int i = 0; i < mListItems.size(); i++) {
            items[i] = mListItems.get(i).mItemName;
            checkedItems[i] = mListItems.get(i).mIsChecked;
        }

        new AlertDialog.Builder(getContext())
                .setTitle(mTitleTextView.getText())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO
                        SparseBooleanArray checkedItems =
                                ((AlertDialog) dialog).getListView().getCheckedItemPositions();
                        doPrefChanged();
                    }
                })
                .setMultiChoiceItems(items, checkedItems, null)
                .show();
    }

    private void showRadioListDialog() {
        CharSequence[] items = new CharSequence[mListItems.size()];
        int selectedPos = 0;
        for (int i = 0; i < mListItems.size(); i++) {
            items[i] = mListItems.get(i).mItemName;
            if (mListItems.get(i).mIsChecked) {
                selectedPos = i;
            }
        }
        new AlertDialog.Builder(getContext())
                .setTitle(mTitleTextView.getText())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String selectedName = mListItems.get(which).mItemName;
                        for (PrefListItem item: mListItems) {
                            item.mIsChecked = StringUtils.equals(selectedName, item.mItemName);
                        }
                        setSummary(selectedName);
                        doPrefChanged();
                    }
                })
                .setSingleChoiceItems(items, selectedPos, null)
                .show();
    }
}
