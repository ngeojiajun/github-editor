package com.jiajun.githubeditor.ghapi.contracts;

/****
 * Directory entry which returned by /repos/OWNER/REPO/contents/PATH?ref={something}
 */
@SuppressWarnings("unused") //all access from reflection GSON
public class DirectoryEntry {
    private String name;
    private String path;
    private String sha;
    private String url;
    private String type;
    private String download_url;

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getSha() {
        return sha;
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public String getDownloadUrl() {
        return download_url;
    }
}
