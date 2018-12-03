package ch.epfl.sweng.favors.favors;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ch.epfl.sweng.favors.R;
import ch.epfl.sweng.favors.database.Favor;
import ch.epfl.sweng.favors.database.ObservableArrayList;

public class NotificationListAdapter extends RecyclerView.Adapter<NotificationListAdapter.NotificationViewHolder>{
    private ObservableArrayList<Notification> notificationList;
    private static final String TAG = "NOTIFICATION_ADAPTER_LIST";

    public class NotificationViewHolder extends RecyclerView.ViewHolder{
        public TextView notificationText;
        public NotificationListAdapter adapter;

        public NotificationViewHolder(View itemView, NotificationListAdapter adapter) {
            super(itemView);
            notificationText = itemView.findViewById(R.id.title);
            this.adapter = adapter;
        }

        public void bind(final Notification item){
            Notification notification = item;
            setText(notification);
        }

        private void setText(Notification n) {
            Favor favor = n.getFavor();
            NotificationType nt = n.getNotificationType();
            if(nt != null && favor != null) {
                notificationText.setText(nt.getNotificationMessage() + " " + favor.get(Favor.StringFields.title));
            }
        }
    }

    /**
     * Constructor for a FavorListAdapter
     * @param notificationList
     */
    public NotificationListAdapter(ObservableArrayList<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    @Override
    public NotificationListAdapter.NotificationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //set layout to itemView using Layout inflater
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);
        return new NotificationListAdapter.NotificationViewHolder(itemView, this);
    }

    @Override
    public void onBindViewHolder(NotificationViewHolder holder, int position) {
        holder.bind(notificationList.get(position));
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

}
