import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "chats")
public class ChatEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String chatName;
    private String avatarUrl;
    private long timestamp;
    public ChatEntity(@NonNull String id, String chatName, String avatarUrl, long timestamp) {
        this.id = id;
        this.chatName = chatName;
        this.avatarUrl = avatarUrl;
        this.timestamp = timestamp;
    }
    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getChatName() { return chatName; }
    public void setChatName(String chatName) { this.chatName = chatName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
