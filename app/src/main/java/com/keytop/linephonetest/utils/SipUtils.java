package com.keytop.linephonetest.utils;

import android.content.Context;

import org.linphone.core.LinphoneCall;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 软通话工具类
 * Created by fengwenhua on 2016/11/23.
 */

public class SipUtils {

    public static void copyIfNotExist(Context context, int ressourceId, String target) throws IOException {
        File lFileToCopy = new File(target);
        if (!lFileToCopy.exists()) {
            copyFromPackage(context, ressourceId, lFileToCopy.getName());
        }
    }

    public static void copyFromPackage(Context context, int ressourceId, String target) throws IOException {
        FileOutputStream lOutputStream = context.openFileOutput(target, 0);
        InputStream lInputStream = context.getResources().openRawResource(ressourceId);
        int readByte;
        byte[] buff = new byte[8048];
        while ((readByte = lInputStream.read(buff)) != -1) {
            lOutputStream.write(buff, 0, readByte);
        }
        lOutputStream.flush();
        lOutputStream.close();
        lInputStream.close();
    }

    public static boolean isCallRunning(LinphoneCall call) {
        if (call == null) {
            return false;
        }

        LinphoneCall.State state = call.getState();

        return state == LinphoneCall.State.Connected ||
                state == LinphoneCall.State.CallUpdating ||
                state == LinphoneCall.State.CallUpdatedByRemote ||
                state == LinphoneCall.State.StreamsRunning ||
                state == LinphoneCall.State.Resuming;
    }

    public static boolean isCallEstablished(LinphoneCall call) {
        if (call == null) {
            return false;
        }

        LinphoneCall.State state = call.getState();

        return isCallRunning(call) ||
                state == LinphoneCall.State.Paused ||
                state == LinphoneCall.State.PausedByRemote ||
                state == LinphoneCall.State.Pausing;
    }
}
