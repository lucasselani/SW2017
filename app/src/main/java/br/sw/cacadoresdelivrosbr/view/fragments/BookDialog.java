package br.sw.cacadoresdelivrosbr.view.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import br.sw.cacadoresdelivrosbr.R;
import br.sw.cacadoresdelivrosbr.model.Book;
import br.sw.cacadoresdelivrosbr.view.activities.MainActivity;

/**
 * Created by lucasselani on 29/04/17.
 */

public class BookDialog extends DialogFragment {
    private EditText mBookName;
    private Button mPictureButton;
    private Bitmap image;
    private boolean imageShared = false;
    private static final int CAMERA_PIC_REQUEST = 1337;

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

        mBookName = (EditText) modifyView.findViewById(R.id.bookname);
        mPictureButton = (Button) modifyView.findViewById(R.id.picturebutton);
        mPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
            }
        });

        builder.setPositiveButton("ENVIAR", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) { }
        });

        builder.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });
        builder.setTitle("Doe um livro");

        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog alertDialog = (AlertDialog) getDialog();
        if(alertDialog != null){
            Button positiveButton = alertDialog.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Boolean wantToCloseDialog = true;
                    String name = "";
                    LatLng loc = ((MainActivity)getActivity()).mCurrlatLng;


                    if(mBookName.getText().toString().equals("")){
                        mBookName.setHint("Preencha este campo!");
                        mBookName.setHintTextColor(Color.RED);
                        wantToCloseDialog = false;
                    }
                    if(!imageShared) {
                        Toast.makeText(getActivity().getApplicationContext(),
                                "Adicione uma foto do livro!",
                                Toast.LENGTH_LONG).show();
                        wantToCloseDialog = false;
                    }
                    if(loc == null){
                        Toast.makeText(getActivity().getApplicationContext(),
                                "Não foi possível conseguir a sua localização!",
                                Toast.LENGTH_LONG).show();
                        wantToCloseDialog = false;
                    }

                    if (wantToCloseDialog) {
                        mBookName.setHintTextColor(Color.GRAY);
                        name = mBookName.getText().toString();
                        String bookId = generateRandomId();
                        String imageString = getStringFromBitmap(image);
                        double lat = loc.latitude;
                        double log = loc.longitude;

                        SharePhotoContent content = new SharePhotoContent.Builder()
                                .addPhoto(new SharePhoto.Builder()
                                        .setBitmap(image)
                                        .build())
                                .setShareHashtag(new ShareHashtag.Builder()
                                        .setHashtag("#CaçadoresDeLivros#DoeUmLivro")
                                        .build())
                                .build();

                        ShareDialog shareDialog = new ShareDialog(getActivity());
                        shareDialog.show(content);

                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("location");
                        GeoFire geoFire = new GeoFire(ref);
                        geoFire.setLocation(bookId, new GeoLocation(lat, log));

                        ref = FirebaseDatabase.getInstance().getReference("books");
                        ref.child(bookId).setValue(new Book(bookId,name,imageString));

                        dismiss();
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

    private String getStringFromBitmap(Bitmap bitmapPicture) {
         /*
         * This functions converts Bitmap picture to a string which can be
         * JSONified.
         * */
        final int COMPRESSION_QUALITY = 100;
        String encodedImage;
        ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
        bitmapPicture.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY,
                byteArrayBitmapStream);
        byte[] b = byteArrayBitmapStream.toByteArray();
        encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        return encodedImage;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_PIC_REQUEST) {
            if(data == null) return;
            if(data.hasExtra("data")) image = (Bitmap) data.getExtras().get("data");
            imageShared = true;

        }
    }
}