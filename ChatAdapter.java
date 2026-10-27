import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
public class ChatAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private List<ChatEntity> chatList;
    public ChatAdapter(Context context, List<ChatEntity> chatList) {
        this.inflater = LayoutInflater.from(context);
        this.chatList = chatList != null ? chatList : new ArrayList<>();
    }
    public void setChatList(List<ChatEntity> chatList) {
        this.chatList = chatList != null ? chatList : new ArrayList<>();
        notifyDataSetChanged();
    }
    @Override
    public int getCount() {
        return chatList.size();
    }
    @Override
    public ChatEntity getItem(int position) {
        return chatList.get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_chat_message, parent, false);
            holder = new ViewHolder();
            holder.tvMessage = convertView.findViewById(R.id.tvMessage);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        ChatEntity chat = getItem(position);
        if (chat != null) {
            holder.tvMessage.setText(chat.getChatName());
        }
        return convertView;
    }
    private static class ViewHolder {
        TextView tvMessage;
    }
}
