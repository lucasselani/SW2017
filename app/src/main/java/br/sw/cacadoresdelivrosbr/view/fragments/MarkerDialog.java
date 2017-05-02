package br.sw.cacadoresdelivrosbr.view.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareButton;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import br.sw.cacadoresdelivrosbr.R;
import br.sw.cacadoresdelivrosbr.model.Book;
import br.sw.cacadoresdelivrosbr.view.activities.MainActivity;

import static android.app.Activity.RESULT_OK;


/**
 * Created by lucasselani on 29/04/17.
 */

public class MarkerDialog extends DialogFragment {
    private TextView mBookName, mBookDescription;
    private ImageView mBookPicture;
    private Bitmap image;
    private File mOutput;
    private boolean imageShared = false;
    private boolean wantToCloseDialog;
    private static final int CAMERA_PIC_REQUEST = 1;
    private String markerId;

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
        final View modifyView = inflater.inflate(R.layout.marker_layout, null);
        builder.setView(modifyView);

        mBookName = (TextView) modifyView.findViewById(R.id.bookname);
        mBookPicture = (ImageView) modifyView.findViewById(R.id.bookimage);
        mBookDescription = (TextView) modifyView.findViewById(R.id.bookdesc);

        markerId = ((MainActivity)getActivity()).markerToDelete.getTitle();
        ArrayList<Book> books = ((MainActivity)getActivity()).bookList;
        for(Book b : books){
            if(b.bookId.equals(markerId)){
                mBookName.setText(b.bookName);
                mBookPicture.setImageBitmap(getBitmapFromString(b.bookImage));
                mBookDescription.setText(b.bookDesc);
                break;
            }
        }

        builder.setPositiveButton("PEGAR!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) { }
        });

        builder.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });

        builder.setTitle("Ganhe um livro");
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
                    wantToCloseDialog = false;

                    LatLng markerPos = ((MainActivity)getActivity()).markerToDelete.getPosition();
                    Location loc1 = new Location("");
                    loc1.setLatitude(((MainActivity)getActivity()).mCurrlatLng.latitude);
                    loc1.setLongitude(((MainActivity)getActivity()).mCurrlatLng.longitude);
                    Location loc2 = new Location("");
                    loc2.setLatitude(markerPos.latitude);
                    loc2.setLongitude(markerPos.longitude);
                    float distanceInMeters = loc1.distanceTo(loc2);
                    if(distanceInMeters < 100){
                        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                        mOutput = new File(dir, "temp_sharing.jpeg");
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mOutput));
                        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
                    } else{
                        Toast.makeText(getActivity().getApplicationContext(),
                                "É preciso estar próximo ao livro para pegá-lo!",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    Runnable r = new Runnable() {
        @Override
        public void run() {
            DatabaseReference refBooks = FirebaseDatabase.getInstance().getReference("books");
            refBooks.child(markerId).removeValue();
            DatabaseReference refLocations = FirebaseDatabase.getInstance().getReference("location");
            refLocations.child(markerId).removeValue();
            ((MainActivity)getActivity()).deleteMarker();
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_PIC_REQUEST && resultCode == RESULT_OK) {
            try {
                image = MediaStore.Images.Media.getBitmap(
                        getActivity().getContentResolver(), Uri.fromFile(mOutput));
            } catch (IOException e) {
                imageShared = false;
                e.printStackTrace();
                return;
            }
            imageShared = true;
            wantToCloseDialog = true;

            r.run();
            SharePhotoContent content = new SharePhotoContent.Builder()
                    .addPhoto(new SharePhoto.Builder()
                            .setBitmap(image)
                            .build())
                    .setShareHashtag(new ShareHashtag.Builder()
                            .setHashtag("#CaçadoresDeLivros#GanheUmLivro")
                            .build())
                    .build();

            ShareDialog shareDialog = new ShareDialog(this);
            shareDialog.show(content);

            dismiss();
        }
    }

    private Bitmap getBitmapFromString(String jsonString) {
        /*
        * This Function converts the String back to Bitmap
        * */
        byte[] decodedString = Base64.decode(jsonString, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

}
