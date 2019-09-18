package marius.stana.note.encrypt2;

import android.Manifest;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
    private ImageView image = null;
    private boolean isImage = false;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private String currentPhotoPath;

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
        final Note draftNote = noteQueryInterface.getFromPosition(position);
        backupNote = noteQueryInterface.getFromPosition(position);
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

        ImageButton confirm = findViewById(R.id.addConfirm);
        ImageButton cancel = findViewById(R.id.addDiscard);

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        configureImageView();
    }

    private void configureImageView() {

        image = findViewById(R.id.addPhoto);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();

            }
        });

    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_delete);
        if (!reason)
            item.setVisible(false);
        super.onPrepareOptionsMenu(menu);
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
            if (reason == false) {
                titleEdit.setText("");
                bodyEdit.setText("");
                onBackPressed();
            } else {

                titleEdit.setText(backupNote.getTitle());
                bodyEdit.setText(backupNote.decrypt().getBody());
                onBackPressed();
            }
        }
        if (item.getItemId() == R.id.action_delete) {
            noteQueryInterface.delete(noteQueryInterface.getFromPosition(position));
            noteQueryInterface.decreasePositions(position);
            onBackPressed();
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }


    @Override
    public void onBackPressed() {
        if (reason == false && TextUtils.isEmpty(titleEdit.getText()) && TextUtils.isEmpty(bodyEdit.getText().toString())) {
            // If empty  discards it.
            noteQueryInterface.delete(noteQueryInterface.getFromPosition(position));
            noteQueryInterface.decreasePositions(position);
            Intent intent = new Intent();
            intent.putExtra("pos", -2);
            setResult(0, intent);
            finish();
        } else if (TextUtils.isEmpty(titleEdit.getText()) && TextUtils.isEmpty(bodyEdit.getText().toString())) {
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
                            intent.putExtra("pos", position);
                            finish();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Do you want to remove this note?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        } else {
            //set Encryption & Date here !!
            if (reason == false) {
                noteQueryInterface.updateTimestampFromPosition(position, "Created on: " + Utils.getInstance().getTimeRightNow());
                noteQueryInterface.updateIsEncriptedFromPosition(position, Utils.getInstance().isEnc());
            } else {
                noteQueryInterface.updateTimestampFromPosition(position, "Edited on: " + Utils.getInstance().getTimeRightNow());
                //noteQueryInterface.updateIsEncriptedFromPosition(position, Utils.getInstance().isEnc());
            }
            Intent intent = new Intent();
            setResult(0, intent);
            if (!reason)
                intent.putExtra("pos", -1);
            else
                intent.putExtra("pos", position);
            finish();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);
        Log.d("AddNote:onCreate", "Create/Edit a note that is on position:" + position);
        configureLayout();





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

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    private void dispatchTakePictureIntent() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        MY_CAMERA_REQUEST_CODE);
            }
        }
        try {
            createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoPath);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }


    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_REQUEST_CODE) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();

            } else {

                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();

            }

        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            File imgFile = new  File(currentPhotoPath);
            if(imgFile.exists())            {
                image.setImageURI(Uri.fromFile(imgFile));
            }
            /*
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            image.setImageBitmap(imageBitmap);
            Toast.makeText(getApplicationContext(), "Image Selected", Toast.LENGTH_SHORT).show();
            */
        }
    }
}
