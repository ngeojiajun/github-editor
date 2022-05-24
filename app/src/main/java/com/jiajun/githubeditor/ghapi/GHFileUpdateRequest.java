package com.jiajun.githubeditor.ghapi;

import android.content.Context;

import androidx.annotation.Nullable;

import com.android.volley.Response;
import com.google.gson.Gson;
import com.jiajun.githubeditor.ghapi.contracts.ContentUpdateBody;
import com.jiajun.githubeditor.ghapi.contracts.ContentUpdateResponse;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class GHFileUpdateRequest extends GHRequestBase<ContentUpdateResponse>{
    private final ContentUpdateBody m_Body;
    public GHFileUpdateRequest(Context ctx, String repoName, String path, @NotNull ContentUpdateBody body, @NotNull ResponseListener<ContentUpdateResponse> responseListener, @Nullable Response.ErrorListener errorListener) {
        super(ctx,Method.PUT, ("/repos/:slug/contents"+path).replace(":slug",repoName), responseListener, errorListener);
        m_Body=body;
    }

    @Override
    protected Type getType() {
        return ContentUpdateResponse.class;
    }

    @Override
    public boolean shouldAttemptParseResponse() {
        return true;
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
}
