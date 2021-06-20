package com.lampu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.lampu.R;
import com.lampu.model.Memory;

import org.w3c.dom.Text;

import java.util.ArrayList;

public abstract class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder> implements ItemMoveCallback.ItemTouchHelperContract  {
    private ArrayList<Memory> memory = new ArrayList<>();
    private Context mContext;

    public MemoryAdapter(Context context, ArrayList<Memory> memory) {
        mContext = context;
        this.memory = memory;
    }

    @Override
    public MemoryAdapter.MemoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_devices, parent, false);
        MemoryViewHolder viewHolder = new MemoryViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(MemoryAdapter.MemoryViewHolder holder, int position) {
        holder.bindMemory(memory.get(position), position);
    }

    @Override
    public int getItemCount() {
        return memory.size();
    }

    public class MemoryViewHolder extends RecyclerView.ViewHolder {

        private Context mContext;
        private TextView txtName;
        private TextView txtLength;
        private ImageView imgMenu;

        public MemoryViewHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();
            txtName = (TextView) itemView.findViewById(R.id.txtName);
            txtLength = (TextView) itemView.findViewById(R.id.txtLength);
            imgMenu = (ImageView) itemView.findViewById(R.id.img_menu);
        }

        public void bindMemory(Memory memory, int position) {

            try {
                txtName.setText(memory.getName());
                txtLength.setText(memory.getLength() + " bit");

                imgMenu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onMenuClick(memory, view);

                    }
                });

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onSelectMemory(position);
                    }
                });
            } catch (Exception e) {

            }


        }
    }

    public abstract void onSelectMemory(int position);
    public abstract void onMenuClick(Memory memory, View view);

}