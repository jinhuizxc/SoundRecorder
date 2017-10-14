package com.example.jh.soundrecorder.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jh.soundrecorder.R;
import com.example.jh.soundrecorder.RecordingService;
import com.melnykov.fab.FloatingActionButton;

import java.io.File;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link RecordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

/**
 * MediaRecorder类介绍：

 MediaRecorder类是Android sdk提供的一个专门用于音视频录制，一般利用手机麦克风采集音频，摄像头采集图片信息。

 MediaRecorder主要函数：

 setAudioChannels(int numChannels) 设置录制的音频通道数

 setAudioEncoder(int audio_encoder) 设置audio的编码格式

 setAudioEncodingBitRate(int bitRate) 设置录制的音频编码比特率

 setAudioSamplingRate(int samplingRate) 设置录制的音频采样率

 setAudioSource(int audio_source) 设置用于录制的音源

 setAuxiliaryOutputFile(String path) 辅助时间的推移视频文件的路径传递

 setAuxiliaryOutputFile(FileDescriptor fd)在文件描述符传递的辅助时间的推移视频

 setCamera(Camera c) 设置一个recording的摄像头

 setCaptureRate(double fps) 设置视频帧的捕获率

 setMaxDuration(int max_duration_ms) 设置记录会话的最大持续时间（毫秒）

 setMaxFileSize(long max_filesize_bytes) 设置记录会话的最大大小（以字节为单位）

 setOutputFile(FileDescriptor fd) 传递要写入的文件的文件描述符

 setOutputFile(String path) 设置输出文件的路径

 setOutputFormat(int output_format) 设置在录制过程中产生的输出文件的格式

 setPreviewDisplay(Surface sv) 表面设置显示记录媒体（视频）的预览

 setVideoEncoder(int video_encoder) 设置视频编码器，用于录制

 setVideoEncodingBitRate(int bitRate) 设置录制的视频编码比特率

 setVideoFrameRate(int rate) 设置要捕获的视频帧速率

 setVideoSize(int width, int height) 设置要捕获的视频的宽度和高度

 setVideoSource(int video_source) 开始捕捉和编码数据到setOutputFile（指定的文件）

 setLocation(float latitude, float longitude) 设置并存储在输出文件中的地理数据（经度和纬度）

 setProfile(CamcorderProfile profile) 指定CamcorderProfile对象

 setOrientationHint(int degrees)设置输出的视频播放的方向提示

 setOnErrorListener(MediaRecorder.OnErrorListener l)注册一个用于记录录制时出现的错误的监听器

 setOnInfoListener(MediaRecorder.OnInfoListener listener)注册一个用于记录录制时出现的信息事件

 getMaxAmplitude() 获取在前一次调用此方法之后录音中出现的最大振幅

 prepare()准备录制。

 release()释放资源

 reset()将MediaRecorder设为空闲状态

 start()开始录制

 stop()停止录制

 MediaRecorder主要配置参数：

 1.）视频编码格式MediaRecorder.VideoEncoder

 default，H263，H264，MPEG_4_SP，VP8

 2.）音频编码格式MediaRecorder.AudioEncoder

 default，AAC，HE_AAC，AAC_ELD，AMR_NB，AMR_WB，VORBIS

 3.）视频资源获取方式MediaRecorder.VideoSource

 default，CAMERA，SURFACE

 4.）音频资源获取方式MediaRecorder.AudioSource

 defalut，camcorder，mic，voice_call，voice_communication,voice_downlink,voice_recognition, voice_uplink

 5.）资源输出格式MediaRecorder.OutputFormat

 amr_nb，amr_wb,default,mpeg_4,raw_amr,three_gpp，aac_adif， aac_adts， output_format_rtp_avp， output_format_mpeg2ts ，webm
 */
