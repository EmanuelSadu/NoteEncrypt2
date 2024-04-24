package marius.stana.note.encrypt2;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.internal.Util;

@Entity(tableName = "note")
public class Note {
    @NonNull
    private String title;
    private String body;
    private int position;
    private String timeStamp;
    private boolean isEncrypted;
    private boolean hidden = false;
    private String file;


    @PrimaryKey
    @NonNull
    private String noteId;

    //Encrypts note with AES
    public Note encrypt() {
        if (this.isEncrypted== true) {
            try {
                String passwd = Utils.getInstance().getPasswd();

                IvParameterSpec iv = new IvParameterSpec(Utils.getInstance().hashBasedCheck(String.valueOf(noteId))
                        .substring(48)
                        .getBytes(StandardCharsets.UTF_8));

                String encTitle = title;
                String encBody = Utils.getInstance().encrypt(iv,body,passwd);
                return new Note(encTitle, encBody, position);
            } catch (Exception ignore) {

                return new Note("", "", 0);
            }
        }
        System.out.println("Failed to encrypt+ "+this.isEncrypted);
        return this;
    }

    //decrypts note with AES
    Note decrypt() {
        String passwd = Utils.getInstance().getPasswd();

        if (this.isEncrypted== false) {
            Log.d("Note:dec","Wrong");
            return this;
        }
        try {
            IvParameterSpec iv = new IvParameterSpec(Utils.getInstance().hashBasedCheck(String.valueOf(this.noteId))
                    .substring(48)
                    .getBytes(StandardCharsets.UTF_8));

           String decTitle = null,decBody = null;
            if (!title.equals(""))
                decTitle = title;
            if (!body.equals("")) {
                System.out.println(body);
                decBody = Utils.getInstance().decrypt(iv, body, passwd);

            }
            return new Note(decTitle, decBody, position);
        } catch (Exception e) {
            e.printStackTrace();
            return new Note("", "", 0);
        }
    }

    @Override
    public String toString() {
        return position + title + body;
    }

    String getNoteId() {
        return noteId;
    }

    void setNoteId(String noteId) {
        this.noteId = noteId;
    }


    Note(@NonNull String title, String body, int position) {
        this.title = title;
        this.body = body;
        this.position = position;
        this.noteId = Utils.getInstance().hashBasedCheck(Utils.getInstance().getTimeRightNow());
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Note))
            return false;
        if(!title.equals(((Note) obj).getTitle()))
            return false;
        if (!body.equals(((Note) obj).getBody()))
            return false;
        if(position != ((Note) obj).getPosition())
            return false;
        if(file != null && ((Note) obj).getFile() == null)
            return false;
        if(file == null && ((Note) obj).getFile() != null)
            return false;
        else if(file != null && ((Note) obj).getFile() != null && !file.equals(((Note) obj).getFile()))
            return false;
        return true;

    }

    public void fillNoteFromNote(Note org){

        this.setTitle(org.getTitle());
        this.setBody(org.getBody());
        this.setEncrypted(org.isEncrypted());
        this.setHidden(org.isHidden());
        this.setFile(org.getFile());
        this.setPosition(org.getPosition());
        this.setTimeStamp(org.getTimeStamp());
        this.setNoteId(org.getNoteId());
        org=null;
    }

    public static Note noteFRomJson (JSONObject org) throws JSONException {

        String title = org.getString("title");
        String body = org.getString("body");
        int position = org.getInt("position");
        String timestamp = org.getString("timestamp");

        Note note =  new Note(title,body,position);
        note.setTimeStamp(timestamp);
        note.setNoteId(org.getString("noteId"));
        note.setEncrypted(org.getBoolean("encrypted"));
        return note;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @NonNull
    public String getTitle() {
        return this.title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    public String getBody() {
        if (body == null)
            return "";
        return this.body;
    }


    String getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public void setEncrypted(boolean encrypted) {
        isEncrypted = encrypted;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public int hashCode (){
        int result  =1 ;
        result = result + Objects.hashCode(title);
        result = result + Objects.hashCode(body);
        result = result + Objects.hashCode(timeStamp);
        return result;
    }
}
