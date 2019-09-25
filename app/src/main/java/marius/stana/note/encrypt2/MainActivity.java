package marius.stana.note.encrypt2;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.fingerprint.FingerprintManager;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class MainActivity extends AppCompatActivity implements CustomAdapter.ItemClickListener {
    CustomAdapter adapter;
    private long mLastClickTime = 0;
    ConfigureRecycler recycleView;
    private String pass;
    FingerprintManagerCompat fingerprintManager = BiometricUtils.getFingerprintManagerCompat(this);
    private MenuItem fg;
    SearchView searchView;

    public void getPassword(final String information, final MenuItem item, final String action,Integer position) {

        final EditText input = Utils.getInstance().getEditText(this);
        final AlertDialog.Builder builder = Utils.getInstance().getAlertBox(this,information,input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                invalidateOptionsMenu();
                pass = input.getText().toString();
                NoteDao n = Utils.getInstance().getNoteQuerryInterfce(getApplicationContext(),null);

                if (action.equals("check_edit")) {
                    if (n.getFromPosition(-1).getBody().equals(Utils.getInstance().hashBasedCheck(pass))) {
                        Utils.getInstance().setPasswd(pass);
                        Intent add = new Intent(MainActivity.this, AddNote.class);
                        add.putExtra("position", String.valueOf(position)); //open for add ?
                        startActivityForResult(add, 0);


                    } else {
                        getPassword("Wrong password, please try again", item, action,position);
                        builder.setTitle("Wrong pass");
                    }
                }

                if (action.equals("check_menu")) {
                    if (n.getFromPosition(-1).getBody().equals(Utils.getInstance().hashBasedCheck(pass))) {
                        Utils.getInstance().setPasswd(pass);
                        Utils.getInstance().setEnc(true);

                    } else {
                        getPassword("Wrong password, please try again", item, action,position);
                        builder.setTitle("Wrong pass");
                    }
                    item.setIcon(R.drawable.ic_no_encryption_black_24dp);
                    item.setTitle("Disable encryption");
                }

                if (action.equals("enc")) {
                    Utils.getInstance().setEnc(true);
                    Utils.getInstance().setPasswd(pass);
                    n.insert(new Note("enc", Utils.getInstance().hashBasedCheck(pass), -1));
                    Utils.getInstance().setEncFieldSet(true);
                    Utils.getInstance().setEnc(true);
                    item.setIcon(R.drawable.ic_no_encryption_black_24dp);
                    item.setTitle("Disable encryption");
                    try {
                        //encryptAll(n);w
                        Toast.makeText(getApplicationContext(), "Encryption enabled", Toast.LENGTH_SHORT).show();
                    } catch (Exception ignore) {}
                }

            }


        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

            }
        });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

            }
        });

        builder.show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        NoteDao n = Utils.getInstance().getNoteQuerryInterfce(this,null);


        if(data.getIntExtra("deleted",-1)>=0){
            adapter.notifyItemRemoved(data.getIntExtra("deleted",0));
            recycleView.getNotesList().getRecycledViewPool().clear();
            return;
        }
        if (requestCode == 0) {//edit/add/deleted
            recycleView.getNotesList().getRecycledViewPool().clear();
            if (data.getIntExtra("pos", -2) == -2)//do not add note
                return;
            if (data.getIntExtra("pos", -2) == -1)//item added
            {
                recycleView.getNotesList().smoothScrollToPosition(0);
                adapter.notifyItemInserted(0);

                n.search("%" + searchView.getQuery().toString() + "%");
            } else {
                System.out.print("Here");
                adapter.notifyItemChanged(data.getIntExtra("pos", -2));
            }


        }
        if (requestCode == 1) {
            if (data.getIntExtra("code", -2) == 1) {
                recycleView.getNotesList().setVisibility(View.VISIBLE);
                findViewById(R.id.addBtn).setVisibility(View.VISIBLE);
                findViewById(R.id.imageView).setVisibility(View.INVISIBLE);
                findViewById(R.id.textView3).setVisibility(View.INVISIBLE);
                findViewById(R.id.usePin).setVisibility(View.INVISIBLE);
                InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                //Find the currently focused view, so we can grab the correct window token from it.
                View view = this.getCurrentFocus();
                //If no view currently has focus, create a new one, just so we can grab a window token from it
                if (view == null) {
                    view = new View(this);
                }
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                invalidateOptionsMenu();

            }
            if (data.getIntExtra("code", -2) == 0) {
                SharedPreferences prefs = this.getSharedPreferences(
                        "marius.stana.note.encrypt2", Context.MODE_PRIVATE);
                prefs.edit().putBoolean("finger", true).apply();
                fg.setTitle("Disable encryption");
                Toast.makeText(getApplicationContext(), "Fingerprint enabled", Toast.LENGTH_SHORT).show();
            }
            if (data.getIntExtra("code", -2) == -1) {

                Toast.makeText(getApplicationContext(), "Fingerprint not enabled", Toast.LENGTH_SHORT).show();
            }
        }
        //Qr ?
        if (requestCode == 2) {
            if (resultCode == 2)
                return;
            if (resultCode == 1) {
                final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Error");
                alertDialog.setMessage("Wrong QR code. Please try again");
                alertDialog.setButton(android.app.AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialog.show();
            }

            String notes = data.getStringExtra("notes");
            String pwd = data.getStringExtra("pwd");
            IvParameterSpec iv = new IvParameterSpec(Utils.getInstance().hashBasedCheck(String.valueOf("a"))
                    .substring(48)
                    .getBytes(StandardCharsets.UTF_8));
            notes = Utils.getInstance().decrypt(iv, notes, pwd);
            try {
                JSONArray notesArr = new JSONArray(notes);
                for (int i = 0; i < notesArr.length(); i++) {
                    JSONObject jsonNote = notesArr.getJSONObject(i);
                    n.increasePositions(-1);
                    n.insert(new Note(jsonNote.getString("title"), jsonNote.getString("body"), 0));
                    recycleView.notesList.getRecycledViewPool().clear();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Successfully imported", Toast.LENGTH_SHORT).show();

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        NoteDao n =Utils.getInstance().getNoteQuerryInterfce(this,null);

        System.out.println(item.getItemId());
        //send to device
        if (item.getItemId() == R.id.action_send) {
            List<Note> notesList = new ArrayList<>();
            if (Utils.getInstance().isEnc()) {
                for (int i = 0; i < n.getNotes().size() - 1; i++) {
                    notesList.add(n.getFromPosition(i).decrypt());
                }
            } else {
                notesList = n.getNotes();
            }
            String notes = new Gson().toJson(notesList);
            Intent intent = new Intent(MainActivity.this, QRActivity.class);
            intent.putExtra("N", notes);
            startActivity(intent);
        }

        if (item.getItemId() == R.id.action_receive) {
            Intent intent = new Intent(MainActivity.this, ReceiveActivity.class);
            startActivityForResult(intent, 2);

        }
        if (item.getItemId() == R.id.action_toggle_encryption) {
            if (Utils.getInstance().isEnc()==false) {

                if(!Utils.getInstance().isEncFieldSet())
                    getPassword("Please set the new encryption password", item, "enc",null);
                else
                    getPassword("Enter encryption password", item, "check_menu",null);

                recycleView.notesList.getRecycledViewPool().clear();
                adapter.notifyDataSetChanged();
            }
            if (Utils.getInstance().isEnc()) {
                item.setIcon(R.drawable.ic_enhanced_encryption_black_24dp);
                item.setTitle("Enable encryption");
                Utils.getInstance().setEnc(false);
                recycleView.notesList.getRecycledViewPool().clear();
                adapter.notifyDataSetChanged();
                Toast.makeText(this, "Encryption disabled", Toast.LENGTH_SHORT).show();


            }
        }

        if (item.getItemId() == R.id.action_toggle_fingerprint) {
            if (!BiometricUtils.checkFinger(this)) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Your device does not have a fingerprint sensor.")
                        .setCancelable(true)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        });

                AlertDialog alert = builder.create();
                alert.show();
            } else {
                if (item.getTitle().toString().equals("Enable fingerprint")) {

                    fg = item;
                    Intent add = new Intent(MainActivity.this, PinActivity.class);
                    add.putExtra("check", 0); //open for add
                    startActivityForResult(add, 1);


                } else {
                    SharedPreferences prefs = this.getSharedPreferences(
                            "marius.stana.note.encrypt2", Context.MODE_PRIVATE);
                    prefs.edit().putBoolean("finger", false).apply();
                    Toast.makeText(getApplicationContext(), "Fingerprint disabled", Toast.LENGTH_SHORT).show();
                    item.setTitle("Enable fingerprint");

                }
            }

        }
        return true;

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_toggle_encryption);
        if (Utils.getInstance().isEnc()) {
            item.setTitle("Disable encryption");
            item.setIcon(R.drawable.ic_no_encryption_black_24dp);
        }

        item = menu.findItem(R.id.action_toggle_fingerprint);
        SharedPreferences prefs = Utils.getInstance().getSharedPrefs(this);
        if (prefs.getBoolean("finger", false))
            item.setTitle("Disable fingerprint");
        if (findViewById(R.id.notesList).getVisibility() == View.INVISIBLE) {
            menu.findItem(R.id.action_toggle_encryption).setVisible(false);
            menu.findItem(R.id.action_toggle_fingerprint).setVisible(false);
        } else {
            menu.findItem(R.id.action_toggle_encryption).setVisible(true);
            menu.findItem(R.id.action_toggle_fingerprint).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.m_menu, menu);
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                return false;
            }
        });
        searchView.setSubmitButtonEnabled(false);
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                try {
                    adapter.getFilter().filter(query);
                } catch (Exception ignore) {
                }
                return true;
            }
        });
        return true;
    }

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()) {
            searchView.setQuery("", false);
            searchView.clearFocus();
            searchView.setIconified(true);
        } else {
            super.onBackPressed();
        }
    }

    //confiugres activity layout
    private void configureLayout() {
        //sets the use pin button when fingerprint is enabled
        Button usePin = findViewById(R.id.usePin);
        usePin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent add = new Intent(MainActivity.this, PinActivity.class);
                add.putExtra("check", 2); //open for add
                startActivityForResult(add, 1);
            }
        });

        recycleView= new ConfigureRecycler(this);//populates recycle

        adapter = recycleView.getAdapter();
        FloatingActionButton addBtn = findViewById(R.id.addBtn);
        //prevents doubletouch
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                Intent add = new Intent(MainActivity.this, AddNote.class);
                add.putExtra("position", "-1"); //open for add
                startActivityForResult(add, 0);

            }
        });
        SharedPreferences prefs = Utils.getInstance().getSharedPrefs(this);
        if (prefs.getBoolean("finger", false)) {
            final FingerprintHandler fph = new FingerprintHandler(this, (TextView) findViewById(R.id.textView3), (ImageView) findViewById(R.id.imageView), recycleView.notesList);
            // We are ready to set up the cipher and the key
            try {
                generateKey();
                Cipher cipher = generateCipher();
                FingerprintManagerCompat.CryptoObject cryptoObject =
                        new FingerprintManagerCompat.CryptoObject(Objects.requireNonNull(cipher));
                fph.doAuth(BiometricUtils.getFingerprintManagerCompat(this), cryptoObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
            SharedPreferences.Editor preferencesEditor = prefs.edit();
        preferencesEditor.putBoolean("screenFlip", false);
            preferencesEditor.apply();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NoteDao noteQuerryInterface = Utils.getInstance().getNoteQuerryInterfce(this,null  );
        //String notes = new Gson().toJson(n.getNotes());

        noteQuerryInterface.showAll();
        SharedPreferences prefs = Utils.getInstance().getSharedPrefs(this);

       if(prefs.getBoolean("passSet",false)){
            if (noteQuerryInterface.getFromPosition(-1).getTitle().equals("enc"))
            Utils.getInstance().setEncFieldSet(true);

        } else
        {
            Utils.getInstance().setEncFieldSet(false);
        }
       if(savedInstanceState != null)
            Utils.getInstance().setEnc((savedInstanceState.getBoolean("Enc") || prefs.getBoolean("Enc",false)) && Utils.getInstance().getPasswd() !=null);
       else{

           Utils.getInstance().setEnc( prefs.getBoolean("Enc",false)  && Utils.getInstance().getPasswd() !=null);
       }
            configureLayout();


    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor preferencesEditor = Utils.getInstance().getSharedPrefsEditor(this);
        preferencesEditor.putBoolean("passSet", Utils.getInstance().isEncFieldSet());
        preferencesEditor.putBoolean("Enc", Utils.getInstance().isEnc());
        preferencesEditor.apply();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        SharedPreferences.Editor preferencesEditor = Utils.getInstance().getSharedPrefsEditor(this);
        preferencesEditor.remove("Enc");
        preferencesEditor.apply();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("Enc",Utils.getInstance().isEnc());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        SharedPreferences.Editor preferencesEditor = Utils.getInstance().getSharedPrefsEditor(this);
        if(newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation==Configuration.ORIENTATION_PORTRAIT ) {
            Log.d("Orientation","Changed");
            preferencesEditor.putBoolean("screenFlip", true);
        }
        preferencesEditor.apply();
    }

    @Override
    public void onItemClick(int position) {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();

        if (Utils.getInstance().getNoteQuerryInterfce(this, null).getFromPosition(position).isEncrypted()) {
            if (Utils.getInstance().isEnc() == false)
                getPassword(getString(R.string.pass_solicitation), null, "check_edit", position);
            else{
                Intent add = new Intent(MainActivity.this, AddNote.class);
                add.putExtra("position", String.valueOf(position)); //open for add ?
                startActivityForResult(add, 0);
            }
        } else{

            Intent add = new Intent(MainActivity.this, AddNote.class);
             add.putExtra("position", String.valueOf(position)); //open for add ?
             startActivityForResult(add, 0);
      }
        // Toast.makeText(this, "You clicked " + adapter.getItem(position) + " on row number " + position, Toast.LENGTH_SHORT).show();

    }

    //Utils

    KeyStore keyStore;
    KeyGenerator keyGenerator;

    private void generateKey() {
        try {
            // Get the reference to the key store
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            // Key generator to generate the key
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore");
            keyStore.load(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                keyGenerator.init(new
                        KeyGenParameterSpec.Builder("a",
                        KeyProperties.PURPOSE_ENCRYPT |
                                KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setUserAuthenticationRequired(true)
                        .setEncryptionPaddings(
                                KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .build());
            }
            keyGenerator.generateKey();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private Cipher generateCipher() {
        try {
            Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            SecretKey key = (SecretKey) keyStore.getKey("a",
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher;
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }

    }


}

