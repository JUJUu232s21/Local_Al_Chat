import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;
@Dao
public interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY timestamp DESC")
    LiveData<List<ChatEntity>> getAllChats();
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    LiveData<List<MessageEntity>> getMessagesForChat(String chatId);
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    List<MessageEntity> getMessagesForChatSync(String chatId);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChat(ChatEntity chat);
    @Insert
    void insertMessage(MessageEntity message);
    @Transaction
    default void insertChatAndMessage(ChatEntity chat, MessageEntity message) {
        insertChat(chat);
        insertMessage(message);
    }
    @Query("DELETE FROM chats WHERE id = :chatId")
    void deleteChat(String chatId);
}
