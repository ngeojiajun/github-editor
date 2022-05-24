package com.jiajun.githubeditor.security;

import java.security.SecureRandom;
import java.util.Arrays;

/***
 * A class which represents the credential
 */
public class Credential {
    private final byte[] enc,blob;
    /****
     * Create the credential object with the string as the content
     * @param string content
     */
    public Credential(String string){
        //get 10 bytes from the CSPRNG
        blob= SecureRandom.getSeed(10);
        enc=performJob(string.getBytes());
    }

    /***
     * Reveal the content
     * @return the revealed string
     */
    public String reveal(){
        return new String(performJob(enc));
    }

    ////TODO: place this into NDK code for better performance and security
    private byte[] performJob(byte[] in){
        byte[] out=new byte[in.length];
        Arrays.fill(out,(byte)0xF2);
        for(int i=0;i<5;i++){
            for(int j=0;j<in.length;j++){
                out[j]=(byte)((in[j]^0x0E)^blob[j%10]);
            }
        }
        return out;
    }
}
