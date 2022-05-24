package com.jiajun.githubeditor.ghapi;

import android.content.Context;

import androidx.annotation.Nullable;

import com.android.volley.Response;
import com.google.gson.reflect.TypeToken;
import com.jiajun.githubeditor.ghapi.contracts.Branch;
import com.jiajun.githubeditor.ghapi.contracts.DirectoryEntry;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

/****
 * This class is used to obtain the list of the branches that exposed by a repo
 */
public class GHRepoContentRequest extends GHRequestBase<List<DirectoryEntry>>{
    public GHRepoContentRequest(Context ctx, String repoName,String path,String branch, @NotNull ResponseListener<List<DirectoryEntry>> responseListener, @Nullable Response.ErrorListener errorListener) {
        super(ctx, Method.GET, ("/repos/:slug/contents"+path+"?ref="+branch).replace(":slug",repoName), responseListener, errorListener);
        setShouldCache(false); //never cache this
    }

    @Override
    protected Type getType() {
        return new TypeToken<LinkedList<DirectoryEntry>>(){}.getType();
    }

    @Override
    public boolean shouldAttemptParseResponse() {
        return true;
    }
}
