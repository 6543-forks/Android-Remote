/* This file is part of the Android Clementine Remote.
 * Copyright (C) 2014, Andreas Muttscheller <asfa194@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package de.qspool.clementineremote.backend.mediasession;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.session.MediaSession;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import de.qspool.clementineremote.App;
import de.qspool.clementineremote.R;
import de.qspool.clementineremote.backend.Clementine;
import de.qspool.clementineremote.backend.player.MySong;
import de.qspool.clementineremote.backend.receivers.ClementineBroadcastReceiver;
import de.qspool.clementineremote.utils.Utilities;

public class ClementineMediaSessionNotification extends ClementineMediaSession {

    public final static int NOTIFIFCATION_ID = 78923748;

    public final static String EXTRA_NOTIFICATION_ID = "NotificationID";

    private NotificationManager mNotificationManager;

    private Notification.Builder mNotificationBuilder;

    private RemoteViews mNotificationView;

    private int mNotificationWidth;

    private int mNotificationHeight;

    private boolean mTurnColor;

    public ClementineMediaSessionNotification(Context context) {
        super(context);
        mNotificationManager = (NotificationManager) mContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
        SharedPreferences colorPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mTurnColor = colorPreferences.getBoolean("pref_noti_color", false);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void registerSession() {
        Resources res = mContext.getResources();
        mNotificationHeight = (int) res
                .getDimension(android.R.dimen.notification_large_icon_height);
        mNotificationWidth = (int) res.getDimension(android.R.dimen.notification_large_icon_width);

        mNotificationBuilder = new Notification.Builder(mContext)
                .setSmallIcon(R.drawable.notification)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mNotificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        mNotificationBuilder.setContentIntent(Utilities.getClementineRemotePendingIntent(mContext));

        mNotificationView = new RemoteViews(mContext.getPackageName(), R.layout.notification_small);
        if (mTurnColor) {
            mNotificationView.setInt(R.id.noti, "setBackgroundColor", Color.TRANSPARENT);
            mNotificationView.setImageViewResource(R.id.noti_play_pause, R.drawable.ic_media_play);
            mNotificationView.setImageViewResource(R.id.noti_next, R.drawable.ic_media_next);
        }
        mNotificationBuilder.setContent(mNotificationView);
    }

    @Override
    public void unregisterSession() {
        mNotificationManager.cancel(NOTIFIFCATION_ID);
    }

    @Override
    public void updateSession() {
        MySong song = App.Clementine.getCurrentSong();
        if (song != null) {
            Bitmap scaledArt = Bitmap.createScaledBitmap(song.getArt(),
                    mNotificationWidth,
                    mNotificationHeight,
                    false);

            mNotificationView.setImageViewBitmap(R.id.noti_icon, scaledArt);
            mNotificationView.setTextViewText(R.id.noti_title, song.getTitle());
            mNotificationView.setTextViewText(R.id.noti_subtitle, song.getArtist() +
                    " / " +
                    song.getAlbum());
        } else {
            mNotificationView.setTextViewText(R.id.noti_title, mContext.getString(R.string.app_name));
            mNotificationView.setTextViewText(R.id.noti_subtitle, mContext.getString(R.string.player_nosong));
        }

        // Play or pause?
        Intent intentPlayPause = new Intent(mContext, ClementineBroadcastReceiver.class);
        Intent intentNext = new Intent(mContext, ClementineBroadcastReceiver.class);
        intentNext.setAction(ClementineBroadcastReceiver.NEXT);

        if (App.Clementine.getState() == Clementine.State.PLAY) {
            mNotificationView.setImageViewResource(R.id.noti_play_pause,
                    mTurnColor ? R.drawable.ic_media_pause : R.drawable.ab_media_pause);
            intentPlayPause.setAction(ClementineBroadcastReceiver.PAUSE);
        } else {
            mNotificationView.setImageViewResource(R.id.noti_play_pause,
                    mTurnColor ? R.drawable.ic_media_play : R.drawable.ab_media_play);
            intentPlayPause.setAction(ClementineBroadcastReceiver.PLAY);
        }
        mNotificationView.setOnClickPendingIntent(R.id.noti_play_pause,
                PendingIntent
                        .getBroadcast(mContext, 0, intentPlayPause,
                                PendingIntent.FLAG_ONE_SHOT));
        mNotificationView.setOnClickPendingIntent(R.id.noti_next,
                PendingIntent
                        .getBroadcast(mContext, 0, intentNext,
                                PendingIntent.FLAG_ONE_SHOT));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            notifyV15();
        } else {
            mNotificationManager.notify(NOTIFIFCATION_ID, mNotificationBuilder.build());
        }
    }

    @SuppressWarnings("deprecation")
    private void notifyV15() {
        //noinspection deprecation
        mNotificationManager.notify(NOTIFIFCATION_ID, mNotificationBuilder.getNotification());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setMediaSessionCompat(MediaSession mediaSession) {
        if (mediaSession == null) {
            return;
        }

        //mNotificationBuilder.setStyle(new Notification.MediaStyle()
        //        .setMediaSession(mediaSession.getSessionToken()));
    }
}
