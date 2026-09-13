package com.keeperrl.offlinemapsforwearos;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class GpxImportListenerService extends WearableListenerService {

    private static final String TAG = "GpxImportListener";
    private static final String PATH_PREFIX = "/gpx-import/";

    public static final String ACTION_GPX_IMPORTED = "com.keeperrl.offlinemapsforwearos.GPX_IMPORTED";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_SUCCESS = "success";

    @Override
    public void onChannelOpened(ChannelClient.Channel channel) {
        String path = channel.getPath();
        if (path == null || !path.startsWith(PATH_PREFIX))
            return;

        String name = Uri.decode(path.substring(PATH_PREFIX.length()));
        if (!name.toLowerCase().endsWith(".gpx"))
            name = name + ".gpx";

        ChannelClient channelClient = Wearable.getChannelClient(this);
        boolean success = false;
        try {
            InputStream in = Tasks.await(channelClient.getInputStream(channel), 15, TimeUnit.SECONDS);
            File target = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), name);
            FileOutputStream out = new FileOutputStream(target);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1)
                out.write(buffer, 0, read);
            out.flush();
            out.close();
            in.close();
            success = true;
            Log.i(TAG, "Saved incoming GPX track: " + target.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to receive GPX track", e);
        } finally {
            try {
                Tasks.await(channelClient.close(channel), 5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }

        Intent broadcast = new Intent(ACTION_GPX_IMPORTED);
        broadcast.setPackage(getPackageName());
        broadcast.putExtra(EXTRA_NAME, name);
        broadcast.putExtra(EXTRA_SUCCESS, success);
        sendBroadcast(broadcast);
    }
}
