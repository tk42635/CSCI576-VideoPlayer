import java.util.ArrayList;
import java.util.Arrays;

public class VideoSummarizer {
   static String videoDir;
   static String audioDir;

   public static void main(String[] args) throws PlayWaveException {
      if (args.length < 2) {
         System.err.println("usage: java VideoSummarizer [VideoFileDirectory(.rgb)] [AudioFile]");
         return;
      }

      videoDir = args[0];
      audioDir = args[1];

      //Mock Results!
      ArrayList<Integer> breaks = new ArrayList<>(Arrays.asList(0, 2, 10, 100, 10000, 13000, 15000));
      AudioAnalyze audioAnalyze = new AudioAnalyze(audioDir);
      ArrayList<Double> audioWeights = audioAnalyze.getAudioWeights(breaks);

      System.out.println();
      System.out.print("audioWeights: ");
      for (double i : audioWeights)
         System.out.print(i + " ");
      System.out.println();

      //Mock Results!
      ArrayList<Integer> finalShots = new ArrayList<>(Arrays.asList(0, 120, 1200, 1320));
      CreateWaveFile createWaveFile = new CreateWaveFile(audioDir);
      createWaveFile.writeNewWavFile(finalShots);

      //TODO: change arguments
      String[] args2 = {"../project_dataset/frames_rgb/soccer/", "outputAudio.wav"};
      AVPlayer.main(args2);
   }

}
