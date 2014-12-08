package com.radionama.samplevoicechanger;

import com.radionama.samplevoicechanger.visualizer.VisualizerView;
import com.radionama.samplevoicechanger.visualizer.renderer.BarGraphRenderer;
import com.radionama.samplevoicechanger.visualizer.renderer.CircleBarRenderer;
import com.radionama.samplevoicechanger.visualizer.renderer.CircleRenderer;
import com.radionama.samplevoicechanger.visualizer.renderer.LineRenderer;
import com.squareup.otto.Subscribe;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;


public class MainActivity extends ActionBarActivity {

    private static int[] sSampleRates = new int[]{8000, 11025, 16000, 22050, 32000, 44100};
    private static final int RECORDING_SAMPLE_RATE = sSampleRates[2];

    private AudioRecord mAudioRec;
    private AudioTrack mAudioTrack;

    private boolean mIsRecording = false;
    private int mBufSize = 0;
    private int mSamplingRate = sSampleRates[2];

    private RadioGroup mRadioGroup;
    private Button mToggleButton;

    private VisualizerView mVisualizerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BusProvider.getInstance().register(this);

        mRadioGroup = (RadioGroup) findViewById(R.id.voice_key);
        mToggleButton = (Button) findViewById(R.id.toggle_button);
        mVisualizerView = (VisualizerView) findViewById(R.id.visualizer);

        // AudioRecordの作成
        findAudioRecord();

        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_SYSTEM,
                mSamplingRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mBufSize,
                AudioTrack.MODE_STREAM
        );

        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsRecording) {
                    mToggleButton.setText(R.string.start);
                } else {
                    mToggleButton.setText(R.string.stop);
                }

                recodingAndPlayMain();
            }
        });
    }

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
                    setPlaybackRate();
                    recodingAndPlay();
                }
            }).start();
        }
    }

    private void recodingAndPlay() {
        mAudioTrack.setPlaybackRate(mSamplingRate);
        mAudioTrack.play();
        BusProvider.getInstance().post(new AudioTrackPlayEvent());

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
        mVisualizerView.setVisualizerEnabled(false);
    }

    public void findAudioRecord() {
        try {
            int bufferSize = AudioRecord.getMinBufferSize(
                    RECORDING_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            );

            mAudioRec = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    RECORDING_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
            );
            if (mAudioRec.getState() == AudioRecord.STATE_INITIALIZED) {
                mBufSize = bufferSize;
            }

            return;

        } catch (Exception e) {
            Log.e("TAG", RECORDING_SAMPLE_RATE + "Exception, keep trying.", e);
        }
    }

    private void setPlaybackRate() {
        int id = mRadioGroup.getCheckedRadioButtonId();
        switch (id) {
            case R.id.voice_lowest:
                mSamplingRate = sSampleRates[0];
                break;
            case R.id.voice_lower:
                mSamplingRate = sSampleRates[1];
                break;
//            case R.id.voice_default:
//                mSamplingRate = sSampleRates[2];
//                break;
            case R.id.voice_higher:
                mSamplingRate = sSampleRates[3];
                break;
            case R.id.voice_very_higher:
                mSamplingRate = sSampleRates[4];
                break;
            case R.id.voice_highest:
                mSamplingRate = sSampleRates[5];
                break;
        }
    }

    @Override
    protected void onDestroy() {
        mAudioRec.release();
        mAudioTrack.release();
        super.onDestroy();
    }

    @Subscribe
    public void onAudioRecordEvent(AudioTrackPlayEvent event) {
        mVisualizerView.link(mAudioTrack);
        addLineRenderer();
//        addBarGraphRenderers();
//        addCircleBarRenderer();
//        addCircleRenderer();
    }

    // Methods for adding renderers to visualizer
    private void addBarGraphRenderers() {
        Paint paint = new Paint();
        paint.setStrokeWidth(50f);
        paint.setAntiAlias(true);
        paint.setColor(Color.argb(200, 56, 138, 252));
        BarGraphRenderer barGraphRendererBottom = new BarGraphRenderer(16, paint, false);
        mVisualizerView.addRenderer(barGraphRendererBottom);

        Paint paint2 = new Paint();
        paint2.setStrokeWidth(12f);
        paint2.setAntiAlias(true);
        paint2.setColor(Color.argb(200, 181, 111, 233));
        BarGraphRenderer barGraphRendererTop = new BarGraphRenderer(4, paint2, true);
        mVisualizerView.addRenderer(barGraphRendererTop);
    }

    private void addCircleBarRenderer() {
        Paint paint = new Paint();
        paint.setStrokeWidth(8f);
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN));
        paint.setColor(Color.argb(255, 222, 92, 143));
        CircleBarRenderer circleBarRenderer = new CircleBarRenderer(paint, 32, true);
        mVisualizerView.addRenderer(circleBarRenderer);
    }

    private void addCircleRenderer() {
        Paint paint = new Paint();
        paint.setStrokeWidth(3f);
        paint.setAntiAlias(true);
        paint.setColor(Color.argb(255, 222, 92, 143));
        CircleRenderer circleRenderer = new CircleRenderer(paint, true);
        mVisualizerView.addRenderer(circleRenderer);
    }

    private void addLineRenderer() {
        Paint linePaint = new Paint();
        linePaint.setStrokeWidth(1f);
        linePaint.setAntiAlias(true);
        linePaint.setColor(Color.argb(88, 0, 128, 255));

        Paint lineFlashPaint = new Paint();
        lineFlashPaint.setStrokeWidth(5f);
        lineFlashPaint.setAntiAlias(true);
        lineFlashPaint.setColor(Color.argb(188, 255, 255, 255));
        LineRenderer lineRenderer = new LineRenderer(linePaint, lineFlashPaint, true);
        mVisualizerView.addRenderer(lineRenderer);
    }
}
