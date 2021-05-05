import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class VideoSummarizer {
   static String videoDir;
   static String audioDir;

   public static void main(String[] args) throws PlayWaveException, IOException {
      if (args.length < 2) {
         System.err.println("usage: java VideoSummarizer [VideoFileDirectory(.rgb)] [AudioFile]");
         return;
      }
      videoDir = args[0];
      audioDir = args[1];

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

      ArrayList<Integer> breaks = ren.detectShots();
      breaks.add(16200);
      AudioAnalyze audioAnalyze = new AudioAnalyze(audioDir);
      ArrayList<Double> audioWeights = audioAnalyze.getAudioWeights(breaks);
      System.out.println();
      System.out.print("audioWeights: ");
      for (double i : audioWeights)
         System.out.print(i + " ");
      System.out.println();

      ArrayList<Integer> finalShots = ren.getFinalShots(audioWeights);
      //Mock finalShots
      //ArrayList<Integer> finalShots = new ArrayList<>(Arrays.asList(0, 120, 1200, 1320));

      CreateWaveFile createWaveFile = new CreateWaveFile(audioDir);
      createWaveFile.writeNewWavFile(finalShots);

      FileInputStream inputStream;
      try {
         inputStream = new FileInputStream("outputAudio.wav");
         // inputStream = this.getClass().getResourceAsStream(filename);
      } catch (FileNotFoundException e) {
         e.printStackTrace();
         return;
      }

      // initializes the playSound Object
      PlaySound playSound = new PlaySound(inputStream);
      // plays the sound
      try {
         playSound.play();
      } catch (PlayWaveException e) {
         e.printStackTrace();
         return;
      }

      // temporarily changed ImageDisplay.run() to play the newly generated video
      ren.start();
      playSound.start();

//      String[] args2 = {"../project_dataset/frames_rgb/soccer/", "outputAudio.wav"};
//      AVPlayer.main(args2);

   }

}
