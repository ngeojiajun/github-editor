package com.jiajun.githubeditor.ghapi;

import android.content.Context;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

import com.android.volley.ClientError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.jiajun.githubeditor.Application;
import com.jiajun.githubeditor.security.Credential;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * Base class for all GH API request
 */
public abstract class GHRequestBase<T> extends Request<T> {
    private final Gson gson=new Gson();
    private GHRequestBase.ResponseListener<T> m_listener;
    private final Context m_Context;
    private final Pattern m_diamondQuotes=Pattern.compile("<(.*?)>");
    private final Pattern m_quotes=Pattern.compile("rel=\"(.*?)\"");
    private final Map<String,String> m_resolvedMap=new HashMap<>();

    public GHRequestBase(Context ctx, int method, String url, @NotNull GHRequestBase.ResponseListener<T> responseListener, @Nullable Response.ErrorListener errorListener) {
        super(method, "https://api.github.com"+url, errorListener);
        m_listener=responseListener;
        m_Context=ctx;
    }

    /****
     * Parse the response and bounce it to client
     * @param response response to parse
     * @return Response
     */
    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        if(response.statusCode>=500){
            return Response.error(new ServerError(response));
        }
        if(response.statusCode>=400){
            return Response.error(new ClientError(response));
        }
        if(response.notModified){
            Log.d("GHResponseParser","Using cache for url="+getUrl());
        }
        if(!shouldAttemptParseResponse()){
            return Response.success(null,null);
        }
        try {
            //try to decode its links first
            m_resolvedMap.clear();
            if(response.headers!=null&&response.headers.containsKey("Link")){
                //get the string
                String link=response.headers.get("Link");
                //split it using ,
                String[] entries= Arrays.stream(link.split(",")).map(String::trim).toArray(String[]::new);
                for(String v : entries){
                    Matcher matcher=m_diamondQuotes.matcher(v);
                    String url=null,rel = null;
                    //the url part
                    if(matcher.find()){
                        url=matcher.group(1);
                    }
                    //the rel
                    matcher=m_quotes.matcher(v);
                    if(matcher.find()){
                        rel=matcher.group(1);
                    }
                    if(rel==null||url==null){
                        m_resolvedMap.clear();
                        Log.w("GHResponseParser","Cannot parse link");
                        Log.w("GHResponseParser","link= "+link);
                        break;
                    }
                    else{
                        url=url.replace(getUrl().replaceFirst("\\\\?(.*?)",""),""); //empty the base part
                        m_resolvedMap.put(rel,url);
                    }
                }
            }
            Type obj=getType();
            if(obj==byte[].class){
                //if it ask for byte array just return the handle to the called
                return Response.success((T)response.data,HttpHeaderParser.parseCacheHeaders(response));
            }
            //then try to decode its response
            String json = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            if(obj == String.class){
                //if it is excepted to response String then don't brother to parse it
                return Response.success((T)json,HttpHeaderParser.parseCacheHeaders(response));
            }
            return Response.success(gson.fromJson(json, getType()),
                    HttpHeaderParser.parseCacheHeaders(response));
        }
        catch(Exception ex){
            return Response.error(new ParseError(ex));
        }
    }

    protected abstract Type getType();

    @Override
    public synchronized void cancel() {
        super.cancel();
        m_listener=null;
    }

    @Override
    protected void deliverResponse(T response)
    {
        if(m_listener!=null) {
            m_listener.onResponse(response);
        }
    }

    @CallSuper
    @Override
    public Map<String,String> getHeaders() {
        Map<String,String> map=new HashMap<>();
        if(m_Context.getApplicationContext() instanceof Application){
            //try to cast into Application
            Credential cred=((Application) m_Context.getApplicationContext()).getGHCredential();
            if(cred!=null) {
                map.put("Authorization", "Basic " + cred.reveal());
            }
        }
        if(getType()==byte[].class){
            map.put("Accept","application/vnd.github.v3.raw");
        }
        else {
            map.put("Accept", "application/vnd.github.v3+json");
        }
        return map;
    }


    public Map<String, String> getResolvedLinkMap() {
        return m_resolvedMap;
    }

    /****
     * Returns should the response parsed by the application
     * @return true if it should
     */
    public abstract boolean shouldAttemptParseResponse();

    protected void overrideListener(ResponseListener<T> res){
        m_listener=res;
    }

    @FunctionalInterface
    public interface ResponseListener<T> {
        void onResponse(T response);
    }

}
