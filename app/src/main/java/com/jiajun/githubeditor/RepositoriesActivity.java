package com.jiajun.githubeditor;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.ClientError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.jiajun.githubeditor.databinding.ActivityRepositoriesBinding;
import com.jiajun.githubeditor.databinding.ListItemBinding;
import com.jiajun.githubeditor.ghapi.GHRequestBase;
import com.jiajun.githubeditor.ghapi.GHUserRepoRequest;
import com.jiajun.githubeditor.ghapi.contracts.Repository;

import java.util.ArrayList;
import java.util.List;


public class RepositoriesActivity extends AppCompatActivity {

    private final static int REQUEST_CODE_LOGIN=150;
    private ActivityRepositoriesBinding view;
    private RepositoryAdapter m_repositoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Repositories");
        view= ActivityRepositoriesBinding.inflate(getLayoutInflater());
        setContentView(view.getRoot());
        if(checkLogin()){
            initialize();
        }
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

    /***
     * actually initialize the application
     */
    private void initialize() {
        LinearLayoutManager manager=new LinearLayoutManager(this);
        if(m_repositoryAdapter==null) {
            m_repositoryAdapter = new RepositoryAdapter();
            m_repositoryAdapter.loadData();
        }
        view.listRepos.setLayoutManager(manager);
        view.listRepos.setAdapter(m_repositoryAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //dismiss the dialog
        if(m_repositoryAdapter!=null&&m_repositoryAdapter.m_loadingDialog!=null) {
            m_repositoryAdapter.m_loadingDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate the menu
        getMenuInflater().inflate(R.menu.menu_entry_list,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.menuItemRefresh){
            m_repositoryAdapter.loadData();
        }
        return super.onOptionsItemSelected(item);
    }

    private class RepositoryAdapter extends RecyclerView.Adapter<RepositoryAdapter.ViewHolder>{
        private final ArrayList<Repository> m_Repos=new ArrayList<>();
        private GHUserRepoRequest m_loader=null;
        private final Handler m_Handlers=new Handler();
        private final Context m_execContext=RepositoriesActivity.this;
        @SuppressWarnings("deprecation")
        public final ProgressDialog m_loadingDialog;

        public RepositoryAdapter(){
            m_loadingDialog=new ProgressDialog(m_execContext);
            m_loadingDialog.setTitle("Loading");
            m_loadingDialog.setCancelable(false);
            setHasStableIds(true);
        }

        @NonNull
        @Override
        public RepositoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater=LayoutInflater.from(parent.getContext());
            View view=inflater.inflate(R.layout.list_item,parent,false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RepositoryAdapter.ViewHolder holder, int position) {
            holder.bind(m_Repos.get(position));
        }

        @Override
        public int getItemCount() {
            return m_Repos.size();
        }

        @Override
        public long getItemId(int position) {
            if(position>m_Repos.size()){
                return -1;
            }
            else{
                return m_Repos.get(position).getId();
            }
        }

        public void loadData(){
            //cancel all operations
            ((Application)getApplication()).cancelAll(m_execContext);
            m_Repos.clear();
            notifyDataSetChanged();
            m_loader=new GHUserRepoRequest(m_execContext,m_Handlers,m_Handlers);
            ((Application)getApplication()).addRequest(m_execContext,m_loader);
            m_loadingDialog.show();
        }

        private class Handler implements GHRequestBase.ResponseListener<List<Repository>>,Response.ErrorListener{

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
            public void onResponse(List<Repository> response) {
                if(m_loadingDialog.isShowing()){
                    m_loadingDialog.dismiss(); //dismiss the dialog
                }
                int pos=m_Repos.size(); //old size
                m_Repos.addAll(response); //set the data list
                notifyItemRangeInserted(pos,response.size());
                Log.d("RepositoriesLoader",String.format("Next chunk loaded, cur=%d, new=%d",pos,m_Repos.size()));
                if(m_loader.getResolvedLinkMap().containsKey("next")){
                    //queue a load on the next page in background when it have more
                    String next=m_loader.getResolvedLinkMap().get("next");
                    m_loader=new GHUserRepoRequest(m_execContext,next,this,this);
                    ((Application)getApplication()).addRequest(m_execContext,m_loader);
                }
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private final ListItemBinding view;
            private Repository m_val;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                view=ListItemBinding.bind(itemView);
                view.imgIcon.setContentDescription("Repository");
                itemView.setOnClickListener(this);
            }
            public void bind(Repository val){
                m_val=val;
                view.txtItemName.setText(val.getFullName());
            }

            @Override
            public void onClick(View view) {
                Intent intent=new Intent(m_execContext,RepositoryContentActivity.class);
                intent.putExtra(RepositoryContentActivity.EXTRAS_REPO_NAME,m_val.getFullName());
                startActivity(intent);
            }
        }
    }
}