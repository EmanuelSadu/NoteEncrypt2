package marius.stana.note.encrypt2;

import android.Manifest;
import android.app.Activity;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.widget.EditText;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Calendar;
import java.util.Date;

class Utils {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int MY_CAMERA_REQUEST_CODE = 100;

    private boolean enc;
    private boolean isEncFieldSet;
    private static final Utils ourInstance = new Utils();

    private static SimpleDateFormat simpleDateFormat= new SimpleDateFormat("EEE, d MM yyyy hh:mm aaa");
    private String passwd=null;

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public String encrypt(IvParameterSpec iv, String text, String passwd) {

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");

            byte[] bytes = passwd.getBytes();
            digest.update(bytes, 0, bytes.length);
            byte[] key = digest.digest();
            SecretKeySpec spec = new SecretKeySpec(key, "AES");
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            c.init(Cipher.ENCRYPT_MODE, spec, iv);
            return Base64.encodeToString(c.doFinal(text.getBytes()),Base64.DEFAULT);

        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String decrypt(IvParameterSpec iv, String data, String passwd) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = passwd.getBytes();
            digest.update(bytes, 0, bytes.length);
            byte[] key = digest.digest();
            SecretKeySpec spec = new SecretKeySpec(key, "AES");
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            c.init(Cipher.DECRYPT_MODE, spec, iv);
            return new String(c.doFinal(Base64.decode(data, Base64.DEFAULT)));
        }
        catch (NoSuchAlgorithmException e) {

        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isEnc() {
        return enc;
    }

    public void setEnc(boolean enc) {
        this.enc = enc;
    }

    public String hashBasedCheck(String data) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(data.getBytes());
        return bytesToHex(md.digest()).substring(64);
    }

    public String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte byt : bytes)
            result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    static Utils getInstance() {
        return ourInstance;
    }

    private Utils() {

    }


    public NoteDao getNoteQuerryInterfce(Context activity,String db){
        if(db == null)
            db = "db-contacts2";
        AppDatabase database = Room.databaseBuilder(activity, AppDatabase.class, db)
                .allowMainThreadQueries()   //Allows room to do operation on main thread
                .build();
        return  database.getNoteDao();
    }

    public String getTimeRightNow(){
        Date now = Calendar.getInstance().getTime();
       return simpleDateFormat.format(now);
    }


    public boolean isEncFieldSet() {
        return isEncFieldSet;
    }

    public void setEncFieldSet(boolean encFieldSet) {
        isEncFieldSet = encFieldSet;
    }



    public  EditText getEditText(Activity activity){
        final EditText input = new EditText(activity);
        input.setTransformationMethod(new PasswordTransformationMethod());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return input;
    }

    public AlertDialog.Builder getAlertBox(Activity activity,String information,final EditText input) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Password");
        builder.setCancelable(false);
        builder.setMessage(information);

        // Set up the input

        builder.setView(input);

        return builder;
    }

    public SharedPreferences getSharedPrefs(Context activity){
        return  activity.getSharedPreferences(
                "marius.stana.note.encrypt2", Context.MODE_PRIVATE);
    }

    public SharedPreferences.Editor getSharedPrefsEditor(Context activity){
        return  activity.getSharedPreferences(
                "marius.stana.note.encrypt2", Context.MODE_PRIVATE).edit();
    }

    public void getCameraPermission(Activity app){
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
        }
        else {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (app.checkSelfPermission(Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    app.requestPermissions(new String[]{Manifest.permission.CAMERA},
                            MY_CAMERA_REQUEST_CODE);
                }
            }

        }
    }

}
