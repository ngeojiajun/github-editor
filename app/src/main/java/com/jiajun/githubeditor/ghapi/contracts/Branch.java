package com.jiajun.githubeditor.ghapi.contracts;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

/***
 * Contain partial data we need for the /repos/OWNER/REPO/branches
 */
@SuppressWarnings("unused") //all access from reflection GSON
public class Branch {
    private String name;

    @SerializedName("protected")
    private boolean _protected;

    private Commit commit;

    public String getName() {
        return name;
    }

    public boolean getIsProtected() {
        return _protected;
    }

    @NonNull
    public String toString(){
        return getName();
    }

    public String getSha(){
        return commit.sha;
    }

    /***
     * Internal use only
     */
    private static class Commit {
        public String sha;
    }
}
