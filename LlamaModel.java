import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
public class LlamaModel {
    private static final String TAG = "LlamaModel";
    private static final String MODEL_ASSET_NAME = "gemma-3-1b-it-Q8_0.gguf";
    static {
        System.loadLibrary("local_al_chat");
    }
    private long modelPtr = 0;
    public interface TokenCallback {
        void onTokenGenerated(String token);
        void onFinished(String stats);
    }
    public static String getOrCopyModelPath(Context context) {
        File file = new File(context.getFilesDir(), MODEL_ASSET_NAME);
        if (!file.exists() || file.length() == 0) {
            Log.i(TAG, "Copying model file from assets to " + file.getAbsolutePath());
            try (InputStream in = context.getAssets().open(MODEL_ASSET_NAME);
                 FileOutputStream out = new FileOutputStream(file)) {
                byte[] buffer = new byte[16384];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
                Log.i(TAG, "Model copied successfully.");
            } catch (IOException e) {
                Log.e(TAG, "Error copying model from assets: " + e.getMessage(), e);
            }
        } else {
            Log.i(TAG, "Model file already exists at " + file.getAbsolutePath());
        }
        return file.getAbsolutePath();
    }
    public boolean load(String modelPath) {
        if (modelPtr != 0) {
            freeModel(modelPtr);
        }
        modelPtr = initModel(modelPath);
        return modelPtr != 0;
    }
    public void generateStream(String prompt, int maxTokens, TokenCallback callback) {
        if (modelPtr == 0) {
            if (callback != null) {
                callback.onTokenGenerated("Error: Model not loaded");
                callback.onFinished("");
            }
            return;
        }
        generateStreamNative(modelPtr, prompt, maxTokens, callback);
    }
    public void close() {
        if (modelPtr != 0) {
            freeModel(modelPtr);
            modelPtr = 0;
        }
    }
    public void stopGeneration() {
        if (modelPtr != 0) {
            stopGenerationNative(modelPtr);
        }
    }
    public native long initModel(String modelPath);
    public native void generateStreamNative(long modelPtr, String prompt, int maxTokens, TokenCallback callback);
    public native void stopGenerationNative(long modelPtr);
    public native void freeModel(long modelPtr);
}
