package net.nologin.meep.ca;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import net.nologin.meep.ca.view.TiledBitmapView;
import net.nologin.meep.ca.view.WolframCAView;


public class MainActivity extends Activity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        final String list[] = new String[256];
        for (int i = 0; i < 256; i++) {
            list[i] = "CA Rule " + i;
        }

        final TiledBitmapView tbv = (TiledBitmapView)findViewById(R.id.caView);
        final Spinner spinner = (Spinner) findViewById(R.id.caSpinner);

        ArrayAdapter<String> caValueAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        caValueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(caValueAdapter);


        final ImageButton caRulePrevBut = (ImageButton) findViewById(R.id.caRulePrev);
        caRulePrevBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

             tbv.togglePause();

            }
        });




    }


}

