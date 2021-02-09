package com.example.painelbt;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private List<MainActivity.MessageInfo> values;

    //Test to allow calls MainActivity's methods from this adapter
    private Context mContext;


//    public class MyAdapter extends RecyclerView.Adapter<com.example.painelbt.MyAdapter.ViewHolder> {
//        private List<String> values;


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView txtHeader;
        public TextView txtFooter;
        public View layout;

        public ViewHolder(View v) {
            super(v);
            layout = v;
            txtHeader = (TextView) v.findViewById(R.id.id_textView);
            txtFooter = (TextView) v.findViewById(R.id.msg_textView);
        }
    }

    //public void add(int position, String item) {
    public void add(int position, MainActivity.MessageInfo item) {
        values.add(position, item);
        notifyItemInserted(position);
    }

    public void remove(int position) {
        values.remove(position);
        notifyItemRemoved(position);

        //teste de remoção
        notifyDataSetChanged();
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    //public MyAdapter(List<String> myDataset) {
    //public MyAdapter(List<MainActivity.MessageInfo> myDataset) {
    public MyAdapter(List<MainActivity.MessageInfo> myDataset, Context context) {
        values = myDataset;
        this.mContext = context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        LayoutInflater inflater = LayoutInflater.from(
                parent.getContext());
        View v =
                inflater.inflate(R.layout.row_layout, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        //final String name = values.get(position); mky
        final String name = values.get(position).msgText;
        final String id = values.get(position).msgID;

        //this is the actual id to delete a message
        //holder.txtHeader.setText(id);

        //this is just to show the sequence in the recyclerView
        holder.txtHeader.setText(String.valueOf(position+1));
        holder.txtFooter.setText(name);
        Log.d("MyADAPTER, position: ", String.valueOf(position));

        holder.txtHeader.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                AlertDialog dialog = new AlertDialog.Builder(v.getContext())
                        //TextView myMsg = new TextView(v.getContext())
                        .setTitle("Remover Mensagem ?")
                        .setMessage("\t" + "\n" + "\"" + name + "\"")

                        //OK button dialog box
                        .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //convert string decimal to int, e.g. "16" to 16 integer
                                int string2IntDecimal = Integer.parseInt(id);
                                //convert decimal to hex, with 2 digits. e.g. decimal 16 to 0F hex string (2 digits)
                                String hexValString = String.format("%04X", string2IntDecimal);
                                String deleteCode = "0E";

                                Log.d("MyADAPTER, id: ", id);
                                Log.d("MyADAPTER, deleteCode: ", deleteCode + hexValString);
                                Log.d("MyADAPTER, mensagem: ", name);
                                Log.d("MyADAPTER, position: ", Integer.toString(position));

                                //Remove the item from the list of RecyclerView
                                remove(position);

                                //Send the delete message to Bluetooth interface, using method in MainActivity
                                ((MainActivity) mContext).sendControlMessageBT(
                                        deleteCode + hexValString);
                            }
                        })

                        //Cancel button dialog box
                        .setNegativeButton("Não", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();


            } //end onClick at element of list
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return values.size();
    }

}