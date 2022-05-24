package com.jiajun.githubeditor.ghapi;

import android.content.Context;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.Response;
import com.google.gson.reflect.TypeToken;
import com.jiajun.githubeditor.ghapi.contracts.Repository;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

/***
 * Represent the /user/repo endpoint
 */
public class GHUserRepoRequest extends GHRequestBase<List<Repository>> {
    public GHUserRepoRequest(Context ctx, @NotNull ResponseListener<List<Repository>> responseListener, @Nullable Response.ErrorListener errorListener) {
        this(ctx, "", responseListener, errorListener);
    }

    public GHUserRepoRequest(Context ctx,String additionalParam,@NotNull ResponseListener<List<Repository>> responseListener, @Nullable Response.ErrorListener errorListener) {
        super(ctx, Request.Method.GET, "/user/repos"+additionalParam, responseListener, errorListener);
    }

    @Override
    protected Type getType() {
        return new TypeToken<LinkedList<Repository>>(){}.getType();
    }

    @Override
    public boolean shouldAttemptParseResponse() {
        return true;
    }
}
