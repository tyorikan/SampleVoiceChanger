package com.radionama.samplevoicechanger;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;


public class MainActivity extends ActionBarActivity {

    private AudioRecord mAudioRec;
    private AudioTrack mAudioTrack;

    private boolean mIsRecording = false;
    private int mBufSize = 0;
    private int mSamplingRate = 0;

    private static int[] sSampleRates = new int[]{8000, 11025, 22050, 44100};

    private static short[] sAudioFormat = new short[]{
            AudioFormat.ENCODING_PCM_8BIT,
            AudioFormat.ENCODING_PCM_16BIT};

    private static short[] sChannelConfig = new short[]{
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.CHANNEL_IN_STEREO};

    private void recodingAndPlayMain() {
        if (mIsRecording) {
            mIsRecording = false;
        } else {
            // 録音開始
            Log.v("AudioRecord", "startRecording");
            mAudioRec.startRecording();
            mIsRecording = true;
            // 録音スレッド
            new Thread(new Runnable() {
                @Override
                public void run() {
                    recodingAndPlay();
                }
            }).start();
        }
    }

    public void findAudioRecord() {
        for (int rate : sSampleRates) {
            for (short audioFormat : sAudioFormat) {
                for (short channelConfig : sChannelConfig) {
                    try {
                        Log.d("TAG", "Attempting rate " + rate + "Hz, bits: "
                                + audioFormat + ", channel: " + channelConfig);

                        int bufferSize = AudioRecord
                                .getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            mAudioRec = new AudioRecord(
                                    MediaRecorder.AudioSource.MIC,
                                    rate,
                                    AudioFormat.CHANNEL_IN_MONO,
                                    audioFormat,
                                    bufferSize
                            );
                            if (mAudioRec.getState() == AudioRecord.STATE_INITIALIZED) {
                                mBufSize = bufferSize;
                            }
                            mSamplingRate = rate;

                            return;
                        }
                    } catch (Exception e) {
                        Log.e("TAG", rate + "Exception, keep trying.", e);
                    }
                }
            }
        }
    }

    private void recodingAndPlay() {
        byte buf[] = new byte[mBufSize];
        while (mIsRecording) {
            // 録音データ読み込み, 再生処理
            mAudioRec.read(buf, 0, mBufSize);
            mAudioTrack.write(buf, 0, buf.length);
        }
        // 録音停止
        Log.v("AudioRecord", "stop");
        mAudioRec.stop();
        mAudioTrack.stop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // AudioRecordの作成
        findAudioRecord();

        mAudioTrack = new AudioTrack(
                MediaRecorder.AudioSource.MIC,
                mSamplingRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mBufSize,
                AudioTrack.MODE_STREAM
        );
        mAudioTrack.setPlaybackRate(sSampleRates[1]);
        mAudioTrack.play();

        findViewById(R.id.btProc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recodingAndPlayMain();
            }
        });
    }

    @Override
    protected void onDestroy() {
        mAudioRec.release();
        mAudioTrack.release();
        super.onDestroy();
    }
}
