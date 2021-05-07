import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;

public class AVPlayer {
   static final int WIDTH = 320;
   static final int HEIGHT = 180;

   static AVPlayer avPlayer;
   static String videoDir;
   static String audioDir;
   static FileInputStream inputStream;
   static int[] flag = new int[16200];
   JFrame frame;
   JLabel lbIm1;
   BufferedImage img;
   AudioThread audioThread;
   ImageThread imageThread;
   int status = 2;
   public long startTime;
   int numOfFinalFrames = 16200;

   public static void main(String[] args) {
      if (args.length < 2) {
         System.err.println("usage: java AVPlayer [VideoFileDirectory(.rgb)] [AudioFile]");
         return;
      }

      videoDir = args[0];
      audioDir = args[1];

      avPlayer = new AVPlayer();
      Arrays.fill(flag, 1);
      avPlayer.showIms();
   }

   public void showIms() {
      // Use label to display the image
      frame = new JFrame("Basic Player");
      frame.setLocation(300, 300);

      GridBagLayout gLayout = new GridBagLayout();
      frame.getContentPane().setLayout(gLayout);

      JLabel lbText1 = new JLabel(" Video Path = " + videoDir);
      lbText1.setHorizontalAlignment(SwingConstants.LEFT);
      JLabel lbText2 = new JLabel(" Audio Path = " + audioDir);
      lbText2.setHorizontalAlignment(SwingConstants.LEFT);

      img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
      lbIm1 = new JLabel(new ImageIcon(img));

      JButton pauseButton = new JButton("Play/Pause");
      pauseButton.setHorizontalAlignment(SwingConstants.CENTER);
      pauseHandler pauseHandle = new pauseHandler();
      pauseButton.addActionListener(pauseHandle);

      JButton stopButton = new JButton("Stop");
      pauseHandler stopHandle = new pauseHandler();
      stopButton.setHorizontalAlignment(SwingConstants.CENTER);
      stopButton.addActionListener(stopHandle);

      GridBagConstraints c = new GridBagConstraints();

      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 0.5;
      c.gridx = 0;
      c.gridy = 3;
      frame.getContentPane().add(pauseButton, c);

      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 0.5;
      c.gridx = 1;
      c.gridy = 3;
      frame.getContentPane().add(stopButton, c);

      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 0.0;
      c.gridx = 0;
      c.gridy = 0;
      c.gridwidth = 2;
      frame.getContentPane().add(lbText1, c);

      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 0.0;
      c.gridx = 0;
      c.gridy = 1;
      c.gridwidth = 2;
      frame.getContentPane().add(lbText2, c);

      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridx = 0;
      c.gridy = 2;
      c.gridwidth = 2;
      c.weightx = 0.0;
      frame.getContentPane().add(lbIm1, c);

      frame.pack();
      frame.setVisible(true);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   }

   public void setFlag(int[] flag) {
      this.flag = flag;
   }

   public void setNumOfFinalFrames(int numOfFinalFrames) {
      this.numOfFinalFrames = numOfFinalFrames;
   }

   public void playSummaryVideo(String videoDir, String audioDir, int[] flag, int numOfFinalFrames) {
      avPlayer = new AVPlayer();
      avPlayer.videoDir = videoDir;
      avPlayer.audioDir = audioDir;
      avPlayer.setFlag(flag);
      avPlayer.setNumOfFinalFrames(numOfFinalFrames);
      avPlayer.showIms();
   }

   private class pauseHandler implements ActionListener {
      public void actionPerformed(ActionEvent e) {
         JButton x = (JButton) e.getSource();
         String buttonText = x.getText();
         if (buttonText.equals("Play/Pause")) {
            if (status == 1) {
               startTime = System.currentTimeMillis();
               status = 0;
            } else {
               status = 1;
               startTime = System.currentTimeMillis();
               System.out.println("LOG: startTime=" + startTime);
               if (audioThread == null) {
                  System.out.println("Begin");
                  RunAVThreads();
               } else if (!audioThread.isAlive()) {
                  System.out.println("Not Alive");
                  RunAVThreads();
               }
            }
            System.out.println("status: " + status);
         }
         if (buttonText.equals("Stop")) {
            status = 2;
            System.out.println("status: " + status);
         }
      }

      private void RunAVThreads() {
         try {
            inputStream = new FileInputStream(audioDir);
         } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
         }

         audioThread = new AudioThread(inputStream, avPlayer);
         imageThread = new ImageThread(videoDir, avPlayer);
         imageThread.setFlag(flag);
         imageThread.setNumOfFinalFrames(numOfFinalFrames);

         try {
            audioThread.play();
         } catch (PlayWaveException e) {
            e.printStackTrace();
            return;
         }
         audioThread.start();
         imageThread.start();
      }

   }
}
