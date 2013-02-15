package net.nologin.meep.ca;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.*;
import greendroid.app.GDActivity;
import greendroid.widget.ActionBar;
import greendroid.widget.ActionBarItem;
import greendroid.widget.LoaderActionBarItem;
import net.nologin.meep.ca.util.Utils;
import net.nologin.meep.ca.view.WolframCAView;

import java.util.Random;

import static net.nologin.meep.ca.util.Utils.log;

public class MainActivity extends GDActivity {


    private final Handler mHandler = new Handler();

    private WolframCAView caView;

    private static int[] RULES = {
            30, 54, 60, 62, 90, 94,
            102, 110, 122, 126,
            150, 158, 182, 188,
            190, 220, 222, 250
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setActionBarContentView(R.layout.main);
        // was: setContentView(R.layout.main);

        getActionBar().setType(ActionBar.Type.Empty);


        // TODO: fix
        caView = (WolframCAView) findViewById(R.id.caView);
        caView.setupForRule(110);


        addActionBarItem(ActionBarItem.Type.Refresh, R.id.action_bar_refresh);


        // for a custom icon
//        addActionBarItem(getActionBar()
//                .newActionBarItem(NormalActionBarItem.class)
//                .setDrawable(R.drawable.ic_title_export)
//                .setContentDescription(R.string.gd_export), R.id.action_bar_export);

        addActionBarItem(ActionBarItem.Type.Settings, R.id.actionbar_settings);


        // is this right?
        TextView tv = (TextView) this.findViewById(R.id.gd_action_bar_title);
        tv.setText("Wolfram CA");



        final String list[] = new String[RULES.length];
        for (int i = 0; i < RULES.length; i++) {
            list[i] = "CA Rule " + RULES[i];
        }

        /*
        final Spinner spinner = (Spinner) findViewById(R.id.caSpinner);

        ArrayAdapter<String> caValueAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        caValueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(caValueAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                log("Got item id " + id + " at position " + position);
                caView.setupForRule(RULES[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        findViewById(R.id.caRulePrev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                log("prev from item " + spinner.getSelectedItemPosition());
                int pos = spinner.getSelectedItemPosition();
                spinner.setSelection(pos-1 < 0 ? RULES.length-1 : pos-1);

            }
        });

        findViewById(R.id.caRuleNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                log("next from item " + spinner.getSelectedItemPosition());
                int pos = spinner.getSelectedItemPosition();
                spinner.setSelection(pos+1 >= RULES.length ? 0 : pos+1);
            }
        });
           */

    }


    @Override
    public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {

        switch (item.getItemId()) {

            case R.id.actionbar_settings:
                Toast.makeText(this, "Settings!" , Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case R.id.action_bar_refresh:
                final LoaderActionBarItem loaderItem = (LoaderActionBarItem) item;
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        loaderItem.setLoading(false);
                    }
                }, 2000);
                Toast.makeText(this, "Refresh done", Toast.LENGTH_SHORT).show();
                break;

            default:
                return super.onHandleActionBarItemClick(item, position);
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        caView.setDisplayDebug(Utils.Prefs.getPrefDebugEnabled(this));


    }
}

