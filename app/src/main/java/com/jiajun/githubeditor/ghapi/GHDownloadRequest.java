package com.jiajun.githubeditor.ghapi;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.volley.Response;
import com.jiajun.githubeditor.ghapi.contracts.DirectoryEntry;

import java.lang.reflect.Type;

/***
 * This class represent the request to download the file
 * @apiNote This class will fire the download as soon as it get the url
 */
public class GHDownloadRequest extends GHRequestBase<DirectoryEntry> implements GHRequestBase.ResponseListener<DirectoryEntry>{

    private final Context m_Context;

    public GHDownloadRequest(Context ctx, String repoName, String path, String branch, @Nullable Response.ErrorListener errorListener) {
        super(ctx, Method.GET, ("/repos/:slug/contents"+path+"?ref="+branch).replace(":slug",repoName), null, errorListener);
        m_Context=ctx;
        overrideListener(this);
    }

    @Override
    protected Type getType() {
        return DirectoryEntry.class;
    }

    @Override
    public boolean shouldAttemptParseResponse() {
        return true;
    }

    /***
     * Overridden to get the url
     */
    @Override
    public void onResponse(DirectoryEntry response) {
        DownloadManager mgr= (DownloadManager) m_Context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request req=new DownloadManager.Request(Uri.parse(response.getDownloadUrl()));
        req.setTitle(response.getName());
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,response.getName());
        req.setAllowedOverMetered(true);
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        req.allowScanningByMediaScanner();
        Toast.makeText(m_Context, "Downloading.....", Toast.LENGTH_SHORT).show();
        mgr.enqueue(req);
    }
}
