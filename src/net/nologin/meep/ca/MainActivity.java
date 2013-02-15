package net.nologin.meep.ca;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.*;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import net.nologin.meep.ca.util.Utils;
import net.nologin.meep.ca.view.WolframCAView;

public class MainActivity extends SherlockActivity {


    private final Handler mHandler = new Handler();

    private WolframCAView caView;

    private static int[] RULES = {
            30, 54, 60, 62, 90, 94,
            102, 110, 122, 126,
            150, 158, 182, 188,
            190, 220, 222, 250
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Used to put dark icons on light action bar
        //boolean isLight = SampleList.THEME == R.style.Theme_Sherlock_Light;

        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.actionbar_itemlist, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.actionbar_changerule:
                Toast.makeText(this,"changin' the rule", Toast.LENGTH_LONG).show();
                return true;
            case R.id.actionbar_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);


        // TODO: fix
        caView = (WolframCAView) findViewById(R.id.caView);
        caView.setupForRule(110);


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
    protected void onResume() {
        super.onResume();

        caView.setDisplayDebug(Utils.Prefs.getPrefDebugEnabled(this));


    }
}

