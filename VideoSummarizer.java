import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Arrays;
import java.io.File;

import javax.lang.model.util.ElementScanner6;

public class VideoSummarizer {
   static String videoDir;
   static String audioDir;
   static final int TOTAL_FRAMES = 16200;

   public static void main(String[] args) throws PlayWaveException, IOException {
      if(args.length == 3)
      {
         playExistedFile(args[0], args[1], args[2]);
         return;
      }
      if (args.length < 2) {
         System.err.println("Generate a summary and play: java VideoSummarizer [VideoFileDirectory(.rgb)] [AudioFile]");
         System.err.println("Play a video on .rgb directory: java VideoSummarizer [VideoFileDirectory(.rgb)] [AudioFile] 0");
         System.err.println("Play a summary video on .txt frame file: java VideoSummarizer [VideoFileDirectory(.rgb)] [GeneratedAudioFile] [VideoFrameLabel(.txt)]");
         return;
      }
      videoDir = args[0];
      audioDir = args[1];

      System.out.println("VideoSummarizer is now working...");

      ImageDisplay ren = new ImageDisplay();
      ren.initialize(args[0]);

      System.out.println("Working to detect shots...");
      ArrayList<Integer> breaks = ren.detectShots();
      breaks.add(TOTAL_FRAMES);

      System.out.println("Detection finished, working to generate audio weights...");
      AudioAnalyze audioAnalyze = new AudioAnalyze(audioDir);
      ArrayList<Double> audioWeights = audioAnalyze.getAudioWeights(breaks);

      System.out.println("Working to select final shots...");
      ArrayList<Integer> finalShots = ren.getFinalShots(audioWeights);
      
      CreateWaveFile createWaveFile = new CreateWaveFile(audioDir);
      String audioFile = createWaveFile.writeNewWavFile(finalShots);

      AVPlayer avPlayer = new AVPlayer();
      avPlayer.playSummaryVideo(args[0], audioFile, ren.getFlag(), ren.getNumOfFinalFrames());
   }

   public static void playExistedFile(String videoDir, String audioFile, String txtFile) throws PlayWaveException, IOException {
      int[] flag = new int[TOTAL_FRAMES];
      int numFinalFrames = 0;
      if(!txtFile.equals("0"))
      {
         Scanner in = new Scanner(new File(txtFile));
         while(in.hasNextInt())
         {
            flag[in.nextInt()] = 1;
            numFinalFrames++;
         }
      }
      else
      {
         Arrays.fill(flag, 1);
         numFinalFrames = TOTAL_FRAMES;
      }
      AVPlayer avPlayer = new AVPlayer();
      avPlayer.playSummaryVideo(videoDir, audioFile, flag, numFinalFrames);
   }

}
