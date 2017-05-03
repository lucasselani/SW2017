package br.sw.cacadoresdelivrosbr.view.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.sw.cacadoresdelivrosbr.R;
import br.sw.cacadoresdelivrosbr.model.Book;
import br.sw.cacadoresdelivrosbr.view.activities.MainActivity;

import static android.app.Activity.RESULT_OK;

/**
 * Created by lucasselani on 29/04/17.
 */

public class BookDialog extends DialogFragment {
    private EditText mBookName, mBookDesc;
    private boolean moreImages = true;
    private File mOutput=null;
    private static final int CAMERA_PIC_REQUEST = 1337;
    private LatLng loc;
    private ShareDialog shareDialog;
    private Thread t;
    private ArrayList<Bitmap> images;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface MDialogListener {
        public void onMDialogPositiveClick(String newValue);
    }

    // Use this instance of the interface to deliver action events
    MDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View modifyView = inflater.inflate(R.layout.book_layout, null);
        builder.setView(modifyView);

        mBookDesc = (EditText) modifyView.findViewById(R.id.bookdescET);
        mBookName = (EditText) modifyView.findViewById(R.id.booknameET);

        builder.setPositiveButton("COMPARTILHAR", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) { }
        });

        builder.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });
        builder.setTitle("Doe um livro");

        images = new ArrayList<>();
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog alertDialog = (AlertDialog) getDialog();
        if(alertDialog != null){
            if(isAdded()) shareDialog = new ShareDialog(getActivity());
            Button positiveButton = alertDialog.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Boolean wantToCloseDialog = true;
                    loc = ((MainActivity)getActivity()).mCurrlatLng;


                    if(mBookName.getText().toString().equals("")){
                        mBookName.setHint("Preencha este campo!");
                        mBookName.setHintTextColor(Color.RED);
                        wantToCloseDialog = false;
                    }
                    if(mBookDesc.getText().toString().equals("")){
                        mBookDesc.setHint("Preencha este campo!");
                        mBookDesc.setHintTextColor(Color.RED);
                        wantToCloseDialog = false;
                    }
                    if(loc == null){
                        Toast.makeText(getActivity().getApplicationContext(),
                                "Não foi possível conseguir a sua localização!",
                                Toast.LENGTH_LONG).show();
                        wantToCloseDialog = false;
                    }

                    if (wantToCloseDialog) {
                        TakePhoto();
                    }
                }
            });
        }
    }

    public String generateRandomId(){
        Long tsLong = System.currentTimeMillis()/1000;
        String timestamp = tsLong.toString();
        String uuid = UUID.randomUUID().toString();
        return "book-"+timestamp+uuid;
    }

    private void TakePhoto(){
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        mOutput = new File(dir, "temp_sharing.jpeg");
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mOutput));
        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_PIC_REQUEST && resultCode == RESULT_OK) {
            try {
                images.add(MediaStore.Images.Media.getBitmap(
                        getActivity().getContentResolver(), Uri.fromFile(mOutput)));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            } finally {
                AddMorePhotos();
            }

            if(!moreImages) dismiss();
        }
    }


    public void AddMorePhotos(){
        new AlertDialog.Builder(getActivity())
                .setTitle("Fotos")
                .setMessage("Deseja enviar mais fotos?")
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        moreImages = true;
                        TakePhoto();
                    }
                })
                .setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        moreImages = false;
                        mBookName.setHintTextColor(Color.GRAY);
                        mBookDesc.setHintTextColor(Color.GRAY);
                        String name = mBookName.getText().toString();
                        String desc = mBookDesc.getText().toString();
                        String bookId = generateRandomId();
                        double lat = loc.latitude;
                        double log = loc.longitude;
                        t = new Thread(new ShareOnFacebook(name,desc,bookId,lat,log));
                        t.start();
                        dismiss();
                    }
                }).create().show();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(t != null) t.interrupt();
    }

    private class ShareOnFacebook implements Runnable{
        String name, desc, bookId;
        double lat, log;

        ShareOnFacebook(String name, String desc, String bookId, double lat, double log){
            this.name=name;
            this.desc = desc;
            this.bookId = bookId;
            this.lat = lat;
            this.log = log;
        }

        @Override
        public void run() {
            if(getActivity() == null){
                t.interrupt();
                return;
            }

            List<SharePhoto> fbImages = new ArrayList<>();

            for(Bitmap image : images){
                SharePhoto sharePhoto = new SharePhoto.Builder()
                        .setBitmap(image)
                        .build();
                fbImages.add(sharePhoto);
            }

            SharePhotoContent content = new SharePhotoContent.Builder()
                    .addPhotos(fbImages)
                    .setShareHashtag(new ShareHashtag.Builder()
                            .setHashtag("#CaçadoresDeLivros")
                            .build())
                    .build();

            if(shareDialog != null) shareDialog.show(content);

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("location");
            GeoFire geoFire = new GeoFire(ref);
            geoFire.setLocation(bookId, new GeoLocation(lat, log));

            Book book = new Book(bookId,name,desc);
            ref = FirebaseDatabase.getInstance().getReference("books");
            ref.child(bookId).setValue(book);

            Uri file = Uri.fromFile(mOutput);
            StorageReference imagesRef = FirebaseStorage.getInstance().getReference().child("images/"+bookId+".jpg");
            imagesRef.putFile(file)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Get a URL to the uploaded content
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle unsuccessful uploads
                            // ...
                        }
                    });
        }
    }
}