package com.example.admin.expenses;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.example.admin.expenses.data.ExpensesDatabase;
import com.example.admin.expenses.data.Window;
import com.example.admin.expenses.fragments.AddItemDialogFragment;
import com.example.admin.expenses.fragments.AddParticipantDialogFragment;
import com.example.admin.expenses.helpers.Helper;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Locale;

public class ItemsActivity extends AppCompatActivity {

    ExpensesDatabase db;
    LinearLayout layoutItems;
    LinearLayout layoutParticipants;
    String[] participants;
    long windowID;
    long currentItemId;
    double totalSpent;
    Helper helper;
    TabHost tabHost;
    int textViewsLeftMargin;

    final int PARTICIPANT_LIST_ELEMENT_MINIMAL_HEIGHT = 100;
    final int ADD_BUTTON_DIMENSION = 45;
    final int DELETE_BUTTON_DIMENSION = 35;
    final int FONT_SIZE_NORMAL = 10;
    final int FONT_SIZE_TOTALS = 8;
    final int ITEM_TEXT_LEFT_MARGIN = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeActivity();
        setActionBar();
        setUpTabs();

        Cursor itemCursor = db.item().selectByWindowId(windowID);

        while (itemCursor.moveToNext()) {
            currentItemId = itemCursor.getLong(itemCursor.getColumnIndex("id"));

            addItemConstraintLayout(itemCursor);
            increaseTotalSum(itemCursor);
        }

        itemCursor.close();

        setTotalsText();
        setAddItemButton();

        if (participants.length > 0) {
            createParticipantsList();
        }

