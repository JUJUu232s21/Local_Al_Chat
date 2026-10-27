import java.util.ArrayList;
import java.util.List;
public class ChatHistoryManager {
    private final int maxMessages;
    private final List<ChatMessage> messages;
    public ChatHistoryManager(int maxMessages) {
        this.maxMessages = maxMessages;
        this.messages = new ArrayList<>();
    }
    public void addMessage(String role, String content) {
        messages.add(new ChatMessage(role, content));
        if (messages.size() > maxMessages) {
            messages.remove(0);
        }
    }
    public List<ChatMessage> getMessages() {
        return messages;
    }
    public void clear() {
        messages.clear();
    }
}
