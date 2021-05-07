import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;

import javax.lang.model.util.ElementScanner6;

public class VideoSummarizer {
   static String videoDir;
   static String audioDir;

   public static void main(String[] args) throws PlayWaveException, IOException {
      if(args.length == 1)
      {
         playExistedFile(Integer.valueOf(args[0]));
         return;
      }
      if (args.length < 2) {
         System.err.println("usage: java VideoSummarizer [VideoFileDirectory(.rgb)] [AudioFile]");
         return;
      }
      videoDir = args[0];
      audioDir = args[1];

      System.out.println("VideoSummarizer is now working...");

      ImageDisplay ren = new ImageDisplay();
      ren.initialize(args[0]);

      // tmp code for testing purpose
      /*
      ArrayList<Integer> breaks = new ArrayList<>();
      BufferedReader in = new BufferedReader(new FileReader("./boundary.txt"));
      String str;
      while ((str = in.readLine()) != null) {
         breaks.add(Integer.valueOf(str));
      }
      breaks.add(16200);
      for (int i : breaks)
         System.out.print(i + " ");
       */

      System.out.println("Working to detect shots...");
      ArrayList<Integer> breaks = ren.detectShots();
      breaks.add(16200);

      System.out.println("Detection finished, working to generate audio weights...");
      AudioAnalyze audioAnalyze = new AudioAnalyze(audioDir);
      ArrayList<Double> audioWeights = audioAnalyze.getAudioWeights(breaks);

//      System.out.print("audioWeights: ");
//      for (double i : audioWeights)
//         System.out.print(i + " ");
//      System.out.println();

      System.out.println("Working to select final shots...");
      ArrayList<Integer> finalShots = ren.getFinalShots(audioWeights);
      //Mock finalShots
      //ArrayList<Integer> finalShots = new ArrayList<>(Arrays.asList(0, 120, 1200, 1320));

      CreateWaveFile createWaveFile = new CreateWaveFile(audioDir);
      createWaveFile.writeNewWavFile(finalShots);

      //Mock Final Frames for testing
      /*
      int[] flag = new int[16200];
      // tmp for test
      BufferedReader in = new BufferedReader(new FileReader("./tmpFrames.txt"));
      String str;

      while ((str = in.readLine()) != null) {
         int frame = Integer.valueOf(str);
         flag[frame] = 1;
      }
      ren.setFlag(flag);
       */

      AVPlayer avPlayer = new AVPlayer();

//      for (int i : ren.getFlag()) {
//         System.out.println(i);
//      }

      avPlayer.playSummaryVideo(args[0], "outputAudio_2.wav", ren.getFlag(), ren.getNumOfFinalFrames());
   }

   public static void playExistedFile(int testID) throws PlayWaveException, IOException {
      int[] flag = new int[16200];
      int numFinalFrames = 0;
      String videoDir, audioFile;
      Scanner in;
      if(testID == 1)
      {
         in = new Scanner(new File("./test_frames_1.txt"));
         videoDir = "D:/MyDownloads/Download/test_data/test_data/frames_rgb_test/test_video/";
         audioFile = "./outputAudio_1.wav";
      }
      else if(testID == 2)
      {
         in = new Scanner(new File("./test_frames_2.txt"));
         videoDir = "D:/MyDownloads/Download/test_data_3/test_data_3/frames_rgb_test/test_video_3/";
         audioFile = "./outputAudio_2.wav";
      }
      else
      {
         System.err.println("No such video file!");
         return;
      }
      while(in.hasNextInt())
      {
         flag[in.nextInt()] = 1;
         numFinalFrames++;
      }
      AVPlayer avPlayer = new AVPlayer();
      //System.out.println(numFinalFrames);
      avPlayer.playSummaryVideo(videoDir, audioFile, flag, numFinalFrames);
   }

}