public class RecordFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_POSITION = "position";
    private static final String TAG = RecordFragment.class.getSimpleName();

    private int position;

    //Recording controls
    private FloatingActionButton mRecordButton = null;
    private Button mPauseButton = null;

    private TextView mRecordingPrompt;
    private int mRecordPromptCount = 0;

    private boolean mStartRecording = true;
    private boolean mPauseRecording = true;

    private Chronometer mChronometer = null;
    long timeWhenPaused = 0; //stores time when user clicks pause button

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment Record_Fragment.
     */
    public static RecordFragment newInstance(int position) {
        RecordFragment f = new RecordFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        f.setArguments(b);

        return f;
    }

    public RecordFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position = getArguments().getInt(ARG_POSITION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View recordView = inflater.inflate(R.layout.fragment_record, container, false);

        mChronometer = (Chronometer) recordView.findViewById(R.id.chronometer);
        //update recording prompt text
        mRecordingPrompt = (TextView) recordView.findViewById(R.id.recording_status_text);

        mRecordButton = (FloatingActionButton) recordView.findViewById(R.id.btnRecord);
        mRecordButton.setColorNormal(getResources().getColor(R.color.primary));
        mRecordButton.setColorPressed(getResources().getColor(R.color.primary_dark));
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 可能是android 7.0的原因，必须添加权限才能开始录音，解决开始录音的点击问题，不然会有闪退bug
                if (ActivityCompat.checkSelfPermission(RecordFragment.this.getActivity(), Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(RecordFragment.this.getActivity(), new String[]{Manifest.permission.RECORD_AUDIO},
                            10);
                } else {
                    // 开始录音
                    onRecord(mStartRecording);
                    mStartRecording = !mStartRecording;
                }

            }
        });

        mPauseButton = (Button) recordView.findViewById(R.id.btnPause);
        mPauseButton.setVisibility(View.GONE); //hide pause button before recording starts
        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 如果正在录音，在暂停
                onPauseRecord(mPauseRecording);
                mPauseRecording = !mPauseRecording;
            }
        });

        return recordView;
    }

    // Recording Start/Stop
    //TODO: recording pause
    private void onRecord(boolean start) {

        Intent intent = new Intent(getActivity(), RecordingService.class);
        Log.e(TAG, "intent =" + intent);
        if (start) {
            // start recording
            mRecordButton.setImageResource(R.drawable.ic_media_stop);
            //mPauseButton.setVisibility(View.VISIBLE);
            Toast.makeText(getActivity(), R.string.toast_recording_start, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "toast =" + R.string.toast_recording_start);
            File folder = new File(Environment.getExternalStorageDirectory() + "/SoundRecorder");
            if (!folder.exists()) {
                //folder /SoundRecorder doesn't exist, create the folder
                folder.mkdir();
            }

            //start Chronometer
            mChronometer.setBase(SystemClock.elapsedRealtime());
            mChronometer.start();
            mChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                @Override
                public void onChronometerTick(Chronometer chronometer) {
                    if (mRecordPromptCount == 0) {
                        mRecordingPrompt.setText(getString(R.string.record_in_progress) + ".");
                    } else if (mRecordPromptCount == 1) {
                        mRecordingPrompt.setText(getString(R.string.record_in_progress) + "..");
                    } else if (mRecordPromptCount == 2) {
                        mRecordingPrompt.setText(getString(R.string.record_in_progress) + "...");
                        mRecordPromptCount = -1;
                    }

                    mRecordPromptCount++;
                }
            });

            //start RecordingService
            getActivity().startService(intent);
            //keep screen on while recording
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            mRecordingPrompt.setText(getString(R.string.record_in_progress) + ".");
            mRecordPromptCount++;

        } else {
            //stop recording
            mRecordButton.setImageResource(R.drawable.ic_mic_white_36dp);
            //mPauseButton.setVisibility(View.GONE);
            mChronometer.stop();
            mChronometer.setBase(SystemClock.elapsedRealtime());
            timeWhenPaused = 0;
            mRecordingPrompt.setText(getString(R.string.record_prompt));

            getActivity().stopService(intent);
            //allow the screen to turn off again once recording is finished
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    //TODO: implement pause recording
    private void onPauseRecord(boolean pause) {
        if (pause) {
            //pause recording
            mPauseButton.setCompoundDrawablesWithIntrinsicBounds
                    (R.drawable.ic_media_play, 0, 0, 0);
            mRecordingPrompt.setText((String) getString(R.string.resume_recording_button).toUpperCase());
            timeWhenPaused = mChronometer.getBase() - SystemClock.elapsedRealtime();
            mChronometer.stop();
        } else {
            //resume recording
            mPauseButton.setCompoundDrawablesWithIntrinsicBounds
                    (R.drawable.ic_media_pause, 0, 0, 0);
            mRecordingPrompt.setText((String) getString(R.string.pause_recording_button).toUpperCase());
            mChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenPaused);
            mChronometer.start();
        }
    }
}