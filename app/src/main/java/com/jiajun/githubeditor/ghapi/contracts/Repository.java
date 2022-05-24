package com.jiajun.githubeditor.ghapi.contracts;

/****
 * Represent each entries returned by /user/repos
 */
@SuppressWarnings("unused") //all access from reflection GSON
public class Repository {
    private String name;
    private String full_name;
    private String description;
    private String url;
    private long id;

    public String getName() {
        return name;
    }

    public String getFullName() {
        return full_name;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public long getId() {
        return id;
    }
}
