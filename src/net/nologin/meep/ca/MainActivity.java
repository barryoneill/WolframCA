package net.nologin.meep.ca;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import net.nologin.meep.ca.util.Utils;
import net.nologin.meep.ca.view.WolframCAView;

public class MainActivity extends SherlockActivity {


    private WolframCAView caView;

    private int ruleDialog_rule;

    private int zoomDialog_zoom;


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

                ruleDialog_rule = caView.getCurrentRule();

                View ruleDialogLayout = getLayoutInflater().inflate(R.layout.rule_select, null);

                // set the summary of the selected rule, eg "Rule 110"
                final TextView ruleSummaryTxt = (TextView) ruleDialogLayout.findViewById(R.id.ruleDialog_ruleSummaryTxt);
                ruleSummaryTxt.setText(getResources().getString(R.string.ruledialog_ruleSummary, ruleDialog_rule));

                final SeekBar ruleSeek = (SeekBar)ruleDialogLayout.findViewById(R.id.ruleDialog_ruleSlider);
                ruleSeek.setMax(255);
                ruleSeek.setProgress(ruleDialog_rule);
                ruleSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        ruleDialog_rule = progress;
                        ruleSummaryTxt.setText(getResources().getString(R.string.ruledialog_ruleSummary, ruleDialog_rule));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });


                ruleDialogLayout.findViewById(R.id.ruleDialog_butRulePrev).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ruleDialog_rule = ruleDialog_rule <= 0 ? 255: ruleDialog_rule-1;
                        ruleSeek.setProgress(ruleDialog_rule); // seekbar listener will update edittext value
                    }
                });

                ruleDialogLayout.findViewById(R.id.ruleDialog_butRuleNext).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ruleDialog_rule = ruleDialog_rule >= 255 ? 0: ruleDialog_rule+1;
                        ruleSeek.setProgress(ruleDialog_rule); // seekbar listener will update edittext value
                    }
                });

                AlertDialog.Builder ruleDiaBuilder = new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.ruledialog_title))
                        .setCancelable(true)
                        .setView(ruleDialogLayout)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                getSherlock().getActionBar().setTitle(getResources().getString(R.string.actionbar_ruleSummary,ruleDialog_rule));
                                caView.setupForRule(ruleDialog_rule);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                // create alert dialog
                AlertDialog ruleDialog = ruleDiaBuilder.create();

                // show it
                ruleDialog.show();

                return true;

            case R.id.actionbar_changezoom:

                zoomDialog_zoom = caView.getCurrentPxPerCell();

                View zoomDialogLayout = getLayoutInflater().inflate(R.layout.zoom_select, null);

                // set the summary of the selected rule, eg "Rule 110"
                final TextView zoomSummaryTxt = (TextView) zoomDialogLayout.findViewById(R.id.zoomDialog_zoomSummaryTxt);
                zoomSummaryTxt.setText(getResources().getString(R.string.zoomdialog_zoomSummary, zoomDialog_zoom));

                final SeekBar zoomSeek = (SeekBar)zoomDialogLayout.findViewById(R.id.zoomDialog_zoomSlider);
                zoomSeek.setMax(16);
                zoomSeek.setProgress(zoomDialog_zoom);
                zoomSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        progress = Utils.roundZoomLevel(progress);
                        seekBar.setProgress(progress);

                        zoomDialog_zoom = progress;
                        zoomSummaryTxt.setText(getResources().getString(R.string.zoomdialog_zoomSummary, zoomDialog_zoom));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });


                zoomDialogLayout.findViewById(R.id.zoomDialog_butZoomPrev).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        zoomDialog_zoom = Utils.roundZoomLevel(zoomDialog_zoom-2);
                        zoomSeek.setProgress(zoomDialog_zoom); // seekbar listener will update edittext value
                    }
                });

                zoomDialogLayout.findViewById(R.id.zoomDialog_butZoomNext).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        zoomDialog_zoom = Utils.roundZoomLevel(zoomDialog_zoom + 2);
                        zoomSeek.setProgress(zoomDialog_zoom); // seekbar listener will update edittext value
                    }
                });

                AlertDialog.Builder zoomDiaBuilder = new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.zoomdialog_title))
                        .setCancelable(true)
                        .setView(zoomDialogLayout)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                caView.setupForZoom(zoomDialog_zoom);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                // create alert dialog
                AlertDialog zoomDialog = zoomDiaBuilder.create();

                // show it
                zoomDialog.show();

                return true;

            case R.id.actionbar_backtostart:
                caView.moveToOriginTile();
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

        caView = (WolframCAView) findViewById(R.id.caView);
        caView.setDebugEnabled(Utils.Prefs.getPrefDebugEnabled(this));

        caView.setupForRule(110);
        getSherlock().getActionBar().setTitle(getResources().getString(R.string.actionbar_ruleSummary,110));

    }


    @Override
    protected void onResume() {
        super.onResume();

        caView.setDebugEnabled(Utils.Prefs.getPrefDebugEnabled(this));


    }
}

