/*
 * Copyright 2017 - 2018 Vorlonsoft LLC
 *
 * Licensed under The MIT License (MIT)
 */

package com.vorlonsoft.android.rate;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import static com.vorlonsoft.android.rate.IntentHelper.AMAZON_APPSTORE_PACKAGE_NAME;
import static com.vorlonsoft.android.rate.IntentHelper.BLACKBERRY_WORLD_PACKAGE_NAME;
import static com.vorlonsoft.android.rate.IntentHelper.GOOGLE_PLAY_PACKAGE_NAME;
import static com.vorlonsoft.android.rate.IntentHelper.SAMSUNG_GALAXY_APPS_PACKAGE_NAME;
import static com.vorlonsoft.android.rate.IntentHelper.SLIDEME_PACKAGE_NAME;
import static com.vorlonsoft.android.rate.IntentHelper.createIntentForAmazonAppstore;
import static com.vorlonsoft.android.rate.IntentHelper.createIntentForBlackBerryWorld;
import static com.vorlonsoft.android.rate.IntentHelper.createIntentForGooglePlay;
import static com.vorlonsoft.android.rate.IntentHelper.createIntentForMiAppstore;
import static com.vorlonsoft.android.rate.IntentHelper.createIntentForOther;
import static com.vorlonsoft.android.rate.IntentHelper.createIntentForSamsungGalaxyApps;
import static com.vorlonsoft.android.rate.IntentHelper.createIntentForSlideME;
import static com.vorlonsoft.android.rate.IntentHelper.createIntentForTencentAppStore;
import static com.vorlonsoft.android.rate.PreferenceHelper.setAgreeShowDialog;
import static com.vorlonsoft.android.rate.PreferenceHelper.setRemindInterval;
import static com.vorlonsoft.android.rate.UriHelper.getAmazonAppstoreWeb;
import static com.vorlonsoft.android.rate.UriHelper.getBlackBerryWorldWeb;
import static com.vorlonsoft.android.rate.UriHelper.getGooglePlayWeb;
import static com.vorlonsoft.android.rate.UriHelper.getSamsungGalaxyAppsWeb;
import static com.vorlonsoft.android.rate.UriHelper.getSlideMEWeb;
import static com.vorlonsoft.android.rate.UriHelper.isPackageExists;
import static com.vorlonsoft.android.rate.Utils.getDialogBuilder;

public class DefaultDialogManager implements DialogManager {

    static class Factory implements DialogManager.Factory {
        @Override
        public DialogManager createDialogManager(Context context, DialogOptions options) {
            return new DefaultDialogManager(context, options);
        }
    }

    private static final String TAG = "ANDROIDRATE";

    private final Context context;
    private final DialogOptions options;
    private final OnClickButtonListener listener;

    @SuppressWarnings("WeakerAccess")
    protected final DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            final Intent intentToAppStore;
            switch(options.getStoreType()) {
                case AMAZON:
                    intentToAppStore = createIntentForAmazonAppstore(context);
                    break;
                case BLACKBERRY:
                    intentToAppStore = createIntentForBlackBerryWorld(context, options.getBlackBerryWorldApplicationId());
                    break;
                case MI:
                    intentToAppStore = createIntentForMiAppstore(context);
                    break;
                case OTHER:
                    intentToAppStore = createIntentForOther(options.getOtherStoreUri());
                    break;
                case SAMSUNG:
                    intentToAppStore = createIntentForSamsungGalaxyApps(context);
                    break;
                case SLIDEME:
                    intentToAppStore = createIntentForSlideME(context);
                    break;
                case TENCENT:
                    intentToAppStore = createIntentForTencentAppStore(context);
                    break;
                default:
                    intentToAppStore = createIntentForGooglePlay(context);
            }
            try {
                context.startActivity(intentToAppStore);
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Failed to rate app, no activity found for " + intentToAppStore, e);
                switch(options.getStoreType()) {
                    case AMAZON:
                        if (isPackageExists(context, AMAZON_APPSTORE_PACKAGE_NAME)) {
                            context.startActivity(new Intent(Intent.ACTION_VIEW, getAmazonAppstoreWeb(context.getPackageName())));
                        }
                        break;
                    case BLACKBERRY:
                        if (isPackageExists(context, BLACKBERRY_WORLD_PACKAGE_NAME)) {
                            context.startActivity(new Intent(Intent.ACTION_VIEW, getBlackBerryWorldWeb(options.getBlackBerryWorldApplicationId())));
                        }
                        break;
                    case GOOGLEPLAY:
                        if (isPackageExists(context, GOOGLE_PLAY_PACKAGE_NAME)) {
                            context.startActivity(new Intent(Intent.ACTION_VIEW, getGooglePlayWeb(context.getPackageName())));
                        }
                        break;
                    case SAMSUNG:
                        if (isPackageExists(context, SAMSUNG_GALAXY_APPS_PACKAGE_NAME)) {
                            context.startActivity(new Intent(Intent.ACTION_VIEW, getSamsungGalaxyAppsWeb(context.getPackageName())));
                        }
                        break;
                    case SLIDEME:
                        if (isPackageExists(context, SLIDEME_PACKAGE_NAME)) {
                            context.startActivity(new Intent(Intent.ACTION_VIEW, getSlideMEWeb(context.getPackageName())));
                        }
                        break;
                }
            }
            setAgreeShowDialog(context, false);
            if (listener != null) listener.onClickButton((byte) which);
        }
    };
    @SuppressWarnings("WeakerAccess")
    protected final DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            setAgreeShowDialog(context, false);
            if (DefaultDialogManager.this.listener != null) DefaultDialogManager.this.listener.onClickButton((byte) which);
        }
    };
    @SuppressWarnings("WeakerAccess")
    protected final DialogInterface.OnClickListener neutralListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            setRemindInterval(context);
            if (listener != null) listener.onClickButton((byte) which);
        }
    };

    @SuppressWarnings("WeakerAccess")
    public DefaultDialogManager(final Context context, final DialogOptions options) {
        this.context = context;
        this.options = options;
        this.listener = options.getListener();
    }

    public Dialog createDialog() {
        AlertDialog.Builder builder = getDialogBuilder(context, options.getThemeResId());
        builder.setMessage(options.getMessageText(context));

        if (options.shouldShowTitle()) builder.setTitle(options.getTitleText(context));

        builder.setCancelable(options.getCancelable());

        View view = options.getView();
        if (view != null) builder.setView(view);

        builder.setPositiveButton(options.getPositiveText(context), positiveListener);

        if (options.shouldShowNeutralButton()) {
            builder.setNeutralButton(options.getNeutralText(context), neutralListener);
        }

        if (options.shouldShowNegativeButton()) {
            builder.setNegativeButton(options.getNegativeText(context), negativeListener);
        }

        return builder.create();
    }

}