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
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.View;
import android.widget.TextView;

/**
 * Settings activity.  At the moment, there's just a debug option and an about dialog.
 */
public class SettingsActivity extends PreferenceActivity {

    // key for debug preference
    public static final String PREF_KEY_SHOW_DEBUG = "pref_key_show_debug";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* This way is deprecated, but exists because this app supports 2.3.3 devices.  When/if I
           get time, I should add a fragment-based prefs screen for newer devices as well. */
        addPreferencesFromResource(R.xml.preferences);


        Preference dialogPreference = (Preference) getPreferenceScreen().findPreference("pref_key_about");
        dialogPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                Context ctx = SettingsActivity.this;

                View inflatedDialog = getLayoutInflater().inflate(R.layout.about_wolframca, null);

                TextView tv = (TextView)inflatedDialog.findViewById(R.id.about_version);
                String verName = WolframUtils.getAppVersionName(ctx);
                tv.setText(ctx.getString(R.string.aboutdialog_versionfmt,verName));

                new AlertDialog.Builder(ctx)
                        .setCancelable(true)
                        .setView(inflatedDialog)
                        .create().show();

                return true;
            }
        });

    }




}