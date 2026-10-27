import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class MainActivity extends AppCompatActivity {
    private TextView StatysModel;
    private TextView LoadAilModel;
    private TextView EndLoadAilModel;
    private ProgressBar loadModelBar;
    private ImageView send;
    private ImageView btnMenu;
    private ImageView modelSetting;
    private View drawerOverlay;
    private View drawerPanel;
    private View drawerRightPanel;
    private ListView listView;
    private ListView chatHistory;
    private ChatAdapter chatAdapter;
    private AppDatabase db;
    private ChatDao chatDao;
    private String currentChatId = null;
    private final List<ChatEntity> chatList = new ArrayList<>();
    private EditText editText;
    private EditText maxToken;
    private ImageView Start_micro;
    private List<String> list = new ArrayList<>();
    private final List<String> statsList = new ArrayList<>();
    private MyAdapter adapter;
    private final LlamaModel llamaModel = new LlamaModel();
    private final ChatHistoryManager historyManager = new ChatHistoryManager(15);
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private boolean isModelLoaded = false;
    private boolean isDrawerOpen = false;
    private boolean isRightDrawerOpen = false;
    private boolean getResponse = false;
    private ImageView New_Chat_Ai;
    private EditText SystemPromtAi;
    private CheckBox SelectChatMemory;
    private ImageView img_btn_niz;
    private TextView Battery_optimization;
    private final ActivityResultLauncher<Intent> speechLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                String spokenText = matches.get(0);
                editText.setText(editText.getText().toString() + spokenText);
                editText.setSelection(editText.length());
            }
        }
    });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        send = findViewById(R.id.send);
        btnMenu = findViewById(R.id.btn_menu);
        modelSetting = findViewById(R.id.modelSetting);
        drawerOverlay = findViewById(R.id.drawer_overlay);
        drawerPanel = findViewById(R.id.drawer_panel);
        drawerRightPanel = findViewById(R.id.drawer_right_panel);
        listView = findViewById(R.id.ListView);
        SelectChatMemory = findViewById(R.id.SelectChatMemory);
        editText = findViewById(R.id.editText);
        LoadAilModel = findViewById(R.id.LoadAilModel);
        loadModelBar = findViewById(R.id.load_model_bar);
        StatysModel = findViewById(R.id.StatysModel);
        EndLoadAilModel = findViewById(R.id.EndLoadAilModel);
        maxToken = findViewById(R.id.maxToken);
        New_Chat_Ai = findViewById(R.id.New_Chat_Ai);
        Start_micro = findViewById(R.id.Start_micro);
        SystemPromtAi = findViewById(R.id.SystemPromtAi);
        img_btn_niz = findViewById(R.id.img_btn_niz);
        chatHistory = findViewById(R.id.chatHistory);
        Battery_optimization = findViewById(R.id.Battery_optimization);
        Battery_optimization.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    Intent intent = new Intent();
                    PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                    if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                    } else {
                        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                    }
                    startActivity(intent);
                } catch (Exception e) {
                    try {
                        Intent fallbackIntent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivity(fallbackIntent);
                    } catch (Exception ex) {
                        Intent detailsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        detailsIntent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(detailsIntent);
                    }
                }
            } else {
                Toast.makeText(this, "Оптимизация батареи не поддерживается", Toast.LENGTH_SHORT).show();
            }
        });
        db = AppDatabase.getInstance(this);
        chatDao = db.chatDao();
        chatAdapter = new ChatAdapter(this, chatList);
        chatHistory.setAdapter(chatAdapter);
        chatDao.getAllChats().observe(this, chats -> {
            chatList.clear();
            if (chats != null) {
                chatList.addAll(chats);
            }
            chatAdapter.setChatList(chatList);
        });
        chatHistory.setOnItemClickListener((parent, view, position, id) -> {
            ChatEntity selectedChat = chatAdapter.getItem(position);
            if (selectedChat != null) {
                if (getResponse) {
                    llamaModel.stopGeneration();
                    send.setImageResource(R.drawable.baseline_send_24);
                    getResponse = false;
                }
                currentChatId = selectedChat.getId();
                loadChatMessages(currentChatId);
                closeDrawer();
            }
        });
        chatHistory.setOnItemLongClickListener((parent, view, position, id) -> {
            ChatEntity selectedChat = chatAdapter.getItem(position);
            if (selectedChat != null) {
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this, R.style.CustomAlertDialog)
                        .setTitle("Удалить чат: " + selectedChat.getChatName())
                        .setMessage("Вы уверены, что хотите удалить этот чат?")
                        .setCancelable(false)
                        .setPositiveButton("Удалить", (dialog1, which) -> {
                            String deletedChatId = selectedChat.getId();
                            executor.execute(() -> chatDao.deleteChat(deletedChatId));
                            if (deletedChatId.equals(currentChatId)) {
                                currentChatId = null;
                                list.clear();
                                statsList.clear();
                                historyManager.clear();
                                adapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton("Отмена", null)
                        .create();
                dialog.setOnShowListener(dialogInterface -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(MainActivity.this, R.color.white));
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(MainActivity.this, R.color.white));
                });
                dialog.show();
            }
            return true;
        });
        android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        int savedMaxTokens = prefs.getInt("max_tokens", 256);
        maxToken.setText(String.valueOf(savedMaxTokens));
        String savedSystemPrompt = prefs.getString("system_prompt", "");
        SystemPromtAi.setText(savedSystemPrompt);
        boolean isMemoryEnabled = prefs.getBoolean("chat_memory_enabled", true);
        SelectChatMemory.setChecked(isMemoryEnabled);
        historyManager.clear();
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isDrawerOpen) {
                    closeDrawer();
                } else if (isRightDrawerOpen) {
                    closeRightDrawer();
                } else {
                    finish();
                    overridePendingTransition(0, 0);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
        adapter = new MyAdapter(this, list, statsList);
        listView.setAdapter(adapter);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets imeAndBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(0, imeAndBars.top, 0, imeAndBars.bottom);
            return insets;
        });
        SelectChatMemory.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getPreferences(MODE_PRIVATE).edit().putBoolean("chat_memory_enabled", isChecked).apply();
                if (isChecked) {
                    reloadHistoryManager();
                } else {
                    historyManager.clear();
                }
            }
        });
        New_Chat_Ai.setOnClickListener(view -> {
            if (getResponse) {
                llamaModel.stopGeneration();
                send.setImageResource(R.drawable.baseline_send_24);
                getResponse = false;
            }
            if (list.isEmpty()) {
                Snackbar.make(view, "Новый чат уже открыт!", Snackbar.LENGTH_SHORT).show();
                return;
            }
            currentChatId = null;
            list.clear();
            statsList.clear();
            historyManager.clear();
            adapter.notifyDataSetChanged();
        });
        btnMenu.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!isDrawerOpen && drawerPanel.getTranslationX() < 0) {
                drawerPanel.setTranslationX(-drawerPanel.getWidth());
            }
        });
        modelSetting.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!isRightDrawerOpen && drawerRightPanel.getTranslationX() > 0) {
                drawerRightPanel.setTranslationX(drawerRightPanel.getWidth());
            }
        });
        btnMenu.setOnClickListener(v -> {
            if (isDrawerOpen) {
                closeDrawer();
            } else {
                openDrawer();
            }
        });
        modelSetting.setOnClickListener(v -> {
            if (isRightDrawerOpen) {
                closeRightDrawer();
            } else {
                ClodeKeyBoard();
                openRightDrawer();
            }
        });
        drawerOverlay.setOnClickListener(v -> {
            closeDrawer();
            closeRightDrawer();
        });
        maxToken.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    String text = s.toString().trim();
                    if (!text.isEmpty()) {
                        int maxTokens = Integer.parseInt(text);
                        getPreferences(MODE_PRIVATE).edit().putInt("max_tokens", maxTokens).apply();
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        SystemPromtAi.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                getPreferences(MODE_PRIVATE).edit().putString("system_prompt", s.toString().trim()).apply();
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        Start_micro.setOnClickListener(view -> GOOGLE_SPEEAK_ANALIZE());
        LoadAilModel.setOnClickListener(view -> {
            ClodeKeyBoard();
            LoadAilModel.setEnabled(false);
            loadModelBar.setVisibility(View.VISIBLE);
            StatysModel.setTextColor(ContextCompat.getColor(this, R.color.yellow));
            StatysModel.setText("Загрузка...");
            executor.execute(() -> {
                String modelPath = LlamaModel.getOrCopyModelPath(MainActivity.this);
                isModelLoaded = llamaModel.load(modelPath);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    loadModelBar.setVisibility(View.GONE);
                    LoadAilModel.setEnabled(true);
                    if (isModelLoaded) {
                        closeRightDrawer();
                        StatysModel.setText("Gemma 3 1B");
                        StatysModel.setTextColor(ContextCompat.getColor(this, R.color.green));
                        LoadAilModel.setVisibility(View.GONE);
                        EndLoadAilModel.setVisibility(View.VISIBLE);
                        maxToken.setEnabled(false);
                        SystemPromtAi.setEnabled(false);
                    } else {
                        Snackbar.make(view, "Ошибка загрузки модели", Snackbar.LENGTH_SHORT).show();
                    }
                });
            });
        });
        EndLoadAilModel.setOnClickListener(view -> {
            if (!isModelLoaded) {
                Snackbar.make(view, "Модель не загружена", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (getResponse) {
                llamaModel.stopGeneration();
                send.setImageResource(R.drawable.baseline_send_24);
                getResponse = false;
            }
            executor.execute(() -> {
                llamaModel.close();
                isModelLoaded = false;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    StatysModel.setText("Нет модели");
                    StatysModel.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.red));
                    LoadAilModel.setVisibility(View.VISIBLE);
                    EndLoadAilModel.setVisibility(View.GONE);
                    maxToken.setEnabled(true);
                    SystemPromtAi.setEnabled(true);
                });
            });
        });
        send.setOnClickListener(view -> {
            if (getResponse) {
                llamaModel.stopGeneration();
                send.setImageResource(R.drawable.baseline_send_24);
                getResponse = false;
                return;
            }
            String userQuery = editText.getText().toString().trim();
            if (userQuery.isEmpty()) return;
            if (!isModelLoaded) {
                Snackbar.make(view, "Модель ещё не загружена", Snackbar.LENGTH_SHORT).show();
                ClodeKeyBoard();
                return;
            }
            boolean isNewChat = (currentChatId == null);
            final String activeChatId;
            final long userTimestamp = System.currentTimeMillis();
            if (isNewChat) {
                currentChatId = UUID.randomUUID().toString();
                activeChatId = currentChatId;
                String chatName = userQuery.length() > 15 ? userQuery.substring(0, 15) : userQuery;
                long now = System.currentTimeMillis();
                ChatEntity newChat = new ChatEntity(activeChatId, chatName, null, now);
                MessageEntity messageEntity = new MessageEntity(activeChatId, "user", userQuery, userTimestamp, "");
                executor.execute(() -> chatDao.insertChatAndMessage(newChat, messageEntity));
            } else {
                activeChatId = currentChatId;
                executor.execute(() -> {
                    chatDao.insertMessage(new MessageEntity(activeChatId, "user", userQuery, userTimestamp, ""));
                });
            }
            String systemPrompt = SystemPromtAi.getText().toString().trim();
            boolean memoryEnabled = SelectChatMemory.isChecked();
            String prompt;
            if (memoryEnabled) {
                historyManager.addMessage("user", userQuery);
                if (historyManager.getMessages().size() <= 1) {
                    if (systemPrompt.isEmpty()) {
                        prompt = userQuery;
                    } else {
                        prompt = "Следуя системному промпту: " + systemPrompt + "\nОтветь на мой запрос: " + userQuery;
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    if (!systemPrompt.isEmpty()) {
                        sb.append("Системный промпт: ").append(systemPrompt).append("\n\n");
                    }
                    sb.append("История диалога:\n");
                    for (ChatMessage msg : historyManager.getMessages()) {
                        if ("user".equals(msg.getRole())) {
                            sb.append("Вопрос пользователя: ").append(msg.getContent()).append("\n");
                        } else if ("assistant".equals(msg.getRole())) {
                            sb.append("Ответ нейросети: ").append(msg.getContent()).append("\n");
                        }
                    }
                    sb.append("\nОтветь на последний запрос пользователя:");
                    prompt = sb.toString();
                }
            } else {
                historyManager.clear();
                if (systemPrompt.isEmpty()) {
                    prompt = userQuery;
                } else {
                    prompt = "Следуя системному промпту: " + systemPrompt + "\nОтветь на мой запрос: " + userQuery;
                }
            }
            int maxTokens = 256;
            try {
                String tokenStr = maxToken.getText().toString().trim();
                if (!tokenStr.isEmpty()) {
                    maxTokens = Integer.parseInt(tokenStr);
                    getPreferences(MODE_PRIVATE).edit().putInt("max_tokens", maxTokens).apply();
                }
            } catch (NumberFormatException e) {
                maxTokens = 256;
            }
            list.add(userQuery);
            statsList.add("");
            int aiIndex = list.size();
            list.add("Генерирую ответ...");
            statsList.add("");
            adapter.notifyDataSetChanged();
            editText.setText("");
            getResponse = true;
            send.setImageResource(R.drawable.baseline_stop_24);
            final String finalPrompt = prompt;
            final int finalMaxTokens = maxTokens;
            executor.execute(() -> {
                StringBuilder responseBuilder = new StringBuilder();
                final long[] lastUiUpdate = {0};
                llamaModel.generateStream(finalPrompt, finalMaxTokens, new LlamaModel.TokenCallback() {
                    @Override
                    public void onTokenGenerated(String token) {
                        responseBuilder.append(token);
                        long now = System.currentTimeMillis();
                        if (now - lastUiUpdate[0] > 50) {
                            lastUiUpdate[0] = now;
                            String currentResponse = responseBuilder.toString();
                            runOnUiThread(() -> {
                                if (isFinishing() || isDestroyed()) return;
                                if (aiIndex < list.size()) {
                                    list.set(aiIndex, currentResponse);
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                    @Override
                    public void onFinished(String stats) {
                        String finalResponse = responseBuilder.toString().trim();
                        if (memoryEnabled && !finalResponse.isEmpty()) {
                            historyManager.addMessage("assistant", finalResponse);
                        }
                        if (!finalResponse.isEmpty()) {
                            final long aiTimestamp = System.currentTimeMillis();
                            final String finalStats = stats;
                            executor.execute(() -> {
                                chatDao.insertMessage(new MessageEntity(activeChatId, "assistant", finalResponse, aiTimestamp, finalStats));
                            });
                        }
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            send.setImageResource(R.drawable.baseline_send_24);
                            if (aiIndex < list.size()) {
                                list.set(aiIndex, finalResponse);
                                statsList.set(aiIndex, stats);
                                adapter.notifyDataSetChanged();
                                getResponse = false;
                            }
                        });
                    }
                });
            });
        });
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (totalItemCount == 0) {
                    img_btn_niz.setVisibility(View.GONE);
                    return;
                }
                int lastVisibleItem = firstVisibleItem + visibleItemCount - 1;
                boolean isAtBottom = false;
                if (lastVisibleItem >= totalItemCount - 1) {
                    View lastView = view.getChildAt(visibleItemCount - 1);
                    if (lastView != null) {
                        if (lastView.getBottom() <= view.getHeight() - view.getPaddingBottom()) {
                            isAtBottom = true;
                        }
                    }
                }
                if (!isAtBottom) {
                    img_btn_niz.setVisibility(View.VISIBLE);
                } else {
                    img_btn_niz.setVisibility(View.GONE);
                }
            }
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}
        });
        img_btn_niz.setOnClickListener(view ->{
            listView.smoothScrollToPosition(list.size() - 1);
        });
    }
    private void GOOGLE_SPEEAK_ANALIZE() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...");
        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Голосовой ввод недоступен", Toast.LENGTH_SHORT).show();
        }
    }
    private void openDrawer() {
        if (isRightDrawerOpen) closeRightDrawer();
        isDrawerOpen = true;
        drawerOverlay.setVisibility(View.VISIBLE);
        drawerPanel.animate().translationX(0f).setDuration(300).start();
    }
    private void closeDrawer() {
        isDrawerOpen = false;
        float width = drawerPanel.getWidth() > 0 ? drawerPanel.getWidth() : 1000f;
        drawerPanel.animate().translationX(-width).setDuration(300).withEndAction(() -> {
            if (!isRightDrawerOpen) {
                drawerOverlay.setVisibility(View.GONE);
            }
        }).start();
    }
    private void openRightDrawer() {
        if (isDrawerOpen) closeDrawer();
        isRightDrawerOpen = true;
        drawerOverlay.setVisibility(View.VISIBLE);
        drawerRightPanel.animate().translationX(0f).setDuration(300).start();
    }
    private void closeRightDrawer() {
        isRightDrawerOpen = false;
        float width = drawerRightPanel.getWidth() > 0 ? drawerRightPanel.getWidth() : 1000f;
        drawerRightPanel.animate().translationX(width).setDuration(300).withEndAction(() -> {
            if (!isDrawerOpen) {
                maxToken.clearFocus();
                drawerOverlay.setVisibility(View.GONE);
                SystemPromtAi.clearFocus();
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        updateSystemBa();
        checkBatteryOptimizationStatus();
    }
    private void checkBatteryOptimizationStatus() {
        if (Battery_optimization == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            boolean isIgnoring = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
            if (isIgnoring) {
                Battery_optimization.setText("Оптимизация батареи: Отключена");
            } else {
                Battery_optimization.setText("Оптимизация батареи: Включена");
            }
        } else {
            Battery_optimization.setText("Оптимизация батареи: Не поддерживается");
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                updateSystemBa();
            }
        });
    }
    private void updateSystemBa() {
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.gray2));
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isModelLoaded) {
            llamaModel.stopGeneration();
        }
        executor.execute(() -> {
            llamaModel.close();
            isModelLoaded = false;
        });
        executor.shutdown();
    }
    private void reloadHistoryManager() {
        historyManager.clear();
        if (!SelectChatMemory.isChecked()) return;

        if (currentChatId != null) {
            loadChatMessages(currentChatId);
        } else if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                String text = list.get(i);
                if (i % 2 == 0) {
                    historyManager.addMessage("user", text);
                } else {
                    historyManager.addMessage("assistant", text);
                }
            }
        }
    }
    private void loadChatMessages(String chatId) {
        executor.execute(() -> {
            List<MessageEntity> messages = chatDao.getMessagesForChatSync(chatId);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (chatId == null || !chatId.equals(currentChatId)) return;
                list.clear();
                statsList.clear();
                historyManager.clear();
                boolean memoryEnabled = SelectChatMemory.isChecked();
                for (MessageEntity msg : messages) {
                    if ("user".equals(msg.getSenderId())) {
                        list.add(msg.getText());
                        statsList.add("");
                        if (memoryEnabled) {
                            historyManager.addMessage("user", msg.getText());
                        }
                    } else if ("assistant".equals(msg.getSenderId())) {
                        list.add(msg.getText());
                        statsList.add(msg.getStats() != null ? msg.getStats() : "");
                        if (memoryEnabled) {
                            historyManager.addMessage("assistant", msg.getText());
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                if (!list.isEmpty()) {
                    listView.post(() -> listView.setSelection(list.size() - 1));
                }
            });
        });
    }
    private void ClodeKeyBoard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = this.getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}
