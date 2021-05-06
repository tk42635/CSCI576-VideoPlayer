import javax.sound.sampled.*;
import javax.sound.sampled.DataLine.Info;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AudioThread extends Thread {

   //private final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb
   private final int EXTERNAL_BUFFER_SIZE = 9600;

   AVPlayer avPlayer;
   private final InputStream waveStream;
   private SourceDataLine dataLine;
   private AudioInputStream audioInputStream;

   public AudioThread(InputStream inputStream, AVPlayer avPlayer) {
      this.waveStream = inputStream;
      this.avPlayer = avPlayer;
      this.dataLine = null;
      this.audioInputStream = null;
   }

   public void play() throws PlayWaveException {
      try {
         InputStream bufferedIn = new BufferedInputStream(this.waveStream);
         audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
      } catch (UnsupportedAudioFileException e1) {
         throw new PlayWaveException(e1);
      } catch (IOException e1) {
         throw new PlayWaveException(e1);
      }

      // Obtain the information about the AudioInputStream
      AudioFormat audioFormat = audioInputStream.getFormat();
      Info info = new Info(SourceDataLine.class, audioFormat);

      // opens the audio channel
      try {
         dataLine = (SourceDataLine) AudioSystem.getLine(info);
         //System.out.println(dataLine.getFormat());
         //System.out.println(dataLine.getBufferSize()); // 192000
         dataLine.open(audioFormat, this.EXTERNAL_BUFFER_SIZE);
         //dataLine.open(audioFormat, 9600);
      } catch (LineUnavailableException e1) {
         throw new PlayWaveException(e1);
      }

      // Starts the music :P
      dataLine.start();
   }

   public void run() {
      try {
         int readBytes = 0;
         byte[] audioBuffer = new byte[this.EXTERNAL_BUFFER_SIZE];

         long start = System.currentTimeMillis();
         System.out.println("audioStart " + start);
         try {

            while (readBytes != -1) {
               if (avPlayer.status == 1) {
                  //dataLine.start();
                  //int availableNum = dataLine.available();
                  //readBytes = audioInputStream.read(audioBuffer, 0, availableNum);
                  readBytes = audioInputStream.read(audioBuffer, 0, audioBuffer.length);
                  if (readBytes > 0) {
                     dataLine.write(audioBuffer, 0, readBytes);
                  }
               } else if (avPlayer.status == 0) {
                  //dataLine.stop();
                  Thread.sleep(1);
               } else {
                  //dataLine.stop();
                  return;
               }
            }

            long end = System.currentTimeMillis();
            System.out.println("audioEnd " + end);
            System.out.println((end - start) / 1000.0);
         } catch (IOException e1) {
            throw new PlayWaveException(e1);
         } finally {
            dataLine.drain();
            dataLine.close();

            long drainTime = System.currentTimeMillis();
            System.out.println("audioDrain " + drainTime);
            System.out.println((drainTime - start) / 1000.0);
         }
      } catch (Exception e) {
         // Throwing an exception
         System.out.println("Exception is caught");
         e.printStackTrace();
      }
   }

}
