/*
 *    WolframCA - an android application to view 1-dimensional cellular automata (CA)
 *    Copyright 2013 Barry O'Neill (http://meep.nologin.net/)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
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
import net.nologin.meep.ca.view.WolframCAView;

/**
 * Main WolframCA activity.  This consists of the custom {@link WolframCAView} taking up the full display, with a
 * few menu options being rendered by the {@link SherlockActivity} superclass.
 * <br/><br/>
 * <b>This activity needs the <a href="http://actionbarsherlock.com/">ActionBarSherlock</a> library set as a
 * project dependency!</b>
 * <br/><br/>
 * The following menu options are implemented here:
 * <ul>
 * <li><i>Change Rule</i>: A dialog to choose a different {@link net.nologin.meep.ca.model.WolframRuleTable rule}</li>
 * <li><i>Change Zoom</i>: A dialog to choose how many pixels wide to render each CA cell</li>
 * <li><i>Back To Top</i>: Scroll the {@link WolframCAView} so tile 0,0 is top center in the surface</li>
 * <li><i>Settings</i>: Calls the {@link SettingsActivity}</li>
 * </ul>
 */
public class MainActivity extends SherlockActivity {

    // start on a more interesting rule
    private static final int DEFAULT_RULE = 110;

    private WolframCAView caView;

    // local variables for the dialogs before the user commits their choice
    private int ruleDialog_rule;
    private int zoomDialog_zoom;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        caView = (WolframCAView) findViewById(R.id.caView);
        caView.setupForRule(DEFAULT_RULE);

        // set "Rule #number" descriptive text in the actionbarsherlock actionbar
        getSherlock().getActionBar().setTitle(
                getResources().getString(R.string.actionbar_ruleSummary,
                        caView.getCurrentRule()));

    }

    @Override
    protected void onResume() {
        super.onResume();

        // not in onCreate as we want this to take effect on return from settings
        caView.setDebugEnabled(WolframUtils.Prefs.getPrefDebugEnabled(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.actionbar_itemlist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            // "Change Rule" dialog
            case R.id.actionbar_changerule:
                createAndShowRuleDialog();
                return true;

            // "Change Zoom" dialog
            case R.id.actionbar_changezoom:
                createAndShowZoomDialog();
                return true;

            // "Back to Top"
            case R.id.actionbar_backtotop:
                caView.moveToOriginTile();
                return true;

            // "Settings"
            case R.id.actionbar_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* Setup and display rule change dialog.  Seek bar with values between 0-255, summary of selected rule and
     * 'prev' and 'next' buttons for finer control of rule selection. */
    private void createAndShowRuleDialog() {

        ruleDialog_rule = caView.getCurrentRule();

        View layout = getLayoutInflater().inflate(R.layout.rule_select, null);

        // set the summary of the selected rule, eg "Rule 110"
        final TextView ruleSummaryTxt = (TextView) layout.findViewById(R.id.ruleDialog_ruleSummaryTxt);
        ruleSummaryTxt.setText(getResources().getString(R.string.ruledialog_ruleSummary, ruleDialog_rule));

        final SeekBar ruleSeek = (SeekBar) layout.findViewById(R.id.ruleDialog_ruleSlider);
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


        layout.findViewById(R.id.ruleDialog_butRulePrev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ruleDialog_rule = ruleDialog_rule <= 0 ? 255 : ruleDialog_rule - 1;
                ruleSeek.setProgress(ruleDialog_rule); // seekbar listener will update edittext value
            }
        });

        layout.findViewById(R.id.ruleDialog_butRuleNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ruleDialog_rule = ruleDialog_rule >= 255 ? 0 : ruleDialog_rule + 1;
                ruleSeek.setProgress(ruleDialog_rule); // seekbar listener will update edittext value
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.ruledialog_title))
                .setCancelable(true)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        getSherlock().getActionBar().setTitle(getResources().getString(R.string.actionbar_ruleSummary, ruleDialog_rule));
                        caView.setupForRule(ruleDialog_rule);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });


        builder.create().show();

    }

    /* Setup and display zoom change dialog.  Seek bar with values between 1-16 (in steps of 2), summary of selected
     * zoom level and 'prev' and 'next' buttons for finer control of zoom selection. */
    private void createAndShowZoomDialog() {

        zoomDialog_zoom = caView.getCurrentPxPerCell();

        View layout = getLayoutInflater().inflate(R.layout.zoom_select, null);

        // set the summary of the selected zoom level, eg "4px per cell"
        final TextView zoomSummaryTxt = (TextView) layout.findViewById(R.id.zoomDialog_zoomSummaryTxt);
        zoomSummaryTxt.setText(getResources().getString(R.string.zoomdialog_zoomSummary, zoomDialog_zoom));

        final SeekBar zoomSeek = (SeekBar) layout.findViewById(R.id.zoomDialog_zoomSlider);
        zoomSeek.setMax(16);
        zoomSeek.setProgress(zoomDialog_zoom);
        zoomSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                progress = WolframUtils.sanitizeZoom(progress);
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


        layout.findViewById(R.id.zoomDialog_butZoomPrev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomDialog_zoom = WolframUtils.sanitizeZoom(zoomDialog_zoom - 2);
                zoomSeek.setProgress(zoomDialog_zoom); // seekbar listener will update edittext value
            }
        });

        layout.findViewById(R.id.zoomDialog_butZoomNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomDialog_zoom = WolframUtils.sanitizeZoom(zoomDialog_zoom + 2);
                zoomSeek.setProgress(zoomDialog_zoom); // seekbar listener will update edittext value
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.zoomdialog_title))
                .setCancelable(true)
                .setView(layout)
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


        builder.create().show();

    }


}

