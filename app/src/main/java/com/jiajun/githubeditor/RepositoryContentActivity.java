package com.jiajun.githubeditor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.ClientError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.jiajun.githubeditor.databinding.ActivityRepositoryContentBinding;
import com.jiajun.githubeditor.databinding.LayoutSaveDialogBinding;
import com.jiajun.githubeditor.databinding.ListItemBinding;
import com.jiajun.githubeditor.ghapi.GHDownloadRequest;
import com.jiajun.githubeditor.ghapi.GHFileUpdateRequest;
import com.jiajun.githubeditor.ghapi.GHRawRepoContentRequest;
import com.jiajun.githubeditor.ghapi.GHRepoBranchRequest;
import com.jiajun.githubeditor.ghapi.GHRepoContentRequest;
import com.jiajun.githubeditor.ghapi.GHRepoNewBranchRequest;
import com.jiajun.githubeditor.ghapi.GHRequestBase;
import com.jiajun.githubeditor.ghapi.contracts.Branch;
import com.jiajun.githubeditor.ghapi.contracts.BranchCreationRequest;
import com.jiajun.githubeditor.ghapi.contracts.ContentUpdateBody;
import com.jiajun.githubeditor.ghapi.contracts.DirectoryEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/***
 * Activity to deal with explorer
 */
