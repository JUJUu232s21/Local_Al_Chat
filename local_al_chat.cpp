#include <jni.h>
#include <string>
#include <vector>
#include <cstring>
#include <chrono>
#include <atomic>
#include <android/log.h>
#include "llama.h"

#define LOG_TAG "local_al_chat_native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct LlamaState {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    std::atomic<bool> is_cancelled{false};
};

extern "C" JNIEXPORT jstring JNICALL
Java_semyon_android_studio_local_1al_1chat_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string system_info = llama_print_system_info();
    LOGI("Llama system info: %s", system_info.c_str());
    return env->NewStringUTF(system_info.c_str());
}

extern "C" JNIEXPORT jlong JNICALL
Java_semyon_android_studio_local_1al_1chat_LlamaModel_initModel(
        JNIEnv *env, jobject /* thiz */, jstring model_path_) {
    const char *model_path = env->GetStringUTFChars(model_path_, nullptr);

    llama_backend_init();

    llama_model_params model_params = llama_model_default_params();
    llama_model *model = llama_model_load_from_file(model_path, model_params);

    if (!model) {
        LOGE("Failed to load llama model");
        env->ReleaseStringUTFChars(model_path_, model_path);
        return 0;
    }

    env->ReleaseStringUTFChars(model_path_, model_path);

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048;
    ctx_params.n_threads = 4;
    ctx_params.n_threads_batch = 4;

    llama_context *ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        LOGE("Failed to create llama context");
        llama_model_free(model);
        return 0;
    }

    LlamaState *state = new LlamaState();
    state->model = model;
    state->ctx = ctx;

    LOGI("Model loaded successfully");
    return reinterpret_cast<jlong>(state);
}

extern "C" JNIEXPORT void JNICALL
Java_semyon_android_studio_local_1al_1chat_LlamaModel_generateStreamNative(
        JNIEnv *env, jobject /* thiz */, jlong model_ptr, jstring prompt_, jint max_tokens, jobject callback) {
    if (model_ptr == 0 || callback == nullptr) {
        return;
    }

    LlamaState *state = reinterpret_cast<LlamaState *>(model_ptr);
    state->is_cancelled.store(false);

    const char *prompt = env->GetStringUTFChars(prompt_, nullptr);

    std::string formatted_prompt = "<start_of_turn>user\n" + std::string(prompt) + "<end_of_turn>\n<start_of_turn>model\n";
    env->ReleaseStringUTFChars(prompt_, prompt);

    const llama_vocab *vocab = llama_model_get_vocab(state->model);

    int n_prompt_tokens = -llama_tokenize(vocab, formatted_prompt.c_str(), static_cast<int>(formatted_prompt.length()), nullptr, 0, true, true);
    if (n_prompt_tokens <= 0) {
        return;
    }

    std::vector<llama_token> prompt_tokens(n_prompt_tokens);
    if (llama_tokenize(vocab, formatted_prompt.c_str(), static_cast<int>(formatted_prompt.length()), prompt_tokens.data(), static_cast<int>(prompt_tokens.size()), true, true) < 0) {
        return;
    }

    if (state->ctx) {
        llama_free(state->ctx);
    }
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048;
    ctx_params.n_threads = 4;
    ctx_params.n_threads_batch = 4;
    state->ctx = llama_init_from_model(state->model, ctx_params);

    if (!state->ctx) {
        return;
    }

    jclass callback_class = env->GetObjectClass(callback);
    jmethodID on_token_method = env->GetMethodID(callback_class, "onTokenGenerated", "(Ljava/lang/String;)V");
    jmethodID on_finished_method = env->GetMethodID(callback_class, "onFinished", "(Ljava/lang/String;)V");

    auto sparams = llama_sampler_chain_default_params();
    llama_sampler *smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(42));

    llama_batch batch = llama_batch_get_one(prompt_tokens.data(), static_cast<int32_t>(prompt_tokens.size()));

    auto t_start = std::chrono::high_resolution_clock::now();
    auto t_first = t_start;
    bool first_token = true;

    int n_cur = 0;

    while (n_cur < max_tokens) {
        if (state->is_cancelled.load()) {
            LOGI("Generation cancelled by user");
            break;
        }

        if (llama_decode(state->ctx, batch) != 0) {
            LOGE("llama_decode failed");
            break;
        }

        if (state->is_cancelled.load()) {
            LOGI("Generation cancelled by user");
            break;
        }

        if (first_token) {
            t_first = std::chrono::high_resolution_clock::now();
            first_token = false;
        }

        llama_token new_token_id = llama_sampler_sample(smpl, state->ctx, -1);

        if (llama_vocab_is_eog(vocab, new_token_id)) {
            break;
        }

        char piece[256];
        int n_piece = llama_token_to_piece(vocab, new_token_id, piece, sizeof(piece), 0, true);
        if (n_piece > 0) {
            std::string piece_str(piece, n_piece);
            jstring jpiece = env->NewStringUTF(piece_str.c_str());
            if (on_token_method != nullptr) {
                env->CallVoidMethod(callback, on_token_method, jpiece);
            }
            env->DeleteLocalRef(jpiece);
        }

        batch = llama_batch_get_one(&new_token_id, 1);
        n_cur++;
    }

    auto t_end = std::chrono::high_resolution_clock::now();
    llama_sampler_free(smpl);

    double duration_ms = std::chrono::duration<double, std::milli>(t_end - t_start).count();
    double ttft_ms = std::chrono::duration<double, std::milli>(t_first - t_start).count();
    double ms_per_token = n_cur > 0 ? duration_ms / n_cur : 0;
    double tokens_per_sec = duration_ms > 0 ? (n_cur * 1000.0) / duration_ms : 0;

    char stats_buf[256];
    snprintf(stats_buf, sizeof(stats_buf), "%.0f мс/ток, %.2f ток/сек, %.0f мс TTFT", ms_per_token, tokens_per_sec, ttft_ms);

    if (on_finished_method != nullptr) {
        jstring jstats = env->NewStringUTF(stats_buf);
        env->CallVoidMethod(callback, on_finished_method, jstats);
        env->DeleteLocalRef(jstats);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_semyon_android_studio_local_1al_1chat_LlamaModel_stopGenerationNative(
        JNIEnv * /* env */, jobject /* thiz */, jlong model_ptr) {
    if (model_ptr != 0) {
        LlamaState *state = reinterpret_cast<LlamaState *>(model_ptr);
        state->is_cancelled.store(true);
        LOGI("Generation stop requested");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_semyon_android_studio_local_1al_1chat_LlamaModel_freeModel(
        JNIEnv * /* env */, jobject /* thiz */, jlong model_ptr) {
    if (model_ptr != 0) {
        LlamaState *state = reinterpret_cast<LlamaState *>(model_ptr);
        if (state->ctx) {
            llama_free(state->ctx);
        }
        if (state->model) {
            llama_model_free(state->model);
        }
        delete state;
        llama_backend_free();
        LOGI("Model freed successfully");
    }
}
