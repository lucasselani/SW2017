package br.sw.cacadoresdelivrosbr.controller;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import br.sw.cacadoresdelivrosbr.R;

/**
 * Created by lucasselani on 30/04/17.
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.TipViewHolder> {

    public List<String> genres;

    public static class TipViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView textView;

        public TipViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.cardView);
            textView = (TextView) itemView.findViewById(R.id.genreTextView);
        }
    }

    public RecyclerViewAdapter(Context ctx) {
        genres = new ArrayList<>();
        //TODO

    }

    @Override
    public TipViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewTip) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.genre_card_inflate, viewGroup, false);
        TipViewHolder tipViewHolder = new TipViewHolder(v);
        return tipViewHolder;
    }

    @Override
    public void onBindViewHolder(TipViewHolder holder, int position) {
        if (genres.size() == 0) {
            holder.textView.setText("Nenhum gênero cadastrado!");
            holder.cardView.setCardBackgroundColor(Color.WHITE);
        } else {
            holder.textView.setText(genres.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return genres.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }
}

