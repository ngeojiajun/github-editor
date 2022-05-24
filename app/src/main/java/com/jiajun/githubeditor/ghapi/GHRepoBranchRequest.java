package com.jiajun.githubeditor.ghapi;

import android.content.Context;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.Response;
import com.google.gson.reflect.TypeToken;
import com.jiajun.githubeditor.ghapi.contracts.Branch;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

/****
 * This class is used to obtain the list of the branches that exposed by a repo
 */
public class GHRepoBranchRequest extends GHRequestBase<List<Branch>>{
    public GHRepoBranchRequest(Context ctx, String repoName, @NotNull ResponseListener<List<Branch>> responseListener, @Nullable Response.ErrorListener errorListener) {
        super(ctx, Method.GET, "/repos/:slug/branches".replace(":slug",repoName), responseListener, errorListener);
        setShouldCache(false);
    }

    @Override
    protected Type getType() {
        return new TypeToken<LinkedList<Branch>>(){}.getType();
    }

    @Override
    public boolean shouldAttemptParseResponse() {
        return true;
    }
}
