import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class MyAdapter extends ArrayAdapter<String> {
    private final List<String> statsList;
    public MyAdapter(@NonNull Context context, @NonNull List<String> list, @NonNull List<String> statsList) {
        super(context, R.layout.item_text, list);
        this.statsList = statsList;
    }
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.item_text, parent, false);
        }
        LinearLayout rootLayout = view.findViewById(R.id.rootLayout);
        LinearLayout bubbleLayout = view.findViewById(R.id.bubbleLayout);
        View statsContainer = view.findViewById(R.id.statsContainer);
        TextView textView = view.findViewById(R.id.itemText);
        TextView textStats = view.findViewById(R.id.textStats);
        ImageView copyAiText = view.findViewById(R.id.copy_ai_text);
        textView.setText(formatText(getItem(position)));
        float density = getContext().getResources().getDisplayMetrics().density;
        int pad12 = (int) (12 * density);
        int pad8 = (int) (8 * density);
        int pad2 = (int) (2 * density);
        if (position % 2 == 0) {
            rootLayout.setGravity(Gravity.END);
            bubbleLayout.setGravity(Gravity.END);
            bubbleLayout.setBackgroundResource(R.drawable.text_round);
            bubbleLayout.setPadding(pad12, pad8, pad12, pad8);
            if (statsContainer != null) statsContainer.setVisibility(View.GONE);
            textStats.setVisibility(View.GONE);
            copyAiText.setVisibility(View.GONE);
            bubbleLayout.setOnTouchListener(new View.OnTouchListener() {
                private float startX;
                private float startY;
                private boolean isSwiping = false;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    float density = v.getContext().getResources().getDisplayMetrics().density;
                    float threshold = 80 * density;
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getRawX();
                            startY = event.getRawY();
                            isSwiping = false;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getRawX() - startX;
                            float dy = event.getRawY() - startY;
                            if (!isSwiping && Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 15) {
                                isSwiping = true;
                                if (v.getParent() != null) {
                                    v.getParent().requestDisallowInterceptTouchEvent(true);
                                }
                            }
                            if (isSwiping) {
                                v.setTranslationX(dx);
                                return true;
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (!isSwiping) {
                                v.performClick();
                            } else if (Math.abs(v.getTranslationX()) >= threshold) {
                                ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("Copied Text", getItem(position));
                                if (clipboard != null) {
                                    clipboard.setPrimaryClip(clip);
                                    Snackbar.make(v, "Текст скопирован!", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                            v.animate().translationX(0f).setDuration(200).start();
                            if (isSwiping) {
                                return true;
                            }
                            break;
                    }
                    return false;
                }
            });
        } else {
            rootLayout.setGravity(Gravity.START);
            bubbleLayout.setGravity(Gravity.START);
            bubbleLayout.setBackgroundResource(0);
            bubbleLayout.setPadding(0, pad2, 0, pad2);
            bubbleLayout.setOnTouchListener(null);
            String statsStr = (position < statsList.size()) ? statsList.get(position) : "";
            if (statsStr != null && !statsStr.isEmpty()) {
                if (statsContainer != null) statsContainer.setVisibility(View.VISIBLE);
                textStats.setVisibility(View.VISIBLE);
                copyAiText.setVisibility(View.VISIBLE);
                textStats.setText(statsStr);
            } else {
                if (statsContainer != null) statsContainer.setVisibility(View.GONE);
                textStats.setVisibility(View.GONE);
                copyAiText.setVisibility(View.GONE);
            }
            copyAiText.setOnClickListener(view1 -> {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("AI Response", getItem(position));
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Snackbar.make(view1, "Текст скопирован в буфер обмена", Snackbar.LENGTH_SHORT).show();
                }
            });
        }
        return view;
    }
    private CharSequence formatText(String rawText) {
        if (rawText == null) return "";
        Pattern codeBlockPattern = Pattern.compile("```(?:[a-zA-Z0-9_#-]+)?\\n?([\\s\\S]*?)```");
        Matcher codeBlockMatcher = codeBlockPattern.matcher(rawText);
        StringBuffer sb = new StringBuffer();
        while (codeBlockMatcher.find()) {
            String code = codeBlockMatcher.group(1);
            if (code != null) {
                code = code.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>");
                String replacement = "<br/><span style=\"background-color:#222222; color:#80D8FF;\"><tt>" + code + "</tt></span><br/>";
                codeBlockMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        codeBlockMatcher.appendTail(sb);
        String formatted = sb.toString();
        Pattern inlineCodePattern = Pattern.compile("`([^`]+)`");
        Matcher inlineMatcher = inlineCodePattern.matcher(formatted);
        StringBuffer sbInline = new StringBuffer();
        while (inlineMatcher.find()) {
            String inlineCode = inlineMatcher.group(1);
            if (inlineCode != null) {
                inlineCode = inlineCode.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                String replacement = "<span style=\"background-color:#222222; color:#80D8FF;\"><tt><b>" + inlineCode + "</b></tt></span>";
                inlineMatcher.appendReplacement(sbInline, Matcher.quoteReplacement(replacement));
            }
        }
        inlineMatcher.appendTail(sbInline);
        formatted = sbInline.toString();
        formatted = formatted.replaceAll("(?m)^###\\s*(.*)$", "<h3><b>$1</b></h3>");
        formatted = formatted.replaceAll("(?m)^##\\s*(.*)$", "<h2><b>$1</b></h2>");
        formatted = formatted.replaceAll("(?m)^#\\s*(.*)$", "<h1><b>$1</b></h1>");
        formatted = formatted.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        formatted = formatted.replaceAll("\\*([^*]+)\\*", "<i>$1</i>");
        formatted = formatted.replace("\n", "<br/>");
        CharSequence cs = Html.fromHtml(formatted, Html.FROM_HTML_MODE_LEGACY);
        int len = cs.length();
        while (len > 0 && Character.isWhitespace(cs.charAt(len - 1))) {
            len--;
        }
        return cs.subSequence(0, len);
    }
}
