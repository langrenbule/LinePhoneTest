package com.keytop.linephonetest;

import android.app.Application;

import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreFactory;

/**
 * Application
 * Created by fengwenhua on 2016/11/23.
 */

public class LineApplication extends Application {
    private LinphoneCore mLinphoneCore;



    @Override
    public void onCreate() {
        super.onCreate();

    }


}
