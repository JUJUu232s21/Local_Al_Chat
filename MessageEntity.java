import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
@Entity(
    tableName = "messages",
    foreignKeys = @ForeignKey(
        entity = ChatEntity.class,
        parentColumns = "id",
        childColumns = "chatId",
        onDelete = ForeignKey.CASCADE
    )
)
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String chatId;
    private String senderId;
    private String text;
    private long timestamp;
    private String stats;
    public MessageEntity(String chatId, String senderId, String text, long timestamp, String stats) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
        this.stats = stats;
    }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStats() { return stats; }
    public void setStats(String stats) { this.stats = stats; }
}
