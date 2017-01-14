package kz.algakzru.youtubevideovocabulary.video;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kz.algakzru.youtubevideovocabulary.R;
import kz.algakzru.youtubevideovocabulary.util.Utils;

/**
 * FFmpeg Android Java - http://hiteshsondhi88.github.io/ffmpeg-android-java/
 * Android java library for FFmpeg binary compiled using - https://github.com/hiteshsondhi88/ffmpeg-android
 * prebuilt-binaries.zip - https://github.com/hiteshsondhi88/ffmpeg-android/releases/download/v0.3.3/prebuilt-binaries.zip
 * prebuilt-binaries.tar.gz - https://github.com/hiteshsondhi88/ffmpeg-android/releases/download/v0.3.3/prebuilt-binaries.tar.gz
 */

public class FFMpeg {

	public static String recordFileName = "VideoVocabularyRecorded.mp4";
	public static String convertFileName = "VideoVocabularyConverted.mp4";

    public static void installFfmpeg(Context context) {
        /*String mFfmpegInstallPath = context.getFilesDir().toString() + File.separator + "ffmpeg";
        File ffmpegFile = new File(mFfmpegInstallPath);
        Log.d(FFMpeg.class.getName(), "ffmpeg install path: " + mFfmpegInstallPath);

        if (!ffmpegFile.exists()) {
            try {
                ffmpegFile.createNewFile();
            } catch (IOException e) {
                Log.e(FFMpeg.class.getName(), "Failed to create new file!", e);
            }
            Utils.installBinaryFromRaw(context, R.raw.ffmpeg, ffmpegFile);
        } else {
            Log.d(FFMpeg.class.getName(), "It was already installed");
        }

        ffmpegFile.setExecutable(true);
        Log.d(FFMpeg.class.getName(), String.valueOf(ffmpegFile.canExecute()));

        try { Utils.copyLogo(context, R.raw.logo, new File(mFfmpegInstallPath+".png")); }
        catch (IOException e) { e.printStackTrace(); }

        try { Utils.copyLogo(context, R.raw.sound, new File(mFfmpegInstallPath+".mp3")); }
        catch (IOException e) { e.printStackTrace(); }

        try { Utils.copyLogo(context, R.raw.fonts, new File(mFfmpegInstallPath+".xml")); }
        catch (IOException e) { e.printStackTrace(); }*/
    }

