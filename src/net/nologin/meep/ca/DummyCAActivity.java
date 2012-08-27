package net.nologin.meep.ca;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import net.nologin.meep.ca.view.WolframCAView;


public class DummyCAActivity extends Activity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        final String list[] = new String[256];
        for (int i = 0; i < 256; i++) {
            list[i] = "CA Rule " + i;
        }

        final Spinner spinner = (Spinner) findViewById(R.id.caSpinner);
        final WolframCAView caView = (WolframCAView) findViewById(R.id.caView);


        ArrayAdapter<String> caValueAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        caValueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(caValueAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                caView.changeRuleNo((int) adapterView.getSelectedItemId());
            }

            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinner.setSelection(90);


        final ImageButton caRulePrevBut = (ImageButton) findViewById(R.id.caRulePrev);
        caRulePrevBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int curPos = spinner.getSelectedItemPosition();
                if(curPos > 0){
                    spinner.setSelection(curPos-1);
                }
            }
        });

        final ImageButton caRuleNextBut = (ImageButton) findViewById(R.id.caRuleNext);
        caRuleNextBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int curPos = spinner.getSelectedItemPosition();
                if(curPos < list.length -1){
                    spinner.setSelection(curPos+1);
                }
            }
        });

    }


}

