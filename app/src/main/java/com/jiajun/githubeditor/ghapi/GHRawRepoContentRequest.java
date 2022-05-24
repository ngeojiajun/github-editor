package com.jiajun.githubeditor.ghapi;

import android.content.Context;

import androidx.annotation.Nullable;

import com.android.volley.Response;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/****
 * This class is used to request raw content from github
 */
public class GHRawRepoContentRequest extends GHRequestBase<byte[]>{
    public GHRawRepoContentRequest(Context ctx, String repoName, String path, String branch, @NotNull ResponseListener<byte[]> responseListener, @Nullable Response.ErrorListener errorListener) {
        super(ctx, Method.GET, ("/repos/:slug/contents"+path+"?ref="+branch).replace(":slug",repoName), responseListener, errorListener);
    }

    @Override
    protected Type getType() {
        return byte[].class;
    }

    @Override
    public boolean shouldAttemptParseResponse() {
        return true;
    }

}