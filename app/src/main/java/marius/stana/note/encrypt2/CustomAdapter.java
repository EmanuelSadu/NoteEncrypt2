package marius.stana.note.encrypt2;

import android.app.Activity;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.logging.Logger;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> implements Filterable, ItemTouchHelperAdapter {
    private final CoordinatorLayout snackLay;
    private final RecyclerView rView;
    private NoteDao notes;
    private List<Note> filtered;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private List<Note> filteredList;
    private Activity activity;

    CustomAdapter(RecyclerView rView, CoordinatorLayout snackLay, Context context, NoteDao data) {
        this.mInflater = LayoutInflater.from(context);
        this.notes = data;
        this.snackLay = snackLay;
        this.rView = rView;

    }

    // inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.custom_layout, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Note currentNote = notes.getFromPosition(position);
        if (!currentNote.isHidden()) {
            Log.d("CA:onBindViewHolder","Position is "+position );
            holder.titleTextView.setVisibility(View.VISIBLE);
            holder.bodyTextView.setVisibility(View.VISIBLE);
            holder.line.setVisibility(View.VISIBLE);
            holder.titleTextView.setText(currentNote.getTitle());
            holder.bodyTextView.setText(currentNote.getTimeStamp());



            if(currentNote.isEncrypted()) {

                holder.bodyTextView.setTextColor(Color.RED);

              //  holder.line.setBackgroundColor(R.color.colorEncrypted);
            }
            else{
                holder.bodyTextView.setTextColor(Color.BLACK);
            }
        } else {
            holder.titleTextView.setVisibility(View.GONE);
            holder.bodyTextView.setVisibility(View.GONE);
            holder.line.setVisibility(View.GONE);
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
            return notes.getNotes().size();
    }
    //change order in sqlite when dragging items
    public void onItemMove(int fromPosition, int toPosition) {
        if (notes.getHidden().size()==0) {
            Note tmp = notes.getFromPosition(fromPosition);
            notes.delete(notes.getFromPosition(fromPosition));
            tmp.setPosition(toPosition);
            Note tmp0 = notes.getFromPosition(toPosition);
            notes.delete(notes.getFromPosition(toPosition));
            tmp0.setPosition(fromPosition);
            notes.insert(tmp);
            notes.insert(tmp0);
        }

        notifyItemMoved(fromPosition, toPosition);
    }
    //on swipe item will be set as backup and restored if undo is hit
    public void onItemDismiss(final int position) {
        final Note backup = notes.getFromPosition(position);
        checkPass("Enter passoword to confirm deleting",backup);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();

                notes.showAll();

            if(charString.isEmpty() == false)
                for(int i=0;i<notes.getNotes().size();i++) {
                        if( notes.getFromPosition(i).isEncrypted() && Utils.getInstance().isEnc())
                        {
                            if (!notes.getFromPosition(i).decrypt().getBody().contains(charString) || !notes.getFromPosition(i).getTitle().contains(charString));
                                notes.setHidden(notes.getFromPosition(i).getPosition());
                        }else
                            {
                                if (!notes.getFromPosition(i).getTitle().contains(charString))
                                {
                                    notes.setHidden(notes.getFromPosition(i).getPosition());
                                }
                                if (notes.getFromPosition(i).isEncrypted() ==false && !notes.getFromPosition(i).getBody().contains(charString))
                                {
                                    notes.setHidden(notes.getFromPosition(i).getPosition());
                                }

                        }

                }
            else
                notes.showAll();;
                //notes.search(charString);

                FilterResults filterResults = new FilterResults();
                filterResults.values = filtered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                // refresh the list with filtered data
                rView.getRecycledViewPool().clear();
                notifyDataSetChanged();
            }
        }

                ;
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView titleTextView;
        TextView bodyTextView;
        View line;

        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.textView);
            bodyTextView = itemView.findViewById(R.id.textView2);
            line = itemView.findViewById(R.id.line);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(getAdapterPosition());
        }


    }

    // convenience method for getting data at click position

    Note getItem(int id) {
        return notes.getFromPosition(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }


    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(int position);

    }


    public void checkPass(final String information, Note backup) {
        Log.d("CA:checkPassDel","delete enc");
       Integer position = backup.getPosition();
        if(backup.isEncrypted()== true && Utils.getInstance().isEnc() == false) {

            final EditText input = Utils.getInstance().getEditText(activity);
            final AlertDialog.Builder builder = Utils.getInstance().getAlertBox(activity, information, input);
            boolean notCanceled = true;
            builder.setTitle("Check password");
            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //invalidateOptionsMenu();
                    String pass = input.getText().toString();
                    //n= Utils.getInstance().getNoteQuerryInterfce(getApplicationContext(),null);
                    if (notes.getFromPosition(-1).getBody().equals(Utils.getInstance().hashBasedCheck(pass))) {
                        Utils.getInstance().setPasswd(pass);
                        deleteNote(backup,position);

                    } else {
                        checkPass("Wrong password, please try again", backup);
                        builder.setTitle("Wrong pass");
                    }


                }


            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    rView.getRecycledViewPool().clear();
                    notifyItemChanged(position);
                    dialog.cancel();

                }
            });
            builder.show();


        } else {
            deleteNote(backup, position);
        }
    }

    public void deleteNote(Note backup, Integer position){
        notes.delete(notes.getFromPosition(position));
        notes.decreasePositions(position);
        notifyItemRemoved(position);
        rView.getRecycledViewPool().clear();
        Snackbar snackbar = Snackbar
                .make(snackLay, "Note deleted", Snackbar.LENGTH_LONG).setActionTextColor(Color.WHITE)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        notes.increasePositions(position - 1);
                        notes.insert(backup);
                        rView.getRecycledViewPool().clear();

                        notifyItemInserted(position);
                        rView.getRecycledViewPool().clear();
                        notifyItemChanged(position);
                    }
                });

        snackbar.show();
        snackbar.show();
    }

    public void setActivity(Activity activity){
        this.activity = activity;
    }

}