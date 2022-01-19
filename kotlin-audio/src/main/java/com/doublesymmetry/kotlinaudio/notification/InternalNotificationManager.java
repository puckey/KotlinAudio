package com.doublesymmetry.kotlinaudio.notification;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import java.util.Arrays;
import java.util.List;

public class InternalNotificationManager extends PlayerNotificationManager {
    private Boolean useStopActionInCompactView;

    protected InternalNotificationManager(Context context, String channelId, int notificationId, MediaDescriptionAdapter mediaDescriptionAdapter, @Nullable NotificationListener notificationListener, @Nullable PrimaryActionReceiver primaryActionReceiver, @Nullable CustomActionReceiver customActionReceiver, int smallIconResourceId, int playActionIconResourceId, int pauseActionIconResourceId, int stopActionIconResourceId, int rewindActionIconResourceId, int fastForwardActionIconResourceId, int previousActionIconResourceId, int nextActionIconResourceId, @Nullable String groupKey) {
        super(context, channelId, notificationId, mediaDescriptionAdapter, notificationListener, primaryActionReceiver, customActionReceiver, smallIconResourceId, playActionIconResourceId, pauseActionIconResourceId, stopActionIconResourceId, rewindActionIconResourceId, fastForwardActionIconResourceId, previousActionIconResourceId, nextActionIconResourceId, groupKey);
    }

    @Override
    protected int[] getActionIndicesForCompactView(List<String> actionNames, Player player) {
        int[] indices = super.getActionIndicesForCompactView(actionNames, player);
        if (useStopActionInCompactView) {
            indices  = Arrays.copyOf(indices, indices.length + 1);
            indices[indices.length - 1] = actionNames.indexOf(ACTION_STOP);
        }
        return indices;
    }

    public final boolean getUseStopActionInCompactView() {
        return this.useStopActionInCompactView;
    }

    /**
     * If {@link #setUseNextAction useNextAction} is {@code true}, sets whether the stop action should
     * also be used in compact view.
     *
     * @param setUseStopActionInCompactView Whether to use the stop action in compact view.
     */
    public void setUseStopActionInCompactView(Boolean useStopActionInCompactView) {
        if (this.useStopActionInCompactView != useStopActionInCompactView) {
            this.useStopActionInCompactView = useStopActionInCompactView;
            invalidate();
        }
    }
}
