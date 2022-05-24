package com.jiajun.githubeditor;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.jiajun.githubeditor.security.Credential;
import com.jiajun.githubeditor.security.KeystoreUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/***
 * Contains the application wide declaration of the resources
 */
public class Application extends android.app.Application{
    /***
     * The global request queue for the application
     */
    private RequestQueue m_RequestQueue;
    private final static String TAG="Application";
    private File m_cacheDir;

    public Credential getGHCredential() {
        return m_Credential;
    }

    public void setGHCredential(Credential credential) {
        this.m_Credential = credential;
    }

    private Credential m_Credential;

    @Override
    public void onCreate() {
        super.onCreate();
        //First create the queue using the defaults
        m_RequestQueue= Volley.newRequestQueue(this);
        //Then initialize the keystore
        try {
            KeystoreUtils.init(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Then create the cache directory if not exists
        m_cacheDir=new File(getCacheDir(),"ghFileCache");
        if(!m_cacheDir.exists()){
            m_cacheDir.mkdir();
        }
        wipeDirectory(m_cacheDir);
        Log.d(TAG,"Creating application");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        m_RequestQueue.cancelAll(unused->true);
    }

    /***
     * Get the application wide request queue
     * @return The queue created
     */
    public RequestQueue getRequestQueue(){
        return m_RequestQueue;
    }

    /***
     * Add a request into queue
     * @param ctx Owner of the request
     * @param req Request it self
     * @param <T> Type of the response
     */
    public <T> void addRequest(@NotNull Context ctx, @NotNull Request<T> req){
        req.setTag(ctx);
        m_RequestQueue.add(req);
    }

    /***
     * Try cancel all requests owned by ctx
     * @param ctx Owner of the requests to be cancelled
     */
    public void cancelAll(@NotNull Context ctx){
        m_RequestQueue.cancelAll(request -> request.getTag()==ctx);
    }

    private void wipeDirectory(File dir){
        assert(dir!=null&&dir.exists()&&dir.isDirectory());
        for(File f:dir.listFiles()){
            if(f.isDirectory()){
                wipeDirectory(f);
                f.delete();
            }
            else {
                f.delete();
            }
        }
    }

    public File createTempFile(String ext) throws IOException {
        return File.createTempFile("cache",ext,m_cacheDir);
    }
}
