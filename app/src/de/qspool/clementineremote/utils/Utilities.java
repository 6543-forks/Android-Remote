package de.qspool.clementineremote.utils;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.UnknownHostException;

import de.qspool.clementineremote.App;
import de.qspool.clementineremote.R;
import de.qspool.clementineremote.backend.mediasession.ClementineMediaSessionNotification;
import de.qspool.clementineremote.ui.MainActivity;

public class Utilities {

    @SuppressLint("DefaultLocale")
    public static String PrettyTime(int seconds) {
        // last.fm sometimes gets the track length wrong, so you end up with
        // negative times.
        seconds = Math.abs(seconds);

        int hours = seconds / (60 * 60);
        int minutes = (seconds / 60) % 60;
        seconds %= 60;

        String ret = "";
        if (hours > 0) {
            ret = String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            ret = String.format("%d:%02d", minutes, seconds);
        }

        return ret;
    }

    /**
     * Show a simple designed message dialog
     *
     * @param context In which context will the dialog be displayed?
     * @param title   The resource id of the message dialog title.
     * @param message The resource id of the message
     * @return The Dialog object
     */
    public static Dialog ShowMessageDialog(Context context, int title, int message) {
        return ShowMessageDialog(context, context.getString(title), context.getString(message),
                false);
    }

    /**
     * Show a simple designed message dialog
     *
     * @param context In which context will the dialog be displayed?
     * @param title   The resource id of the message dialog title.
     * @param message The resource id of the message
     * @return The Dialog object
     */
    public static Dialog ShowHtmlMessageDialog(Context context, int title, int message) {
        return ShowMessageDialog(context, context.getString(title), context.getString(message),
                true);
    }

    /**
     * Show a simple designed message dialog
     *
     * @param context In which context will the dialog be displayed?
     * @param title   The message string dialog title.
     * @param message The string of the message
     * @return The Dialog object
     */
    public static Dialog ShowMessageDialog(Context context, String title, String message,
            boolean hasHtml) {
        final Dialog errorDialog = new Dialog(context, R.style.Dialog_Transparent);
        errorDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        errorDialog.setContentView(R.layout.dialog_message);

        // Set the Views
        final TextView tvTitle = (TextView) errorDialog.findViewById(R.id.tvTitle);
        final TextView tvMessage = (TextView) errorDialog.findViewById(R.id.tvMessage);
        tvTitle.setText(title);
        if (hasHtml) {
            tvMessage.setText(Html.fromHtml(message));
        } else {
            tvMessage.setText(message);
        }
        tvMessage.setMovementMethod(LinkMovementMethod.getInstance());

        Button connectButton = (Button) errorDialog.findViewById(R.id.btnClose);
        connectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                errorDialog.cancel();
            }
        });

        errorDialog.show();

        return errorDialog;
    }

    public static byte[] ToIPByteArray(int addr) {
        return new byte[]{(byte) addr, (byte) (addr >>> 8), (byte) (addr >>> 16),
                (byte) (addr >>> 24)};
    }

    public static InetAddress ToInetAddress(int addr) {
        try {
            return InetAddress.getByAddress(ToIPByteArray(addr));
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * Get the free space on the external storage device (like sd card)
     *
     * @return The free space in byte
     */
    @SuppressWarnings("deprecation")
    public static double getFreeSpaceExternal() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return (double) stat.getAvailableBlocks() * (double) stat.getBlockSize();
        } else {
            return (double) stat.getAvailableBlocksLong() * (double) stat.getBlockSizeLong();
        }
    }

    /**
     * Get the free space on the internal storage device
     *
     * @return The free space in byte
     */
    @SuppressWarnings("deprecation")
    public static double getFreeSpaceInternal() {
        StatFs stat = new StatFs(App.getApp().getFilesDir().getPath());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return (double) stat.getAvailableBlocks() * (double) stat.getBlockSize();
        } else {
            return (double) stat.getAvailableBlocksLong() * (double) stat.getBlockSizeLong();
        }
    }

    /**
     * Is the device connceted to a wifi network?
     *
     * @return true if connected to a wifi network
     */
    public static boolean onWifi() {
        ConnectivityManager connManager = (ConnectivityManager) App.getApp()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return mWifi.isConnected();
    }

    /**
     * Convert bytes in a human readable format.
     *
     * @param bytes The byte count
     * @param iec   false for KB, false for KiB
     * @return The human readable file size
     */
    public static String humanReadableBytes(long bytes, boolean iec) {
        // Are we using xB or xiB?
        int byteUnit = iec ? 1024 : 1000;
        float newBytes = bytes;
        int exp = 0;

        // Calculate the file size in the best readable way
        while (newBytes > byteUnit) {
            newBytes = newBytes / byteUnit;
            exp++;
        }

        // What prefix do we have to use?
        String prefix = "";
        if (exp > 0) {
            prefix = (iec ? " KMGTPE" : " kMGTPE").charAt(exp) + ((iec) ? "i" : "");
        }

        // Return a human readable String
        return String.format("%.2f %sB", newBytes, prefix);
    }

    /**
     * Checks if Clementine Remote is connected to an instance
     * of Clementine.
     *
     * @return true if connected, false otherwise
     */
    public static boolean isRemoteConnected() {
        return App.ClementineConnection != null && App.ClementineConnection.isConnected();
    }

    /**
     * This method removes all illegal characters in a file name.
     *
     * @param str The string containing a file or folder name
     * @return A string that is a vaild file name
     */
    public static String removeInvalidFileCharacters(String str) {
        String illegal = "[\\\\~#%&*{}/:<>?|\\\"-]";
        return str.replaceAll(illegal, "");
    }

    public static PendingIntent getClementineRemotePendingIntent(Context context) {
        // Set the result intent
        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        resultIntent.putExtra(ClementineMediaSessionNotification.EXTRA_NOTIFICATION_ID, -1);

        // Create a TaskStack, so the app navigates correctly backwards
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
