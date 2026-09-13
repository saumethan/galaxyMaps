package com.keeperrl.offlinemapsforwearos.mobile;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ShareToWatchActivity extends Activity {

    private static final String TAG = "ShareToWatch";
    private static final String CHANNEL_PATH_PREFIX = "/gpx-import/";

    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_to_watch);
        statusView = (TextView) findViewById(R.id.statusText);

        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.hasExtra(Intent.EXTRA_STREAM)) {
            Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            sendToWatch(uri);
        } else {
            showConnectedWatchStatus();
        }
    }

    private void showConnectedWatchStatus() {
        statusView.setText("Checking for a paired watch...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String message;
                try {
                    List<Node> nodes = Tasks.await(
                            Wearable.getNodeClient(ShareToWatchActivity.this).getConnectedNodes(), 10, TimeUnit.SECONDS);
                    if (nodes.isEmpty()) {
                        message = "No paired watch found.\n\nTo send a GPX track, use \"Share\" on a .gpx file " +
                                "(from Files, Drive, email, etc.) and choose " + getString(R.string.app_name) + ".";
                    } else {
                        message = "Connected to: " + nodes.get(0).getDisplayName() +
                                "\n\nTo send a GPX track, use \"Share\" on a .gpx file (from Files, Drive, email, etc.) " +
                                "and choose " + getString(R.string.app_name) + ".";
                    }
                } catch (Exception e) {
                    message = "Could not check watch connection: " + e.getMessage();
                }
                final String finalMessage = message;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusView.setText(finalMessage);
                    }
                });
            }
        }).start();
    }

    private void sendToWatch(final Uri uri) {
        if (uri == null) {
            statusView.setText("No file was shared.");
            return;
        }
        statusView.setText("Sending to watch...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String name = queryDisplayName(uri);
                    if (name == null)
                        name = "imported.gpx";
                    if (!name.toLowerCase().endsWith(".gpx"))
                        name = name + ".gpx";

                    List<Node> nodes = Tasks.await(
                            Wearable.getNodeClient(ShareToWatchActivity.this).getConnectedNodes(), 10, TimeUnit.SECONDS);
                    if (nodes.isEmpty())
                        throw new IOException("No paired watch is connected");
                    Node node = nodes.get(0);

                    ChannelClient channelClient = Wearable.getChannelClient(ShareToWatchActivity.this);
                    ChannelClient.Channel channel = Tasks.await(
                            channelClient.openChannel(node.getId(), CHANNEL_PATH_PREFIX + Uri.encode(name)), 15, TimeUnit.SECONDS);
                    OutputStream out = Tasks.await(channelClient.getOutputStream(channel), 15, TimeUnit.SECONDS);
                    InputStream in = getContentResolver().openInputStream(uri);
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1)
                        out.write(buffer, 0, read);
                    out.flush();
                    out.close();
                    in.close();
                    Tasks.await(channelClient.close(channel), 5, TimeUnit.SECONDS);

                    final String finalName = name;
                    final String nodeName = node.getDisplayName();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusView.setText("Sent " + finalName + " to " + nodeName);
                            Toast.makeText(ShareToWatchActivity.this, "Sent to watch", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (final Exception e) {
                    Log.e(TAG, "Failed to send GPX to watch", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusView.setText("Failed to send file: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private String queryDisplayName(Uri uri) {
        if (!"content".equals(uri.getScheme()))
            return new File(uri.getPath()).getName();
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(uri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0)
                    return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
}