    public static void convertAndShare(final Context context, final String videoPath, final String width, final String height) {
        final String mFfmpegInstallPath = context.getFilesDir().toString() + File.separator + "ffmpeg";
        if (!new File(mFfmpegInstallPath).exists()) installFfmpeg(context);
        final String outPath = context.getExternalFilesDir(null).toString() + File.separator + FFMpeg.convertFileName;
        final String subtitlePath = context.getExternalFilesDir(null).toString() + File.separator + FFMpeg.recordFileName + ".ass";
        final ProgressDialog barProgressDialog = new ProgressDialog(context);

        barProgressDialog.setTitle("Converting");
        barProgressDialog.setMessage("It may take more than 1 minute...");
        barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        barProgressDialog.setProgress(0);
        barProgressDialog.setMax(100);
        barProgressDialog.show();

        new Thread() {
            public void run() {
                try {
                    double totalSecs = 0;

                    ArrayList<String> cmd = new ArrayList<String>();
                    cmd.add(mFfmpegInstallPath);
                    cmd.add("-y");
                    cmd.add("-f");
                    cmd.add("lavfi");
                    cmd.add("-i");
                    cmd.add("color=color=white:s="+width+"x"+height+":d="+ String.valueOf(20));
                    //cmd.add("-f");
                    //cmd.add("lavfi");
                    //cmd.add("-i");
                    //cmd.add("aevalsrc=0::d="+ String.valueOf(totalSecs));

                    cmd.add("-i");
                    cmd.add("ffmpeg.mp3");  //1
                    cmd.add("-i");
                    cmd.add("ffmpeg.png");  //2
                    cmd.add("-i");
                    cmd.add(videoPath); //3
                    cmd.add("-filter_complex");
                    //String drawtext = "[1:v]transpose=2 [rotate];[rotate]drawtext=fontfile=/system/fonts/DroidSansFallback.ttf:text='" + hskkQuestion + "\n" + getCards().get(currentIndex).getWord() + "':x=(w-text_w)/2:y=h-text_h-line_h:fontcolor=0xFFFFFFFF:fontsize=30:shadowcolor=0x000000EE:shadowx=1:shadowy=1[text];[text][0:v]overlay=10:10 [vert];[vert]transpose=1 [filtered]" ;
                    //String drawtext = "[3:v] [0:0] overlay [cv]; [cv]transpose=2 [rotate];[rotate]subtitles='" + subtitlePath + "'[text];[text][2:v]overlay=10:10 [vert];[vert]transpose=1  [v]" ;
                    String drawtext = "[3:v] setsar=sar=1 [sar]; [0:v] [1:a] [sar] [3:a] concat=n=2:v=1:a=1 [out] [a]; [out] ass='" + subtitlePath + "'[text];[text][2:v] overlay=30:30 [v]" ;
                    cmd.add(drawtext);
                    cmd.add("-map");
                    cmd.add("[v]");
                    cmd.add("-map");
                    cmd.add("[a]");
                    cmd.add("-c:v");
                    cmd.add("libx264");
                    cmd.add("-c:a");
                    cmd.add("aac");
                    cmd.add("-strict");
                    cmd.add("-2");
                    cmd.add("-threads");
                    cmd.add("5");
                    cmd.add("-preset");
                    cmd.add("ultrafast");
//                    cmd.add("-metadata:s:v:0");
//                    cmd.add("rotate=270");
                    cmd.add("-metadata");
                    cmd.add("title=\"Video Vocabulary\"");
                    cmd.add(outPath);

//                    cmd.clear();
//                    cmd.add(mFfmpegInstallPath);
//                    cmd.add("-version");

                    ProcessBuilder b = new ProcessBuilder(cmd).directory(new File(context.getFilesDir().toString())).redirectErrorStream(true);
                    Map<String, String> env = b.environment();
                    env.put("FONTCONFIG_FILE", context.getFilesDir().toString() + "/ffmpeg.xml");
                    for (String key : env.keySet()) System.out.println(key + ": " + env.get(key));
                    Process ffmpegProcess = b.start();

                    String line;
                    BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()));

                    Pattern durPattern = Pattern.compile("Duration: ([\\d:\\.]+)");
                    Pattern timePattern = Pattern.compile("time=([\\d:\\.]+)");

                    Log.d(FFMpeg.class.getName(), "*******Starting FFMPEG");
                    while((line = reader.readLine())!=null){
                        Log.d(FFMpeg.class.getName(), "***" + line + "***");
                        //barProgressDialog.setMessage("asdf");
                        Matcher matcher = durPattern.matcher(line);
                        if (matcher.find()) {
                            String dur = matcher.group(1);
                            System.out.println("duration: " + dur);
                            //if (dur == null) throw new RuntimeException("Could not parse duration.");
                            String[] hms = dur.split(":");
                            totalSecs += Integer.parseInt(hms[0]) * 3600 //02:53.19 03:33.2
                                    + Integer.parseInt(hms[1]) *   60
                                    + Double.parseDouble(hms[2]);
                            System.out.println("Total duration: " + totalSecs + " seconds.");
                        }
                        matcher = timePattern.matcher(line);
                        if (matcher.find()) {
                            String match = matcher.group(1);
                            String[] matchSplit = match.split(":");
                            double currentSecs = Integer.parseInt(matchSplit[0]) * 3600 +
                                    Integer.parseInt(matchSplit[1]) * 60 +
                                    Double.parseDouble(matchSplit[2]);
                            double progress = currentSecs / totalSecs;
                            //System.out.println(  match + " = " + String.valueOf(currentSecs) + " / " + String.valueOf(totalSecs));
                            System.out.printf("Progress: " + String.valueOf(currentSecs) + " / " + String.valueOf(totalSecs) + " =  %.2f%%%n", progress * 100);
                            barProgressDialog.setProgress((int) Math.round(progress * 100));
                        }

                    }
                    Log.d(FFMpeg.class.getName(), "****ending FFMPEG****");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                barProgressDialog.dismiss();

                ContentValues content = new ContentValues(4);
                content.put(MediaStore.Video.VideoColumns.DATE_ADDED, System.currentTimeMillis() / 1000);
                content.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                content.put(MediaStore.Video.Media.DATA, outPath);
                ContentResolver resolver = context.getContentResolver();
                Uri uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, content);

                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("video/*");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, "Motherfucker text");
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Fuck you subject");
                sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
                context.startActivity(Intent.createChooser(sharingIntent, context.getResources().getString(R.string.dlgTitleShare)));
            }
        }.start();

    }

    /*public static void executeFFmpeg(final Context context, final String videoPath, final String width, final String height) {
        final String outPath = directory + "/BIT_Vocabulary.mp4";
        String subtitlePath = outPath.substring(0, outPath.lastIndexOf(".")) + ".srt";
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            double totalSecs = 20;

            String command = "-y -f lavfi color=color=white:s=" + width + "x" + height + ":d=" + String.valueOf(totalSecs);
            command += " -f lavfi -i aevalsrc=0::d=" + String.valueOf(totalSecs);
            command += " -i ffmpeg.png";
            command += " -i " + videoPath;
            command += " -filter_complex \"[3:v] setsar=sar=1 [sar]; [0:v] [1:a] [sar] [3:a] concat=n=2:v=1:a=1 [out] [a]; [out] subtitles='" + subtitlePath + "'[text];[text][2:v] overlay=10:10 [v]\"";
            command += " -map [v] -map [a]";
            command += " -c:v libx264 -c:a aac -strict -2";
            command += " -threads 5 -preset ultrafast";
            command += " -metadata title=\"Video Vocabulary\"";
            command += " outPath";

            //Map<String, String> env = b.environment();
            Map<String, String> env = System.getenv();
            //env.put("FONTCONFIG_FILE", context.getCacheDir().toString() + "/ffmpeg.xml");
            //for (String key : env.keySet()) System.out.println(key + ": " + env.get(key));
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                    Log.d(FFMpeg.class.getName(), "*******Starting FFMPEG");
                }

                @Override
                public void onProgress(String message) {
                    Log.d(FFMpeg.class.getName(), message);
                }

                @Override
                public void onFailure(String message) {
                    Log.d(FFMpeg.class.getName(), message);
                }

                @Override
                public void onSuccess(String message) {}

                @Override
                public void onFinish() {
                    //Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                    Log.d(FFMpeg.class.getName(), "****Finishing FFMPEG****");
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
            Toast.makeText(context, "FFmpeg is already running", Toast.LENGTH_LONG).show();
        }
    }*/
}