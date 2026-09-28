package com.sdstiwi.gatekeeper;

import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.sdstiwi.gatekeeper.R;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

    private List<Room> roomList;

    public RoomAdapter(List<Room> roomList) {
        this.roomList = roomList;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room, parent, false);
        return new RoomViewHolder(view);


    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        Room room = roomList.get(position);
        holder.textViewRoomName.setText(room.getName());
        holder.textViewBuilding.setText("Building: " + room.getBuilding());
        holder.textViewFloor.setText("Floor: " + room.getFloor());

        if (room.getIsActive() == 1) {
            holder.textViewStatus.setText("Status: Authorized");
            holder.textViewStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.textViewStatus.setText("Status: Closed");
            holder.textViewStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
        }
    }




    @Override
    public int getItemCount() {
        return roomList.size();
    }

    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView textViewRoomName, textViewBuilding, textViewFloor,textViewStatus;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewRoomName = itemView.findViewById(R.id.textViewRoomName);
            textViewBuilding = itemView.findViewById(R.id.textViewBuilding);
            textViewFloor = itemView.findViewById(R.id.textViewFloor);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);

        }
    }
}
