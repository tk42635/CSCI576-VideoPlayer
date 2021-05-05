import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.ArrayList;

public class AudioAnalyze {
   private static File audioFile;
   private final double FPS = 30; // Frames Per Second
   private final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb

   private InputStream waveStream;
   private AudioInputStream audioInputStream;

   public AudioAnalyze(String audioDir) throws PlayWaveException {
      try {
         audioFile = new File(audioDir);
         this.waveStream = new FileInputStream(audioDir);
      } catch (FileNotFoundException e) {
         e.printStackTrace();
         return;
      }

      try {
         InputStream bufferedIn = new BufferedInputStream(this.waveStream);
         this.audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
      } catch (UnsupportedAudioFileException e1) {
         throw new PlayWaveException(e1);
      } catch (IOException e1) {
         throw new PlayWaveException(e1);
      }
   }

   public ArrayList<Double> getAudioWeights(ArrayList<Integer> breaks) throws PlayWaveException {
      AudioFormat audioFormat = audioInputStream.getFormat();
      long totalFileBytes = audioFile.length();                      // Total original audio file length in bytes
      int numOfChannels = audioFormat.getChannels();                 // Number of Channels (= 2)
      double bytesPerSample = audioFormat.getFrameSize();            // Number of bytes per sample (= 4)
      double sampleRate = audioFormat.getFrameRate();                // Sample Rate (= 48000 Hz)
      double bytesPerSecond = sampleRate * bytesPerSample;           // 48000 * 4 = 192000
      double bytesPerVideoFrame = bytesPerSample * sampleRate * (1 / FPS); // 4 * 48000 / 30 = 6400

      System.out.println("totalFileBytes: " + totalFileBytes);
      System.out.println("numOfChannels: " + numOfChannels);
      System.out.println("bytesPerSample: " + bytesPerSample);
      System.out.println("sampleRate: " + sampleRate);
      System.out.println("bytesPerSecond: " + bytesPerSecond);
      System.out.println("bytesPerVideoFrame: " + bytesPerVideoFrame);
      System.out.println("-------------------------------------");

      ArrayList<Double> audioWeights = new ArrayList<Double>();

      int readBytes = 0;
      byte[] audioBuffer = new byte[EXTERNAL_BUFFER_SIZE];
      long byteCount = 0;
      int index = 0;
      double weightTotal = 0;
      int weightCount = 0;
      double max = Double.NEGATIVE_INFINITY;
      long breakPoint = (long) (breaks.get(index) * bytesPerVideoFrame);
      index++;

      try {
         while (readBytes != -1) {
            readBytes = audioInputStream.read(audioBuffer, 0, audioBuffer.length); //read raw data

            //System.out.print(" readBytes: "+readBytes);
            // More bytes to read from the audio input stream
            if (readBytes >= 0) {

               // Scan through only the MSBs of the buffer (every other odd byte)
               for (int i = 1; i < readBytes; i += 2) {
                  // If we have not reached the break point yet, keep summing for the average calculation
                  if (byteCount < breakPoint) {
                     weightTotal += Math.abs(audioBuffer[i]);
                     weightCount++;
                  }
                  // At the breakpoint, calculate the average for the previous shot and reset the average weight counters for the next shot
                  else {
                     System.out.print(" index: " + index);
                     System.out.print(" break: " + breakPoint);
                     System.out.print(" endFrame: " + breakPoint / 6400);
                     if (index < breaks.size()) {
                        System.out.print(" Next endFrame: " + breaks.get(index));
                     }
                     System.out.print(" add: " + weightTotal / weightCount);
                     System.out.println(" byteCount: " + byteCount);

                     audioWeights.add(weightTotal / weightCount);
                     weightTotal = 0;
                     weightTotal += Math.abs(audioBuffer[i]);
                     weightCount = 0;
                     weightCount++;
                     //index++;
                     if (index < breaks.size()) {
                        breakPoint = (long) (breaks.get(index) * bytesPerVideoFrame);
                        index++;
                     } else {
                        readBytes = -1;
                     }
                  }
                  byteCount += 2;
               }
            }
         }

         System.out.println(" byteCount: " + byteCount);

         while (audioWeights.size() < breaks.size()) {
            System.out.println(" ---------------");
            System.out.print(" index: " + index);
            System.out.print(" break: " + breakPoint);
            System.out.print(" add: " + weightTotal / weightCount);
            System.out.print(" byteCount: " + byteCount);
            System.out.print(" weightTotal: " + weightTotal);
            System.out.println(" weightCount: " + weightCount);
            audioWeights.add(weightTotal / weightCount);
         }

         // Normalize the audio weights
         for (int i = 0; i < audioWeights.size(); i++) {
            if (audioWeights.get(i) > max) {
               max = audioWeights.get(i);
            }
         }
         System.out.println();
         System.out.println("MAX: " + max);
         System.out.println("audioWeights.size(): " + audioWeights.size());

         for (int i = 0; i < audioWeights.size(); i++) {
            double val = audioWeights.get(i);
            //System.out.println(val);
            //audioWeights.set(i, max / val);
            audioWeights.set(i, val / max);
         }

      } catch (IOException e1) {
         throw new PlayWaveException(e1);
      }
      return audioWeights;
   }

}