public class RepositoryContentActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private final static int REQUEST_CODE_LOGIN=150;
    private final static int REQUEST_CODE_WRITE_STORAGE=151;
    public final static String EXTRAS_REPO_NAME="com.jiajun.githubeditor.repo_name";
    private Spinner m_spinnerBranches;
    private ArrayAdapter<Branch> m_branchesSource;
    private String repoName,branchName;
    private final Stack<String> m_path=new Stack<>();
    private ContentAdapter m_adapter;
    /***
     * File to be downloaded
     */
    private DirectoryEntry m_FileToDownload=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        if(savedInstanceState!=null){
            branchName=savedInstanceState.getString("branchName",null);
            String[] path=savedInstanceState.getStringArray("paths");
            m_path.addAll(Arrays.asList(path != null ? path : new String[]{"/"}));
        }
        else{
            m_path.push("/");
        }
        //bind the view
        com.jiajun.githubeditor.databinding.ActivityRepositoryContentBinding view = ActivityRepositoryContentBinding.inflate(getLayoutInflater());
        setContentView(view.getRoot());
        //initialize the spinner
        m_spinnerBranches=new Spinner(this);
        //initialize the branches adapter
        m_branchesSource=new ArrayAdapter<>(this,R.layout.spinner_actionbar);
        m_branchesSource.setNotifyOnChange(false);
        //setup the action bar
        ActionBar actionBar=getSupportActionBar();
        assert(actionBar!=null);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(m_spinnerBranches);
        //setup the spinner
        m_spinnerBranches.setAdapter(m_branchesSource);
        //check weather the intent is valid
        repoName=getIntent().getStringExtra(EXTRAS_REPO_NAME);
        //setup the adapter
        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        m_adapter=new ContentAdapter();
        view.listContents.setAdapter(m_adapter);
        view.listContents.setLayoutManager(layoutManager);
        if(repoName==null){
            Utils.showDialogAction(this,"Error","Invalid startup param, Bugs?","Back",((dialogInterface, i) -> finish()));
            return;
        }
        if(checkLogin()){
            initialize();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle instanceState) {
        super.onSaveInstanceState(instanceState);
        instanceState.putString("branchName",branchName);
        instanceState.putStringArray("paths",m_path.toArray(new String[]{}));
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_WRITE_STORAGE:{
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    //retry the download operation
                    performFileDownload();
                }
                else{
                    Utils.showQuickDialog(this,"Error","Cannot perform file download because of missing permission");
                }
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void performFileDownload(){
        if(m_FileToDownload==null||!m_FileToDownload.getType().equals("file")){
            Log.e("ContentDownloader","Invalid file enqueued");
            return;
        }
        GHDownloadRequest downloadRequest=new GHDownloadRequest(this,repoName,"/"+m_FileToDownload.getPath(),branchName,(e)->{
           e.printStackTrace();
           Toast.makeText(this,"Cannot download the file",Toast.LENGTH_SHORT).show();
        });
        sendRequest(downloadRequest);
    }

    @SuppressWarnings("deprecation") //We need that because we dont want users to interact until we have done
    private void initialize(){
        m_spinnerBranches.setOnItemSelectedListener(null);
        ProgressDialog pd=new ProgressDialog(this);
        pd.setCancelable(false);
        pd.setTitle("Loading Branches");
        GHRepoBranchRequest loading=new GHRepoBranchRequest(this,repoName,(data)->{
            pd.dismiss();
            m_branchesSource.clear();
            m_branchesSource.addAll(data);
            m_branchesSource.notifyDataSetChanged();
            m_spinnerBranches.setOnItemSelectedListener(RepositoryContentActivity.this);
            if(branchName!=null){
                for (int i = 0; i <data.size() ; i++) {
                    if(data.get(i).getName().equals(branchName)){
                        m_spinnerBranches.setSelection(i);
                        return;
                    }
                }
            }
            m_spinnerBranches.setSelection(0);
        },error ->{
            pd.dismiss();
            error.printStackTrace();
            if(!(error instanceof ClientError)){
                Utils.showQuickDialog(this,"Error","Network error happened, please try again");
            }
            else{
                Utils.showDialogAction(this,"Error","Invalid credentials","Quit",(dialogInterface, i) -> finishAffinity());
            }
        });
        sendRequest(loading);
        pd.show();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        branchName=m_spinnerBranches.getSelectedItem().toString();
        updateList();
    }

    private void updateList() {
        ActionBar actionBar=getSupportActionBar();
        assert(actionBar!=null);
        m_adapter.loadData(m_path.peek());
        if(m_path.size()>1){
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowCustomEnabled(false);
            actionBar.setTitle(m_path.peek());
        }
        else{
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
        }
    }

    private void sendRequest(Request<?> req){
        ((Application)getApplication()).addRequest(this,req);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public void onBackPressed(){
        if(m_path.size()>1){
            m_path.pop();
            updateList();
        }
        else{
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate the menu
        getMenuInflater().inflate(R.menu.menu_entry_list,menu);
        menu.add(Menu.NONE,5000,Menu.NONE,"New branch");
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.menuItemRefresh){
            initialize();
            //updateList();
            return true;
        }
        else if(item.getItemId()==5000){
            AlertDialog.Builder adb=new AlertDialog.Builder(this);
            EditText editor=new EditText(this);
            editor.setHint("Branch name");
            adb.setView(editor);
            adb.setTitle("New branch");
            adb.setPositiveButton(android.R.string.ok,(dialogInterface, i) -> {
                if(editor.getText().length()==0){
                    editor.setError("This field must be filled in");
                    return;
                }
                dialogInterface.dismiss();
                ProgressDialog pd=new ProgressDialog(this);
                pd.setTitle("Creating branch");
                //prepare the request
                BranchCreationRequest body=new BranchCreationRequest();
                body.sha=((Branch)m_spinnerBranches.getSelectedItem()).getSha();
                body.ref="refs/heads/:name".replace(":name",editor.getText());
                GHRepoNewBranchRequest request=new GHRepoNewBranchRequest(this,repoName,body,(result)->{
                    pd.dismiss();
                    Toast.makeText(this, "Branch created", Toast.LENGTH_SHORT).show();
                    initialize(); //reinitialize the activity
                },error -> {
                    pd.dismiss();
                    error.printStackTrace();
                    Toast.makeText(this, "Cannot create the branch", Toast.LENGTH_SHORT).show();
                });
                ((Application)getApplication()).addRequest(this,request);
            });
            adb.setCancelable(true);
            adb.create().show();
        }
        return super.onOptionsItemSelected(item);
    }

    private class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.ViewHolder>{
        private final ArrayList<DirectoryEntry> m_Content=new ArrayList<>();
        private final ContentAdapter.Handler m_Handlers=new ContentAdapter.Handler();
        private final Context m_execContext=RepositoryContentActivity.this;
        @SuppressWarnings("deprecation")
        private final ProgressDialog m_loadingDialog;
        private static final int TYPE_DIR=1;
        private static final int TYPE_FILE=2;

        @SuppressWarnings("deprecation")
        public ContentAdapter(){
            m_loadingDialog=new ProgressDialog(m_execContext);
            m_loadingDialog.setTitle("Loading");
            m_loadingDialog.setCancelable(false);
            setHasStableIds(true);
        }

        @NonNull
        @Override
        public ContentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater=LayoutInflater.from(parent.getContext());
            View view=inflater.inflate(R.layout.list_item,parent,false);
            return new ContentAdapter.ViewHolder(view,viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull ContentAdapter.ViewHolder holder, int position) {
            holder.bind(m_Content.get(position));
        }

        @Override
        public int getItemCount() {
            return m_Content.size();
        }

        @Override
        public long getItemId(int position) {
            if(position>m_Content.size()){
                return -1;
            }
            else{
                return m_Content.get(position).getSha().hashCode();
            }
        }
        @Override
        public int getItemViewType(int pos){
            if(pos>m_Content.size()){
                return -1;
            }
            else{
                DirectoryEntry entry= m_Content.get(pos);
                return entry.getType().equals("dir")?TYPE_DIR:TYPE_FILE;
            }
        }
        @SuppressLint("NotifyDataSetChanged")
        public void loadData(String path){
            //cancel all operations
            ((Application)getApplication()).cancelAll(m_execContext);
            m_Content.clear();
            notifyDataSetChanged();
            GHRepoContentRequest m_loader = new GHRepoContentRequest(m_execContext,repoName,path,branchName, m_Handlers, m_Handlers);
            sendRequest(m_loader);
            m_loadingDialog.show();
        }

        private class Handler implements GHRequestBase.ResponseListener<List<DirectoryEntry>>, Response.ErrorListener{

            @Override
            public void onErrorResponse(VolleyError error) {
                m_loadingDialog.dismiss();
                error.printStackTrace();
                if(!(error instanceof ClientError)){
                    Utils.showQuickDialog(m_execContext,"Error","Network error happened, please try again");
                }
                else{
                    Utils.showQuickDialog(m_execContext,"Error","Some error happened");
                }
            }

            @Override
            public void onResponse(List<DirectoryEntry> response) {
                if(m_loadingDialog.isShowing()){
                    m_loadingDialog.dismiss(); //dismiss the dialog
                }
                int pos=m_Content.size(); //old size
                m_Content.addAll(response); //set the data list
                notifyItemRangeInserted(pos,response.size());
            }
        }

        private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private final ListItemBinding view;
            private DirectoryEntry m_val;
            private final int m_type;
            public ViewHolder(@NonNull View itemView,int type) {
                super(itemView);
                view=ListItemBinding.bind(itemView);
                m_type=type;
                if(type==TYPE_FILE) {
                    view.imgIcon.setContentDescription("File");
                    view.imgIcon.setImageResource(R.drawable.ic_file);
                }
                else{
                    view.imgIcon.setContentDescription("Directory");
                    view.imgIcon.setImageResource(R.drawable.ic_folder);
                }
                itemView.setOnClickListener(this);
            }
            public void bind(DirectoryEntry val){
                m_val=val;
                view.txtItemName.setText(val.getName());
            }

            @Override
            public void onClick(View view) {
                if(m_type==TYPE_DIR) {
                    m_path.push("/"+m_val.getPath());
                    updateList();
                }
                else{
                    AlertDialog.Builder opDialog=new AlertDialog.Builder(m_execContext);
                    opDialog.setItems(new String[]{"View","Edit","Download","Cancel"},(dialogInterface, i) -> {
                        switch (i){
                            case 0:
                            {
                                dialogInterface.dismiss();
                                //show the dialog
                                m_loadingDialog.show();
                                GHRawRepoContentRequest request=new GHRawRepoContentRequest(m_execContext,
                                        repoName,
                                        m_val.getPath(),
                                        branchName,
                                        result->{
                                            m_loadingDialog.dismiss();
                                            Uri uri = null;
                                            //if success try to make a file
                                            try{
                                                String name=m_val.getName();
                                                String ext=name.lastIndexOf(".")!=-1?name.substring(name.lastIndexOf(".")):"";
                                                File f=((Application)getApplication()).createTempFile(ext);
                                                try(FileOutputStream fos=new FileOutputStream(f)){
                                                    fos.write(result);
                                                }
                                                uri= FileProvider.getUriForFile(m_execContext,"com.jiajun.githubeditor.cacheprovider",f);
                                            }
                                            catch(IOException ex){
                                                ex.printStackTrace();
                                                Toast.makeText(m_execContext,"Error in loading the file",Toast.LENGTH_SHORT).show();
                                            }
                                            if(uri!=null){
                                                Intent openIntent = new Intent(Intent.ACTION_VIEW,uri);
                                                openIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                Intent chooser = Intent.createChooser(openIntent, "Open this file with");
                                                try {
                                                    startActivity(chooser);
                                                } catch (ActivityNotFoundException ex) {
                                                    Toast.makeText(m_execContext, "No app is capable to open the file", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        },
                                        error->{
                                            m_loadingDialog.dismiss();
                                            error.printStackTrace();
                                            Toast.makeText(m_execContext,"Error in loading the file",Toast.LENGTH_SHORT).show();
                                        }
                                );
                                sendRequest(request);
                                break;
                            }
                            case 1:{
                                dialogInterface.dismiss();
                                Intent intent=new Intent(m_execContext,TextEditorActivity.class);
                                intent.putExtra(TextEditorActivity.EXTRA_PATH,"/"+m_val.getPath());
                                intent.putExtra(TextEditorActivity.EXTRA_BRANCH_NAME,branchName);
                                intent.putExtra(TextEditorActivity.EXTRA_REPO_NAME,repoName);
                                intent.putExtra(TextEditorActivity.EXTRA_SHA, m_val.getSha());
                                startActivity(intent);
                                break;
                            }
                            case 2:{
                                dialogInterface.dismiss();
                                m_FileToDownload=m_val;
                                //check permission if lower than android Q
                                if(Build.VERSION.SDK_INT<Build.VERSION_CODES.Q){
                                    if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CODE_WRITE_STORAGE);
                                        return;
                                    }
                                }
                                performFileDownload();
                                return;
                            }
                            case 3:
                                dialogInterface.dismiss();
                                break;
                        }
                    }).create().show();
                }
            }
        }
    }
}