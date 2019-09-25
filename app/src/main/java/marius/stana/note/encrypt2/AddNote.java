package marius.stana.note.encrypt2;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class AddNote extends AppCompatActivity {
    private EditText titleEdit;
    private EditText bodyEdit;
    private NoteDao noteQueryInterface;
    private Note backupNote;
    private String m_Text = "";
    int position;
    boolean reason;
    private FloatingActionButton image = null;
    private boolean isImage = false;
    SwitchCompat  switchBar = null;
    private String currentPhotoPath=null;
    public boolean toBeEncrypted;
    Menu menu;

    public File imageFile;


    private void configureLayout() {
        Objects.requireNonNull(getSupportActionBar()).setHomeAsUpIndicator(R.mipmap.ic_back);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        position = Integer.parseInt(getIntent().getStringExtra("position"));

        titleEdit = findViewById(R.id.editTitle);
        bodyEdit = findViewById(R.id.editBody);

        noteQueryInterface = Utils.getInstance().getNoteQuerryInterfce(this, null);

        if (position == -1) {

            noteQueryInterface.increasePositions(-1);
            noteQueryInterface.insert(new Note("", "", 0));

            position = 0;
            noteQueryInterface.updateIsEncriptedFromPosition(position, Utils.getInstance().isEnc());
            reason = false;

            Objects.requireNonNull(getSupportActionBar()).setTitle("Add Note");

        } else {
            titleEdit.setText(noteQueryInterface.getFromPosition(position).getTitle());
            bodyEdit.setText(noteQueryInterface.getFromPosition(position).decrypt().getBody());
            Objects.requireNonNull(getSupportActionBar()).setTitle("Edit Note");
            reason = true;

        }


        backupNote = noteQueryInterface.getFromPosition(position);
        final Note draftNote = noteQueryInterface.getFromPosition(position);
        titleEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                draftNote.setTitle(titleEdit.getText().toString());
                //draftNote.setTitle(draftNote.encrypt().getTitle());
                noteQueryInterface.update(draftNote);

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        bodyEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                draftNote.setBody(bodyEdit.getText().toString());
                draftNote.setBody(draftNote.encrypt().getBody());
                noteQueryInterface.update(draftNote);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        FloatingActionButton confirm = findViewById(R.id.addConfirm);
        FloatingActionButton cancel = findViewById(R.id.addDiscard);
        FloatingActionButton delete = findViewById(R.id.addDelete);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discard();
            }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delete();
            }
        });
        configureImageView(draftNote);

        if (!reason)
            delete.setEnabled(false);

    }

    private void configureImageView(Note draftNote) {

        image = findViewById(R.id.addPhoto);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();

            }
        });
        findViewById(R.id.viewPhoto).setEnabled(false);
        findViewById(R.id.photoDelete).setEnabled(false);
        findViewById(R.id.viewPhoto).setVisibility(View.INVISIBLE);
        findViewById(R.id.photoDelete).setVisibility(View.INVISIBLE);
        if( null!=draftNote.getFile()) {
            imageFile = new File(draftNote.getFile());
            currentPhotoPath = draftNote.getFile();
        }
        final  Context context = getApplicationContext();
        if(imageFile != null){
            findViewById(R.id.viewPhoto).setEnabled(true);
            findViewById(R.id.photoDelete).setEnabled(true);
            findViewById(R.id.viewPhoto).setVisibility(View.VISIBLE);
            findViewById(R.id.photoDelete).setVisibility(View.VISIBLE);

/*
           findViewById(R.id.viewPhoto).setBackground(FileProvider.getUriForFile(this,
                   "marius.stana.note.encrypt2.fileprovider",
                   imageFile));
           findViewById(R.id.photoDelete).setEnabled(true);
           */
        }

        findViewById(R.id.viewPhoto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                                        FileProvider.getUriForFile(getApplicationContext(),
                                                "marius.stana.note.encrypt2.fileprovider",
                                                imageFile): Uri.fromFile(imageFile),
                                "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }

        });

        findViewById(R.id.photoDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageFile.delete();
                currentPhotoPath=null;
                draftNote.setFile(null);
                noteQueryInterface.update(draftNote);
                findViewById(R.id.viewPhoto).setEnabled(false);
                findViewById(R.id.photoDelete).setEnabled(false);
                findViewById(R.id.viewPhoto).setVisibility(View.INVISIBLE);
                findViewById(R.id.photoDelete).setVisibility(View.INVISIBLE);
            }
        });
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.action_delete);
        if (!reason)
            item.setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        System.out.println(item.getItemId());
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        if (item.getItemId() == R.id.action_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, titleEdit.getText().toString() + "\n" + bodyEdit.getText().toString());
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        }
        if (item.getItemId() == R.id.action_discard) {
           discard();
        }
        if (item.getItemId() == R.id.action_delete) {
            delete();

        }
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        this.menu=menu;


        MenuItem item = menu.findItem(R.id.app_bar_switch);
        item.setActionView(R.layout.switch_item);
        toBeEncrypted =noteQueryInterface.getFromPosition(position).isEncrypted();
        switchBar = item.getActionView().findViewById(R.id.switch1);
        switchBar.setChecked(noteQueryInterface.getFromPosition(position).isEncrypted());
        switchBar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if(b) {
                    switchBar.setText("Decrypt Note");
                    toBeEncrypted=b;
                    //noteQueryInterface.update(draftNote.encrypt());
                    //draftNote.setEncrypted(b);

                } else {
                    switchBar.setText("Encrypt Note");
                    toBeEncrypted=b;
                    //noteQueryInterface.update(draftNote.decrypt());
                    //draftNote.setEncrypted(b);
                }
            }
        });
        if(Utils.getInstance().getPasswd() != null || reason == false)
            switchBar.setVisibility(View.VISIBLE);
        else
            switchBar.setVisibility(View.INVISIBLE);

        return true;
    }



    @Override
    public void onBackPressed() {
        if (reason == false && TextUtils.isEmpty(titleEdit.getText()) && TextUtils.isEmpty(bodyEdit.getText().toString()) && currentPhotoPath==null) {
            // If empty  discards it.
            noteQueryInterface.delete(noteQueryInterface.getFromPosition(position));
            noteQueryInterface.decreasePositions(position);
            Intent intent = new Intent();
            intent.putExtra("pos", -2);
            setResult(0, intent);
            finish();
        } else if (TextUtils.isEmpty(titleEdit.getText()) && TextUtils.isEmpty(bodyEdit.getText().toString()) && currentPhotoPath==null) {
            // if fields are put to empty by user than discard Note
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            noteQueryInterface.delete(noteQueryInterface.getFromPosition(position));
                            noteQueryInterface.decreasePositions(position);
                            Intent intent = new Intent();
                            setResult(0, intent);
                            intent.putExtra("deleted", position);
                            finish();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Do you want to remove this note?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            titleEdit.setText(backupNote.getTitle());
                            bodyEdit.setText(backupNote.decrypt().getBody());
                            switchBar.setChecked(backupNote.isEncrypted());

                        }
                    }).show();
        } else {
            //set Encryption & Date here !!
            if (reason == false) {
                noteQueryInterface.updateTimestampFromPosition(position, "Created on: " + Utils.getInstance().getTimeRightNow());
                noteQueryInterface.updateIsEncriptedFromPosition(position, Utils.getInstance().isEnc() || noteQueryInterface.getFromPosition(position).isEncrypted());
            } else {

                Note draft = noteQueryInterface.getFromPosition(position);
                if(draft.isEncrypted() != toBeEncrypted) {

                    if (toBeEncrypted) {
                        draft.setEncrypted(toBeEncrypted);
                        draft.setBody(draft.encrypt().getBody());
                    }
                    else {
                        draft.setBody(draft.decrypt().getBody());
                        draft.setEncrypted(toBeEncrypted);
                    }


                    noteQueryInterface.update(draft);

                }
                if (!backupNote.equals(noteQueryInterface.getFromPosition(position)))
                    noteQueryInterface.updateTimestampFromPosition(position, "Edited on: " + Utils.getInstance().getTimeRightNow());
                }

            Intent intent = new Intent();
            setResult(0, intent);
            if (!reason)
                intent.putExtra("pos", -1);
            else
                intent.putExtra("pos", position);

            if(currentPhotoPath == null && imageFile !=null)
                imageFile.delete();
            finish();

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);
        configureLayout();
        Utils.getInstance().getCameraPermission(this);
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        if(titleEdit.getText().toString().isEmpty()==false)
            imageFileName=titleEdit.getText().toString();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: file_paths for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        Utils. MY_CAMERA_REQUEST_CODE);
            }
        }



        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
               System.out.println("HERE ");

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "marius.stana.note.encrypt2.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, Utils.REQUEST_IMAGE_CAPTURE);
            }
        }
        /*
          try {
            createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
       // takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoPath);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, Utils.REQUEST_IMAGE_CAPTURE);
            */
        }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Utils.MY_CAMERA_REQUEST_CODE) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();

            } else {

                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();

            }

        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {



        try {
            switch (requestCode) {
                case 1: {
                    if (resultCode == RESULT_OK) {
                        if(imageFile !=null)
                            imageFile.delete();
                        imageFile= new File(currentPhotoPath);
                            Note note = noteQueryInterface.getFromPosition(position);
                            note.setFile(currentPhotoPath);
                            noteQueryInterface.update(note);
                        findViewById(R.id.viewPhoto).setEnabled(true);
                        findViewById(R.id.photoDelete).setEnabled(true);
                        findViewById(R.id.viewPhoto).setVisibility(View.VISIBLE);
                        findViewById(R.id.photoDelete).setVisibility(View.VISIBLE);
                            galleryAddPic();

                    }
                    break;
                }
            }

        } catch (Exception error) {
            error.printStackTrace();
        }

/*
        if (requestCode == Utils.REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            File imgFile = new  File(currentPhotoPath);
            if(imgFile.exists())            {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");



                image.setImageBitmap(imageBitmap);

                Uri u = data.getData();
        Note note = noteQueryInterface.getFromPosition(position);
        note.setFile(u.getPath());
                noteQueryInterface.update(note);
            }

            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            image.setImageBitmap(imageBitmap);
            Toast.makeText(getApplicationContext(), "Image Selected", Toast.LENGTH_SHORT).show();

        }
        */
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                FileProvider.getUriForFile(this,
                        "marius.stana.note.encrypt2.fileprovider",
                        imageFile): Uri.fromFile(imageFile);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
    public void delete(){
        if (reason == true) {
            titleEdit.setText("");
            bodyEdit.setText("");
            onBackPressed();
        }
    }

    public void discard(){
        if (reason == false) {
            titleEdit.setText("");
            bodyEdit.setText("");
            onBackPressed();
        } else {

            titleEdit.setText(backupNote.getTitle());
            bodyEdit.setText(backupNote.decrypt().getBody());
            switchBar.setChecked(backupNote.isEncrypted());
            onBackPressed();
        }
    }
}
