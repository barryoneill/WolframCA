package net.nologin.meep.ca;

import android.content.Intent;
import android.net.Uri;
import greendroid.app.GDApplication;


public class CAApplication extends GDApplication {

    @Override
    public Class<?> getHomeActivityClass() {
        return MainActivity.class;
    }

    @Override
    public Intent getMainApplicationIntent() {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.about_githubpage_url)));
    }

}
