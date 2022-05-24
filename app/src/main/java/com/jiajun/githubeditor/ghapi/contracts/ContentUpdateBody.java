package com.jiajun.githubeditor.ghapi.contracts;

import androidx.annotation.Nullable;

/***
 * This class contain the stuffs used in committing new content
 */
public class ContentUpdateBody {
    /***
     * The commit message
     */
    public String message;
    /***
     * The sha of the file being updated
     */
    @Nullable
    public String sha;
    /***
     * Content of the file in base64
     */
    public String content;
    /***
     * Branch to update
     */
    public String branch;
}
