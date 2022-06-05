/*
  Heavily based on https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/Progress.java
  Seriously why am I writing Java, this is so bad?!
 */

package me.schaertl.halina.support;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * Provide an easy to use HTTP API.
 */
public class Http {
    private Http() {}

    /**
     * Download JSON from a remote location.
     *
     * @param url URL of the JSON file on the web.
     * @return Parsed JSON.
     */
    public static JSONObject getJson(String url) throws IOException, JSONException {
        final String body = getStringThrowing(url);
        return new JSONObject(body);
    }

    /**
     * Download some remote file to a temporary location on the file system.
     *
     * @param url URL of the file to download.
     * @param progressHandler Progress will be reported to this handler.
     * @return The location of the downloaded file. It is the callers responsibility to clean up the downloaded file.
     */
    public static String downloadToTempDirectory(String url, ProgressHandler progressHandler) throws IOException {
        final Request request = new Request.Builder().url(url).addHeader("User-Agent", UserAgent.get()).build();

        final OkHttpClient client = new OkHttpClient.Builder().addNetworkInterceptor(new Interceptor() {
            @NonNull
            @Override
            public Response intercept(@NonNull Chain chain) throws IOException {
                final Response response = chain.proceed(chain.request());
                return response.newBuilder().body(new ProgressResponseBody(response.body(), progressHandler)).build();
            }
        }).build();

        // Create file for writing. We will later return the path to this file from this
        // function if everything went well.

        final Path tempDir = Files.createTempDirectory("halina");
        final Path tempFile = Paths.get(tempDir.toString(), "download.bin");

        // Start the HTTP call and copy byte by byte to the temp file.

        try (final Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                final String errorMessage = String.format("unexpected response from %s: %s", url, response);
                throw new IllegalStateException(errorMessage);
            }

            try (final FileOutputStream file = new FileOutputStream(tempFile.toString())) {
                final InputStream rx = response.body().byteStream();
                IOUtils.copy(rx, file);
            }
        }

        return tempFile.toString();
    }

    private static String getStringThrowing(String url) throws IOException {
        final OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder().url(url).addHeader("User-Agent", UserAgent.get()).build();

        try (final Response response = client.newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
        }
    }

    private static class ProgressResponseBody extends ResponseBody {
        private final ResponseBody responseBody;
        private final ProgressHandler progressHandler;
        private BufferedSource bufferedSource;

        public ProgressResponseBody(ResponseBody responseBody, ProgressHandler progressHandler) {
            this.responseBody = responseBody;
            this.progressHandler = progressHandler;
        }

        @Override
        public long contentLength() {
            return this.responseBody.contentLength();
        }

        @Nullable
        @Override
        public MediaType contentType() {
            return this.responseBody.contentType();
        }

        @NonNull
        @Override
        public BufferedSource source() {
            if (this.bufferedSource == null) {
                this.bufferedSource = Okio.buffer(source(responseBody.source()));
            }

            return this.bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(@NonNull Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;

                    if (progressHandler != null) {
                        final Progress report = new Progress(totalBytesRead, responseBody.contentLength());
                        progressHandler.onProgress(report);
                    }

                    return bytesRead;
                }
            };
        }
    }
}
