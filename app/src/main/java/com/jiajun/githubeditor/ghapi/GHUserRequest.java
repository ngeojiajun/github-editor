package com.jiajun.githubeditor.ghapi;

import android.content.Context;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.Response;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/***
 * Represent the /user endpoint
 * @implNote It dont process anything, only used to probe the api usability
 * Assume that if it response then the credential are correct if not it is failed
 */
public class GHUserRequest extends GHRequestBase<Void> {
    public GHUserRequest(Context ctx,@NotNull ResponseListener<Void> responseListener, @Nullable Response.ErrorListener errorListener) {
        super(ctx, Request.Method.GET, "/user", responseListener, errorListener);
    }

    @Override
    protected Type getType() {
        return null;
    }

    @Override
    public boolean shouldAttemptParseResponse() {
        return false;
    }
}
