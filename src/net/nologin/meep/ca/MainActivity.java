package net.nologin.meep.ca;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import net.nologin.meep.ca.view.WolframCAView;

import static net.nologin.meep.ca.util.Utils.log;

public class MainActivity extends Activity {


    private static int[] RULES = {
            30, 54, 60, 62, 90, 94,
            102, 110, 122, 126,
            150, 158, 182, 188,
            190, 220, 222, 250
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);


        final String list[] = new String[RULES.length];
        for (int i = 0; i < RULES.length; i++) {
            list[i] = "CA Rule " + RULES[i];
        }

        final WolframCAView caView = (WolframCAView) findViewById(R.id.caView);
        final Spinner spinner = (Spinner) findViewById(R.id.caSpinner);

        ArrayAdapter<String> caValueAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        caValueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(caValueAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                log("Got item id " + id + " at position " + position);
                caView.changeRule(RULES[position]);
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


    }


}

