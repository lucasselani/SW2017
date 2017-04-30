package br.sw.cacadoresdelivros.view.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import br.sw.cacadoresdelivros.R;
import br.sw.cacadoresdelivros.model.Book;
import br.sw.cacadoresdelivros.view.activities.MainActivity;

import static android.R.attr.key;


/**
 * Created by lucasselani on 29/04/17.
 */

public class MarkerDialog extends DialogFragment {
    private TextView mBookName, mBookDescription;
    private ImageView mBookPicture;
    private Bitmap image;
    private boolean imageShared = false;
    private boolean wantToCloseDialog;
    private static final int CAMERA_PIC_REQUEST = 1337;
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
        for(Book b : ((MainActivity)getActivity()).bookList){
            if(b.bookId.equals(markerId)){
                mBookName.setText(b.bookName);
                mBookPicture.setImageBitmap(getBitmapFromString(b.bookImage));
                mBookDescription.setText("Sem descrição.");
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
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
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
        if (requestCode == CAMERA_PIC_REQUEST) {
            if(data == null) return;
            image = (Bitmap) data.getExtras().get("data");
            imageShared = true;
            wantToCloseDialog = true;

            r.run();
            SharePhotoContent content = new SharePhotoContent.Builder()
                    .addPhoto(new SharePhoto.Builder()
                            .setBitmap(image)
                            .build())
                    .setShareHashtag(new ShareHashtag.Builder()
                            .setHashtag("#CaçadoresDeLivros #GanheUmLivro")
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
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        return decodedByte;
    }

}
