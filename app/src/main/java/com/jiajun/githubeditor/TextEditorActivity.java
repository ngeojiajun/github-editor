package com.jiajun.githubeditor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.jiajun.githubeditor.databinding.ActivityTextEditorBinding;
import com.jiajun.githubeditor.databinding.LayoutSaveDialogBinding;
import com.jiajun.githubeditor.ghapi.GHFileUpdateRequest;
import com.jiajun.githubeditor.ghapi.GHRawRepoContentRequest;
import com.jiajun.githubeditor.ghapi.contracts.ContentUpdateBody;

import java.nio.charset.StandardCharsets;

public class TextEditorActivity extends AppCompatActivity {
    public static final String EXTRA_REPO_NAME="com.jiajun.githubeditor.repo_name";
    public static final String EXTRA_BRANCH_NAME="com.jiajun.githubeditor.branch_name";
    public static final String EXTRA_PATH="com.jiajun.githubeditor.path_name";
    public static final String EXTRA_SHA="com.jiajun.githubeditor.sha";
    private final static int REQUEST_CODE_LOGIN=150;
    private ActivityTextEditorBinding view;
    private String m_RepoName=null,m_BranchName=null,m_Path=null,m_Sha=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        view=ActivityTextEditorBinding.inflate(getLayoutInflater());
        setContentView(view.getRoot());
        //check intent
        Intent intent=getIntent();
        m_RepoName=intent.getStringExtra(EXTRA_REPO_NAME);
        m_BranchName=intent.getStringExtra(EXTRA_BRANCH_NAME);
        m_Path=intent.getStringExtra(EXTRA_PATH);
        m_Sha=intent.getStringExtra(EXTRA_SHA);
        //initialize the app
        if(m_RepoName==null||m_BranchName==null||m_Path==null||m_Sha==null){
            Utils.showDialogAction(this,"Error","Invalid startup param, Bugs?","Back",((dialogInterface, i) -> finish()));
            return;
        }
        if(checkLogin()){
            initialize();
        }
    }

    private void updateUI(){
        ActionBar ab=getSupportActionBar();
        assert(ab!=null);
        ab.setTitle(m_Path);
    }

    @SuppressWarnings("deprecation")
    private void initialize() {
        updateUI();
        ProgressDialog pd=new ProgressDialog(this);
        pd.setTitle("Loading");
        //load the content
        GHRawRepoContentRequest request=new GHRawRepoContentRequest(this,m_RepoName,m_Path,m_BranchName,(result)->{
            pd.dismiss();
            try{
                //decode it
                String s=new String(result);
                view.getRoot().setText(s);
            }
            catch(Exception ex){
                ex.printStackTrace();
                Toast.makeText(this, "Cannot open the file", Toast.LENGTH_SHORT).show();
                finish();
            }
        },error -> {
            pd.dismiss();
            error.printStackTrace();
            Toast.makeText(this, "Cannot open the file", Toast.LENGTH_SHORT).show();
            finish();
        });
        ((Application)getApplication()).addRequest(this,request);
    }

    /***
     * Check weather it is logged in
     * @return Should the application continue to attempt the initialization
     */
    @SuppressWarnings("deprecation") //shut the **** up
    private boolean checkLogin(){
        Application app= (Application) getApplication();
        if(app.getGHCredential()==null){
            //not logged in
            //create into to login activity
            Intent i=new Intent(this,LoginActivity.class);
            startActivityForResult(i,REQUEST_CODE_LOGIN);
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==REQUEST_CODE_LOGIN){
            if(resultCode!=RESULT_OK){
                finish(); //just finish it up because the authentication is not done
            }
            else{
                initialize();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_editor,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.menuItemSaveFile) {
            saveFile();
            return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private void saveFile() {
        AlertDialog.Builder adb=new AlertDialog.Builder(this);
        LayoutSaveDialogBinding dialogView=LayoutSaveDialogBinding.inflate(getLayoutInflater());
        dialogView.editTextFileName.setText(m_Path);
        dialogView.editTextCommitMessage.setText("Update file @".replace("@",m_Path));
        adb.setView(dialogView.getRoot());
        adb.setTitle("Save file");
        adb.setPositiveButton("Save",(dialogInterface, i) -> {
            dialogInterface.dismiss();
            ProgressDialog pd=new ProgressDialog(this);
            pd.setTitle("Saving");
            String target=dialogView.editTextFileName.getText().toString();
            //prepare the request
            ContentUpdateBody body=new ContentUpdateBody();
            body.message=dialogView.editTextCommitMessage.getText().toString();
            if(m_Path.equals(target)){
                body.sha=m_Sha;
            }
            body.content= Base64.encodeToString(view.getRoot().getText().toString().getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP);
            body.branch=m_BranchName;
            GHFileUpdateRequest request=new GHFileUpdateRequest(this,m_RepoName,target,body,(result)->{
                pd.dismiss();
                //update the sha of the file
                m_Sha=result.content.getSha();
                //then the file name
                m_Path=result.content.getPath();
                updateUI();
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            },error -> {
                pd.dismiss();
                error.printStackTrace();
                Toast.makeText(this, "Cannot save the file", Toast.LENGTH_SHORT).show();
            });
            ((Application)getApplication()).addRequest(this,request);
        });
        adb.setCancelable(true);
        adb.create().show();
    }
}