        setAddParticipantsButton();
    }

    private void addItemConstraintLayout(Cursor itemCursor) {
        TextView itemTextView = getItemTextView(itemCursor);
        ImageButton deleteItemButton = getDeletionButtonForItem();

        ConstraintLayout layout = new ConstraintLayout(this);
        layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.addView(itemTextView);
        layout.addView(deleteItemButton);

        int minHeight = helper.getPixelsFromDps(PARTICIPANT_LIST_ELEMENT_MINIMAL_HEIGHT);
        float textSize = (float) helper.getPixelsFromDps(FONT_SIZE_NORMAL);
        layout.setMinHeight(minHeight);
        layout.setBackgroundResource(R.drawable.list_item_border);
        itemTextView.setTextSize(textSize);

        ConstraintSet set = new ConstraintSet();
        set.clone(layout);

        set.connect(itemTextView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, textViewsLeftMargin);
        set.connect(itemTextView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        set.connect(itemTextView.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        set.applyTo(layout);

        set.connect(deleteItemButton.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 20);
        set.connect(deleteItemButton.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        set.connect(deleteItemButton.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        set.applyTo(layout);

        layoutItems.addView(layout);
    }

    private void initializeActivity()
    {
        Intent intent = getIntent();
        setContentView(R.layout.activity_items);
        initializeFields(intent);
    }

    private void initializeFields(Intent intent)
    {
        db = ExpensesDatabase.getInstance(this);
        layoutItems = findViewById(R.id.layout_item_list);
        layoutParticipants = findViewById(R.id.layout_participants_list);
        windowID = intent.getLongExtra("window_id", 0);
        totalSpent = 0;
        String participantsField = db.window().selectParticipantsByWindowId(windowID);

        if (participantsField != null) {
            participants = participantsField.split(",");
        } else {
            participants = new String[0];
        }

        float displayDensity = getResources().getDisplayMetrics().density;
        helper = new Helper(displayDensity);
        tabHost = findViewById(R.id.tabHost);
        tabHost.setup();

        textViewsLeftMargin = helper.getPixelsFromDps(ITEM_TEXT_LEFT_MARGIN);
    }

    private void setUpTabs()
    {
        TabHost.TabSpec specs = getTabSpecForTitle(getResources().getString(R.string.items_activity_tab_expenses_title), R.id.tab_layout_items);

        tabHost.addTab(specs);

        specs = getTabSpecForTitle(getResources().getString(R.string.items_activity_tab_participants_title), R.id.tab_layout_participants);
        tabHost.addTab(specs);
    }

    private TabHost.TabSpec getTabSpecForTitle(String title, int layoutID)
    {
        TabHost.TabSpec specs = tabHost.newTabSpec(title);

        View view = LayoutInflater.from(this).inflate(R.layout.tab_title, null);
        TextView titleView = view.findViewById(R.id.tabsText);
        titleView.setText(title);

        specs.setIndicator(view);
        specs.setContent(layoutID);
        return specs;
    }

    private TextView getItemTextView(Cursor cursor)
    {
        TextView itemTextView = new TextView(this);
        String description = cursor.getString(cursor.getColumnIndex("description"));
        double sum = cursor.getDouble(cursor.getColumnIndex("sum"));

        String itemText = String.format("%s\nSum:%s", description, Double.toString(sum));

        itemTextView.setText(itemText);
        itemTextView.setId(View.generateViewId());

        return itemTextView;
    }

    private void increaseTotalSum(Cursor cursor) {
        double sum = cursor.getDouble(cursor.getColumnIndex("sum"));
        totalSpent += sum;
    }

    private ImageButton getDeletionButtonForItem() {
        ImageButton deleteItemButton = new ImageButton(this);
        deleteItemButton.setBackground(this.getResources().getDrawable(R.drawable.delete_button));
        deleteItemButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                try {
                    db.beginTransaction();
                    db.item().deleteById(currentItemId);
                    db.setTransactionSuccessful();
                } catch (Exception e) {} finally {
                    db.endTransaction();
                    finish();
                    startActivity(getIntent());
                }
            }
        });

        int dimensionPixels = helper.getPixelsFromDps(DELETE_BUTTON_DIMENSION);

        ViewGroup.LayoutParams lp = new ViewGroup.MarginLayoutParams(dimensionPixels, dimensionPixels);
        deleteItemButton.setLayoutParams(lp);

        deleteItemButton.setId(View.generateViewId());
        deleteItemButton.setPadding(0,30,0,0);
        return deleteItemButton;
    }

    private void setTotalsText()
    {
        String text = String.format(Locale.getDefault(), getResources().getString(R.string.items_activity_total_sum) + ": %.2f", totalSpent);

        double plannedSum = db.window().selectPlannedSumByWindowsId(windowID);

        if (plannedSum > 0) {
            text += String.format(Locale.getDefault(), "\n" + getResources().getString(R.string.items_activity_planned_sum) +": %.2f", plannedSum);
            text += String.format(Locale.getDefault(), "\n" + getResources().getString(R.string.items_activity_remained_sum) + ": %.2f", (plannedSum - totalSpent));
        }

        TextView totalSumTextView = findViewById(R.id.totals_item_text_view);
        int fontSize = helper.getPixelsFromDps(FONT_SIZE_TOTALS);
        totalSumTextView.setTextSize(fontSize);

        totalSumTextView.setText(text);
    }

    private void setAddItemButton() {
        ImageButton addItemButton = new ImageButton(this);
        LinearLayout imageLayout = new LinearLayout(this);
        int minHeight = helper.getPixelsFromDps(PARTICIPANT_LIST_ELEMENT_MINIMAL_HEIGHT);
        imageLayout.setMinimumHeight(minHeight);
        imageLayout.setGravity(Gravity.CENTER_VERTICAL);

        imageLayout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AddItemDialogFragment dialog = new AddItemDialogFragment();
                setAddButtonListener(dialog);
            }
        });

        addItemButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AddItemDialogFragment dialog = new AddItemDialogFragment();
                setAddButtonListener(dialog);
            }
        });

        int dimensionPixels = helper.getPixelsFromDps(ADD_BUTTON_DIMENSION);

        imageLayout.setBackgroundResource(R.drawable.list_item_border);
        addItemButton.setBackgroundResource(R.drawable.add_button);
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(dimensionPixels, dimensionPixels);
        lp.setMargins(textViewsLeftMargin,0,0,0);
        addItemButton.setLayoutParams(lp);
        imageLayout.addView(addItemButton);
        layoutItems.addView(imageLayout);
    }

    private void setAddParticipantsButton() {
        ImageButton addParticipantButton = new ImageButton(this);
        LinearLayout imageLayout = new LinearLayout(this);

        imageLayout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AddParticipantDialogFragment dialog = new AddParticipantDialogFragment();
                setAddButtonListener(dialog);
            }
        });

        addParticipantButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AddParticipantDialogFragment dialog = new AddParticipantDialogFragment();
                setAddButtonListener(dialog);
            }
        });

        int dimensionPixels = helper.getPixelsFromDps(ADD_BUTTON_DIMENSION);

        imageLayout.setBackgroundResource(R.drawable.list_item_border);
        addParticipantButton.setBackgroundResource(R.drawable.add_button);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(dimensionPixels, dimensionPixels);
        addParticipantButton.setLayoutParams(lp);
        imageLayout.addView(addParticipantButton);
        layoutParticipants.addView(imageLayout);
    }

    private void setAddButtonListener(DialogFragment dialog)
    {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        android.support.v4.app.Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");

        if (prev != null) {
            ft.remove(prev);
        }

        ft.addToBackStack(null);

        Bundle args = new Bundle();
        args.putLong("window_id", windowID);
        args.putString("window_participants", TextUtils.join(",", participants));
        dialog.setArguments(args);
        dialog.show(ft, "dialog");
    }

    private void createParticipantsList()
    {
        int previousParticipantViewId = 0;

        for (int i = 0; i < participants.length; i++) {

            TextView participantView = getParticipantListElementView(participants[i]);

            placeParticipantViewOnList(participantView, previousParticipantViewId);

            previousParticipantViewId = participantView.getId();
        }
    }

    private TextView getParticipantListElementView(String currentParticipant) {
        TextView participantView = new TextView(this);

        String text = getDebtsToOtherParticipants(currentParticipant);

        participantView.setText(text);
        layoutParticipants.addView(participantView);
        participantView.setId(View.generateViewId());

        int minHeight = helper.getPixelsFromDps(PARTICIPANT_LIST_ELEMENT_MINIMAL_HEIGHT);
        float textSize = (float) helper.getPixelsFromDps(FONT_SIZE_NORMAL);
        participantView.setMinHeight(minHeight);
        participantView.setBackgroundResource(R.drawable.list_item_border);
        participantView.setTextSize(textSize);
        participantView.setGravity(Gravity.CENTER_VERTICAL);

        return participantView;
    }

    private String getDebtsToOtherParticipants(String currentParticipant)
    {
        String text = currentParticipant;

        for (int j = 0; j < participants.length; j++) {
            if (currentParticipant.equals(participants[j])) {
                continue;
            }

            text += getPersonalDebtString(currentParticipant, participants[j]);
        }

        return text;
    }

    private String getPersonalDebtString(String firstParticipant, String secondParticipant)
    {
        String text = "";

        double ownedDebt = db.debt().selectPersonalDebt(firstParticipant, secondParticipant, windowID);
        double ownDebt = db.debt().selectPersonalDebt(secondParticipant, firstParticipant, windowID);

        //TODO cache this shit
        double debt = ownDebt - ownedDebt;

        if (debt > 0) {
            String text1 = getResources().getString(R.string.items_activity_participant_debt_text1);
            String text2 = getResources().getString(R.string.items_activity_participant_debt_text2);
            text = String.format(Locale.getDefault(), "\n%s %.2f %s %s", text1, debt, text2, secondParticipant);
        }

        return text;
    }

    private void placeParticipantViewOnList(TextView participantView, int previousParticipantViewId) {
        // STUB
    }

    public void setActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setTitle(getResources().getString(R.string.items_title));
        actionBar.show();
    }
}