package com.jiajun.githubeditor;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.ClientError;
import com.jiajun.githubeditor.databinding.ActivityLoginBinding;
import com.jiajun.githubeditor.ghapi.GHUserRequest;
import com.jiajun.githubeditor.security.Credential;
import com.jiajun.githubeditor.security.KeystoreUtils;

import java.util.ArrayList;
import java.util.List;

/***
 * Activity to deal with login
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private ActivityLoginBinding view;
    private final static int REQUEST_AUTH_SAVING=1;
    private final static int REQUEST_AUTH_LOADING=2;
    private final static String SHARED_PREFERENCES_GH_KEY="GH_KEY";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view=ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(view.getRoot());
        view.buttonLogin.setOnClickListener(this);
        view.buttonLoginWithSaved.setOnClickListener(this);
        if(!KeystoreUtils.possibleToUse(this)){
            //no security features
            Utils.showDialogAction(this,"Error","To use this app you need to have screen lock enabled.","Exit",(dialogInterface, i) -> finish());
            return;
        }
        resumeSession(true,false);
    }

    @Override
    public void onClick(View view) {
       if(view.getId()== R.id.buttonLogin) {
           login();
       }
       else if(view.getId()==R.id.buttonLoginWithSaved){
           resumeSession(true,true);
       }
    }

    private void login(){
        EditText[] controls=new EditText[]{view.editTextCredEmail,view.editTextCredPat};
        List<String> parts=new ArrayList<>();
        for(EditText control:controls){
            if(control.getText().length()==0){
                control.setError("This field is required");
                return;
            }
            else
            {
                control.setError(null);
                parts.add(control.getText().toString());
            }
        }
        String constructed= String.format("%s:%s", (Object[]) parts.toArray(new String[]{}));
        //create the object
        Credential credential=new Credential(Base64.encodeToString(constructed.getBytes(),Base64.NO_WRAP));
        //set global handle
        ((Application)getApplication()).setGHCredential(credential);
        validateCredentials(false);
    }

    private void resumeSession(boolean promptIfFailed, boolean promptIfMissing){
        try {
            //if credentials are exists and restored
            if (restoreCredentials()) {
                validateCredentials(true);
            }
            else if(promptIfMissing){
                Utils.showQuickDialog(this,"Error","No credentials saved here");
            }
        }
        catch(Exception ex){
            if(promptIfFailed&&KeystoreUtils.shouldAskForCredential(ex)){
                KeystoreUtils.askForCredential(this,REQUEST_AUTH_LOADING,"Authorization","Please authorize to load the key");
            }
            else{
                Utils.showQuickDialog(this,"Error","Cannot load the key");
            }
        }
    }

    /****
     * Try to write the credentials into file
     */
    private void flushCredentials() throws RuntimeException{
        Credential credential=((Application)getApplication()).getGHCredential();
        assert(credential!=null);
        String authorization = KeystoreUtils.encryptString(this, credential.reveal());
        SharedPreferences sp=getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_GH_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor spe=sp.edit();
        spe.putString(SHARED_PREFERENCES_GH_KEY,authorization);
        spe.apply();
        Utils.showQuickDialog(this,"OK","Yey");
    }

    /***
     * Try to read the credentials
     * @return weather valid credential found
     * @throws RuntimeException if there is error happened when decryption is attempted
     */
    private boolean restoreCredentials() throws RuntimeException{
        SharedPreferences sp=getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_GH_KEY, Context.MODE_PRIVATE);
        if(sp.contains(SHARED_PREFERENCES_GH_KEY)){
            String base64=sp.getString(SHARED_PREFERENCES_GH_KEY,null);
            if(base64!=null){
                //try to decrypt it
                Credential credential=new Credential(KeystoreUtils.decryptString(this,base64));
                ((Application)getApplication()).setGHCredential(credential);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data){
        if(reqCode==REQUEST_AUTH_SAVING||reqCode==REQUEST_AUTH_LOADING){
            if(resCode!=RESULT_OK){
                Utils.showQuickDialog(this,"Error","Cannot authenticate, please try again");
            }
            else{
                //retry
                if(reqCode==REQUEST_AUTH_SAVING){
                    try{
                        flushCredentials();
                        completeLogin();
                    }
                    catch(RuntimeException ex){
                        Utils.showQuickDialog(this,"Error","Cannot save key, please try again");
                        ((Application)getApplication()).setGHCredential(null);
                    }
                }
                else{
                    try{
                        //we don't check here because the key never used unless it got one encrypted
                        resumeSession(false, false);
                    }
                    catch(RuntimeException ex){
                        Utils.showQuickDialog(this,"Error","Cannot load key, please try again");
                    }
                }
            }
        }
        else {
            super.onActivityResult(reqCode, resCode, data);
        }
    }

    /***
     * Validate the current saved credentials
     * @param loggingIn is the user is logging in? (resuming from session)
     */
    @SuppressWarnings("deprecation") //We need that because we dont want users to interact until we have done
    private void validateCredentials(boolean loggingIn){
        ProgressDialog pd=new ProgressDialog(this);
        pd.setCancelable(false);
        pd.setTitle("Logging in");
        GHUserRequest validation=new GHUserRequest(this,(unused)->{
            pd.dismiss();
            //authentication succeeded
            if(!loggingIn){
                //try to save this into keystore
                try {
                    flushCredentials();
                    completeLogin();
                }
                catch(Exception ex){
                    if(KeystoreUtils.shouldAskForCredential(ex)){
                        KeystoreUtils.askForCredential(this,REQUEST_AUTH_SAVING,"Authorization","Requesting authentication for saving the GH credentials");
                    }
                    else{
                        Utils.showQuickDialog(this,"Error","Cannot save key, please try again");
                        ((Application)getApplication()).setGHCredential(null);
                        ex.printStackTrace();
                    }
                }
            }else{
                completeLogin();
            }
        },error ->{
            pd.dismiss();
            error.printStackTrace();
            if(!(error instanceof ClientError)){
                Utils.showQuickDialog(this,"Error","Network error happened, please try again");
            }
            else{
                Utils.showQuickDialog(this,"Error","Invalid credentials");
                ((Application)getApplication()).setGHCredential(null);
                SharedPreferences sp=getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_GH_KEY, Context.MODE_PRIVATE);
                SharedPreferences.Editor spe=sp.edit();
                spe.remove(SHARED_PREFERENCES_GH_KEY);
                spe.apply();
            }
        });
        ((Application)getApplication()).addRequest(this,validation);
        pd.show();
    }
    private void completeLogin(){
        //return to the caller
        setResult(RESULT_OK);
        finish();
    }
}