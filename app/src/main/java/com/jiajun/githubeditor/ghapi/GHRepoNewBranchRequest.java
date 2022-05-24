package com.jiajun.githubeditor.ghapi;

import android.content.Context;

import androidx.annotation.Nullable;

import com.android.volley.Response;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jiajun.githubeditor.ghapi.contracts.Branch;
import com.jiajun.githubeditor.ghapi.contracts.BranchCreationRequest;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/****
 * This class is used to create branch in repo
 */
public class GHRepoNewBranchRequest extends GHRequestBase<List<Void>>{
    private final BranchCreationRequest m_Body;
    public GHRepoNewBranchRequest(Context ctx, String repoName, @NotNull BranchCreationRequest body, @NotNull ResponseListener<List<Void>> responseListener, @Nullable Response.ErrorListener errorListener) {
        super(ctx, Method.POST, "/repos/:slug/git/refs".replace(":slug",repoName), responseListener, errorListener);
        setShouldCache(false);
        m_Body=body;
    }

    @Override
    protected Type getType() {
        return null;
    }

    @Override
    public byte[] getBody() {
        String body=new Gson().toJson(m_Body);
        return body.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getBodyContentType() {
        return "application/json";
    }

    @Override
    public boolean shouldAttemptParseResponse() {
        return false;
    }
}
