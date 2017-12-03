package com.kumailn.yhack;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.language.v1.CloudNaturalLanguage;
import com.google.api.services.language.v1.CloudNaturalLanguageRequestInitializer;
import com.google.api.services.language.v1.model.AnalyzeSyntaxRequest;
import com.google.api.services.language.v1.model.AnalyzeSyntaxResponse;
import com.google.api.services.language.v1.model.Document;
import com.google.api.services.language.v1.model.Token;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MyService extends Service
{
    protected AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

    protected boolean mIsListening;
    protected volatile boolean mIsCountDownOn;
    private boolean mIsStreamSolo;
    private static final String CLOUD_API_KEY = "AIzaSyBFJ2oO0tcJSK2qOre48AM1raYgIw2cO5g";
    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;

    @Override
    public void onCreate()
    {
        super.onCreate();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener((RecognitionListener) new SpeechRecognitionListener());
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());

        Toast.makeText(getApplicationContext(), "Service", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId)
    {
        //mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
        try
        {
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            Message msg = new Message();
            msg.what = MSG_RECOGNIZER_START_LISTENING;
            mServerMessenger.send(msg);
        }
        catch (RemoteException e)
        {

        }
        return  START_NOT_STICKY;
    }


    protected class IncomingHandler extends Handler
    {
        private WeakReference<MyService> mtarget;
        private boolean mIsStreamSolo;
        private AudioManager mAudioManager;

        IncomingHandler(MyService target)
        {
            mtarget = new WeakReference<MyService>(target);
        }


        @Override
        public void handleMessage(Message msg)
        {
            final MyService target = mtarget.get();

            switch (msg.what)
            {
                case MSG_RECOGNIZER_START_LISTENING:

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    {
                        // turn off beep sound  
                        if (!mIsStreamSolo)
                        {
                            //mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                            mAudioManager =(AudioManager)getSystemService(Context.AUDIO_SERVICE);
                            mAudioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
                            mAudioManager.setStreamMute(AudioManager.STREAM_ALARM, true);
                            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                            mAudioManager.setStreamMute(AudioManager.STREAM_RING, true);
                            mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);

                            mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
                            mIsStreamSolo = true;
                        }
                    }
                    if (!target.mIsListening)
                    {
                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
                        target.mIsListening = true;
                        //Log.d(TAG, "message start listening"); //$NON-NLS-1$
                    }
                    break;

                case MSG_RECOGNIZER_CANCEL:
                    if (mIsStreamSolo)
                    {
                        mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);
                        mIsStreamSolo = false;
                    }
                    target.mSpeechRecognizer.cancel();
                    target.mIsListening = false;
                    //Log.d(TAG, "message canceled recognizer"); //$NON-NLS-1$
                    break;
            }
        }
    }

    // Count down timer for Jelly Bean work around
    protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(5000, 5000)
    {

        @Override
        public void onTick(long millisUntilFinished)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void onFinish()
        {
            mIsCountDownOn = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
            try
            {
                mServerMessenger.send(message);
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
            }
            catch (RemoteException e)
            {

            }
        }
    };

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (mIsCountDownOn)
        {
            mNoSpeechCountDown.cancel();
        }
        if (mSpeechRecognizer != null)
        {
            mSpeechRecognizer.destroy();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected class SpeechRecognitionListener implements RecognitionListener
    {

      

        @Override
        public void onBeginningOfSpeech()
        {
            // speech input will be processed, so there is no need for count down anymore
            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            //Log.d(TAG, "onBeginingOfSpeech"); //$NON-NLS-1$
        }

        @Override
        public void onBufferReceived(byte[] buffer)
        {

        }

        @Override
        public void onEndOfSpeech()
        {
            //Log.d(TAG, "onEndOfSpeech"); //$NON-NLS-1$
        }

        @Override
        public void onError(int error)
        {
            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            mIsListening = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
            try
            {
                mServerMessenger.send(message);
            }
            catch (RemoteException e)
            {

            }
            //Log.d(TAG, "error = " + error); //$NON-NLS-1$
        }

        @Override
        public void onEvent(int eventType, Bundle params)
        {

        }

        @Override
        public void onPartialResults(Bundle partialResults)
        {

        }

        @Override
        public void onReadyForSpeech(Bundle params)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            {
                mIsCountDownOn = true;
                mNoSpeechCountDown.start();

            }
            Log.d("", "onReadyForSpeech");
            //$NON-NLS-1$
        }

        @Override
        public void onResults(Bundle results)
        {
            //Log.d(TAG, "onResults"); //$NON-NLS-1$
            String str = new String();
            Log.e("SERVICE: ", "onResults " + results);
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < data.size(); i++)
            {
                Log.e("SERVICE: ", "result " + data.get(i));
/*                callNaturalLanguage((String) data.get(i));
                Log.e("OUTSIDE: ", callNaturalLanguage((String) data.get(i)));*/
                str += data.get(i);
            }

            callNaturalLanguage((String) data.get(data.size()-1));


            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            mIsListening = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
            try
            {
                mServerMessenger.send(message);
            }
            catch (RemoteException e)
            {

            }
            //Log.d(TAG, "error = " + error); //$NON-NLS-1$

        }

        @Override
        public void onRmsChanged(float rmsdB)
        {

        }

    }

    private String callNaturalLanguage(final String s) {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        CloudNaturalLanguageRequestInitializer requestInitializer = new CloudNaturalLanguageRequestInitializer(CLOUD_API_KEY);
        CloudNaturalLanguage.Builder builder = new CloudNaturalLanguage.Builder(httpTransport, jsonFactory, null);
        builder.setCloudNaturalLanguageRequestInitializer(requestInitializer);
        builder.setApplicationName("Application name");
        final CloudNaturalLanguage naturalLanguageService = builder.build();

        // this string should be what you want to analyze
        //final String transcript = "analyze what is in front of me";
        final String transcript = s;

        final Document document = new Document();
        document.setType("PLAIN_TEXT");
        document.setLanguage("en-US");
        document.setContent(transcript);

        final AnalyzeSyntaxRequest request = new AnalyzeSyntaxRequest();
        request.setDocument(document);
        request.setEncodingType("UTF16");
        final String[] FLAG1 = {""};
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    AnalyzeSyntaxResponse response = naturalLanguageService.documents()
                            .analyzeSyntax(request).execute();

                    final List<Token> tokenList = response.getTokens();


                            FLAG1[0] = "false";
                            String lemmas = "";
                            String tokens = "";

                            for (Token t : tokenList) {
                                lemmas += "\n" + t.getLemma();
                            }
                            for (Token t : tokenList) {
                                tokens += "\n" + t.getText().getContent();
                            }
                            // what is * type of strings
                            for (int i = 0; i < tokenList.size() - 1; i++) {
                                if (tokenList.get(i).getText().getContent().toUpperCase().hashCode() == "WHAT".hashCode()) {
                                    if (tokenList.get(i + 1).getLemma().toUpperCase().hashCode() == "BE".hashCode()) {
                                        FLAG1[0] = "picture";
                                    }
                                }
                            }
                            // * read * or * text *type of strings
                            if (tokens.contains("read") || tokens.contains("text")) {
                                FLAG1[0] = "read";
                            }

                            try{
                                Log.e("The Flag: ", FLAG1[0]);
                                Intent intent = new Intent("YourAction");
                                Bundle bundle = new Bundle();
                                bundle.putString("google", FLAG1[0]);
                                intent.putExtras(bundle);
                                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                            }

                            catch(Exception e){}
/*
                            AlertDialog dialog =
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle("Sentiment: ")
                                            .setMessage("Text :"
                                                    + transcript + "\nFLAG :" + FLAG1[0])
                                            .setNeutralButton("Okay", null)
                                            .create();
                            dialog.show();
                        */

                } catch (IOException e) {
                }
                // More code here
            }
        });
        try{
        Log.e("The Flag: ", FLAG1[0]);}
        catch(Exception e){}
        return FLAG1[0];
    }



}