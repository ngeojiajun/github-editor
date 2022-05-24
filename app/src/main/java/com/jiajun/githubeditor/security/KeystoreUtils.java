package com.jiajun.githubeditor.security;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Base64;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

/***
 * This class simply represent the static utilities that the application will be use
 */
public class KeystoreUtils {
    private static final String KEYSTORE_NAME="AndroidKeyStore";
    private static final String AES_MODE="AES/GCM/NoPadding";
    private static final String ALIAS="InternalAlias";
    private static final String FIXED_IV="d2Qd5a6x12z=";
    private static KeyStore keystore;
    /***
     * Test weather if the keys created here can be hardware backed
     * @param ctx Any context which enable access toward the package manager
     * @return weather if it is hardware based
     */
    public static boolean isHardwareBacked(@NotNull Context ctx){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE);
        }
        else{
            return false;
        }
    }

    /***
     * Initialize the engine
     * @param ctx The context of application to allow the capabilities test
     * @throws Exception any error happened during the initialization
     */
    public static void init(Context ctx) throws Exception {
        //get the instance toward the android keystore
        keystore = KeyStore.getInstance(KEYSTORE_NAME);
        //load the keystore
        keystore.load(null);
        //try to search for one
        if (!keystore.containsAlias(ALIAS)) {
            //create one if missing
            generateKey(ctx);
        }
    }

    /***
     * Encrypt the provided string and convert it into base64 form
     * @param ctx Application context to regenerate the key
     * @param enc String to encrypt
     * @return Base64 encrypted blob
     * @throws RuntimeException When operation failed
     */
    @NotNull
    public static String encryptString(Context ctx,String enc) throws RuntimeException{
        try {
            //get the key
            Key key=getKey(ctx);
            //create and init the cipher
            Cipher cipher=Cipher.getInstance(AES_MODE);
            cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,FIXED_IV.getBytes()));
            //encrypt it
            byte[] ciphered=cipher.doFinal(enc.getBytes());
            //encode it
            return Base64.encodeToString(ciphered,Base64.NO_WRAP);

        } catch (Exception e) {
            rethrowException(ctx,e);
        }
        return ""; //never returns
    }

    /***
     * Decrypt the provided string and convert it into base64 form
     * @param ctx Application context to regenerate the key
     * @param dec String to decrypt
     * @return Decrypted string
     * @throws RuntimeException When operation failed
     */
    @NotNull
    public static String decryptString(Context ctx,String dec) throws RuntimeException{
        try {
            //get the key
            Key key=getKey(ctx);
            //create and init the cipher
            Cipher cipher=Cipher.getInstance(AES_MODE);
            cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,FIXED_IV.getBytes()));
            //decrypt it
            byte[] decrypted=cipher.doFinal(Base64.decode(dec,Base64.NO_WRAP));
            //encode it
            return new String(decrypted);
        }
        catch (Exception e) {
            rethrowException(ctx,e);
        }
        return ""; //never returns
    }

    /***
     * Test weather the operation can be retried again by authenticating the users
     * @param ex Exception from encryptString or decryptString
     * @return true if the operation can be retried, false otherwise
     */
    public static boolean shouldAskForCredential(@NonNull Exception ex){
        if(ex.getCause()==null){
            return false;
        }
        return (ex.getCause() instanceof UserNotAuthenticatedException);
    }

    public static void askForCredential(@NonNull Activity act, int reqCode, String title, String desc){
        KeyguardManager km=(KeyguardManager) act.getSystemService(Context.KEYGUARD_SERVICE);
        Intent it=km.createConfirmDeviceCredentialIntent(title,desc);
        if(it!=null){
            act.startActivityForResult(it,reqCode);
        }
    }

    public static boolean possibleToUse(Context ctx){
        KeyguardManager km=(KeyguardManager) ctx.getSystemService(Context.KEYGUARD_SERVICE);
        return km.isDeviceSecure();
    }
    private static Key getKey(Context ctx) throws IllegalStateException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        if(keystore==null){
            throw new IllegalStateException("Keystore missing");
        }
        if(!possibleToUse(ctx)){
            throw new IllegalStateException("Cannot perform operation due to the missing security features");
        }
        return keystore.getKey(ALIAS,null);
    }

    private static void generateKey(Context ctx) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyGenerator keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_NAME);
        KeyGenParameterSpec.Builder builder=new KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(false)
                .setUserAuthenticationRequired(true)
                //avoid the biometric requirement
                .setUserAuthenticationValidityDurationSeconds(360);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P){
            builder=builder.setIsStrongBoxBacked(isHardwareBacked(ctx));
        }
        keyGen.init(builder.build());
        keyGen.generateKey();
    }
    private static void rethrowException(Context ctx,Exception ex) throws RuntimeException{
        if(ex instanceof IllegalStateException){
            throw (IllegalStateException)ex;
        }
        if(ex instanceof KeyPermanentlyInvalidatedException || ex instanceof UnrecoverableKeyException){
            //regenerate the key
            //nuke the key
            try {
                keystore.deleteEntry(ALIAS);
                generateKey(ctx);
            } catch (Exception e) {
                //ignore it
                e.printStackTrace();
            }
            throw new RuntimeException("Key invalidated",ex);
        }
        throw new RuntimeException("Operation failed",ex);
    }
}
