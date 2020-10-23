package com.example.vendingmachine.platform.http.download;

/**
 * HE 2018-11-08.
 */

public interface DownloadListener {
    void onStartDownload();

    void onProgress(int progress);

    void onFinishDownload();

    void onFail(String errorInfo);
}
