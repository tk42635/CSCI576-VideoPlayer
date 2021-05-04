import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;

public class CreateWaveFile {
   private static File audioFile;
   private final double FPS = 30; // Frames Per Second
   private final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb
   private double bytesPerVideoFrame;
   private InputStream waveStream;
   private AudioInputStream audioInputStream;

   public CreateWaveFile(String audioDir) throws PlayWaveException {
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

   public void writeNewWavFile(ArrayList<Integer> finalShots) throws PlayWaveException {
      try {
         AudioFormat audioFormat = audioInputStream.getFormat();

         FileOutputStream outFileStream;
         File audioOut;

         long totalFileBytes = audioFile.length();                      // Total original audio file length in bytes
         double bytesPerSample = audioFormat.getFrameSize();            // Number of bytes per sample (= 4)
         double sampleRate = audioFormat.getFrameRate();
         // Sample Rate (= 48000 Hz)
         double bytesPerSecond = sampleRate * bytesPerSample;           // 48000 * 4 = 192000
         bytesPerVideoFrame = bytesPerSample * sampleRate * (1 / FPS);  // 4 * 48000 / 30 = 6400

         audioOut = new File("audioTemp.Bytes");
         boolean success = audioOut.createNewFile();
         if (!success) {
            audioOut.delete();
            audioOut.createNewFile();
         }
         if (!(audioOut.canWrite())) {
            System.err.println("Cannot create output audio file..");
         }

         outFileStream = new FileOutputStream(audioOut, true);

         int count = 0;
         long firstByte = 0;
         long lastByte = 0;
         if (finalShots.size() != 0) {
            firstByte = framesToBytes(finalShots.get(count));
            lastByte = framesToBytes(finalShots.get(count + 1));
            count += 2;
         }

         // Calculate final number of audio bytes in the summary
         long totalBytes = 0;
         for (int i = 0; i < finalShots.size() - 1; i += 2) {
            totalBytes += framesToBytes(finalShots.get(i + 1) - finalShots.get(i));
         }

         long currByte = 0;
         int readBytes = 0;
         byte[] audioBuffer = new byte[EXTERNAL_BUFFER_SIZE];
         System.out.println("Writing Audio...0%");
         while (readBytes != -1) {
            readBytes = audioInputStream.read(audioBuffer, 0, audioBuffer.length);
            if (readBytes >= 0) {
               for (int i = 1; i < readBytes; i += 2) { // Each sample is 2 bytes

                  if ((currByte > lastByte) && (count < finalShots.size())) {
                     System.out.println("Writing Audio..." + Math.round(100 * (double) currByte / (double) totalFileBytes) + "%");
                     firstByte = framesToBytes(finalShots.get(count));
                     lastByte = framesToBytes(finalShots.get(count + 1));
                     count += 2;
                  }

                  if ((currByte >= firstByte) && (currByte <= lastByte)) {
                     outFileStream.write(audioBuffer[i - 1]);
                     outFileStream.write(audioBuffer[i]);
                  }

                  if ((count >= finalShots.size()) && (currByte > lastByte)) {
                     readBytes = -1;
                  }
                  currByte += 2;
               }
            }
         }

         int minutes = (int) Math.floor((double) totalBytes / (bytesPerSecond * 60.0));
         int seconds = (int) Math.floor(((double) totalBytes / bytesPerSecond) - 60.0 * minutes);
         if (seconds < 10) {
            System.out.println(minutes + ":0" + seconds);
         } else {
            System.out.println(minutes + ":" + seconds);
         }

         FileInputStream fis = new FileInputStream(audioOut);
         AudioInputStream audioStream = new AudioInputStream(fis, audioInputStream.getFormat(), audioOut.length() / audioInputStream.getFormat().getFrameSize());

         File audioOutWave = new File("outputAudio.wav");
         boolean successWav = audioOutWave.createNewFile();
         if (!successWav) {
            audioOutWave.delete();
            audioOutWave.createNewFile();
         }
         AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, audioOutWave);

         fis.close();
         outFileStream.close();
         audioOut.delete();

      } catch (FileNotFoundException e) {
         e.printStackTrace();
         return;
      } catch (IOException e1) {
         throw new PlayWaveException(e1);
      }
   }

   private long framesToBytes(double frames) {
      return (long) (frames * bytesPerVideoFrame);
   }
}
