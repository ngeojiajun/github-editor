package com.jiajun.githubeditor;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import org.jetbrains.annotations.NotNull;

/***
 * Some utilities to be used
 */
public class Utils {
    public static void showQuickDialog(@NotNull Context ctx, @NotNull String title, @NotNull String msg){
        showQuickDialog(ctx,title,msg,"OK");
    }

    public static void showQuickDialog(@NotNull Context ctx,@NotNull String title, @NotNull String msg, @NotNull String ok) {
        AlertDialog.Builder adb=new AlertDialog.Builder(ctx);
        adb.setTitle(title);
        adb.setMessage(msg);
        adb.setPositiveButton(ok,((dialogInterface, i) -> dialogInterface.dismiss()));
        adb.create().show();
    }

    public static void showQuickDialog(@NotNull Context ctx, @StringRes int title,@StringRes int msg,@StringRes int ok){
        Resources res=ctx.getResources();
        showQuickDialog(ctx,res.getString(title),res.getString(msg),res.getString(ok));
    }

    public static void showDialogAction(@NotNull Context ctx, @NotNull String title, @NotNull String msg, @NotNull String ok, DialogInterface.OnClickListener clickListener) {
        AlertDialog.Builder adb=new AlertDialog.Builder(ctx);
        adb.setTitle(title);
        adb.setMessage(msg);
        adb.setCancelable(false);
        adb.setPositiveButton(ok,clickListener);
        adb.create().show();
    }

    public static void showDialogAction(@NotNull Context ctx, @StringRes int title,@StringRes int msg,@StringRes int ok, DialogInterface.OnClickListener clickListener){
        Resources res=ctx.getResources();
        showDialogAction(ctx,res.getString(title),res.getString(msg),res.getString(ok),clickListener);
    }
}
