import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageThread extends Thread {
   static final int WIDTH = 320;
   static final int HEIGHT = 180;

   String videoDir;
   AVPlayer avPlayer;
   BufferedImage[] video;

   int[] flag = new int[16200];
   int numOfFinalFrames = 16200;

   public ImageThread(String videoDir, AVPlayer avPlayer) {
      this.videoDir = videoDir;
      this.avPlayer = avPlayer;
      video = new BufferedImage[16200];
   }

//   public void run() {
//      try {
//         long start = System.currentTimeMillis();
//         int pausedFrame = 0;
//
//         System.out.println("videoStart " + start);
//         // Read in the specified image
//         for (int i = 0; i < 16200; i++) {
//            if (avPlayer.status == 1) {
//               if (i > 1) {
//                  video[i - 1].flush();
//                  video[i - 1] = null;
//               }
//
//               video[i] = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
//               readImageRGB(WIDTH, HEIGHT, videoDir + "frame" + i + ".rgb", video[i]);
//               avPlayer.lbIm1.setIcon(new ImageIcon(video[i]));
//
//               int j = i - pausedFrame;
//               long end = avPlayer.startTime + (j / 3) * 100 + (j % 3 == 1 ? 34 : (j % 3 == 2 ? 33 : 0));
//               while (System.currentTimeMillis() < end) { //for synchronization
//
//               }
//            } else if (avPlayer.status == 0) {
//               i--;
//               pausedFrame = i;
//               Thread.sleep(1);
//            } else {
//               avPlayer.lbIm1.setIcon(new ImageIcon(new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)));
//               return;
//            }
//         }
//
//         long end = System.currentTimeMillis();
//         System.out.println("videoEnd " + end);
//         System.out.println((end - start) / 1000.0);
//
//         while (true) {
//            if (avPlayer.status == 2) {
//               System.out.println("Reset after Finishing");
//               avPlayer.lbIm1.setIcon(new ImageIcon(new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)));
//               return;
//            }
//         }
//
//      } catch (Exception e) {
//         // Throwing an exception
//         System.out.println("Exception is caught");
//         e.printStackTrace();
//      }
//   }

   public void run() {
      try {
         Thread.sleep(100);
         long start = System.currentTimeMillis();
         avPlayer.startTime = start;
         int pausedFrame = 0;
         int count = 0;

         System.out.println("videoStart " + start);
         // Read in the specified image
         for (int i = 0; i < 16200 && count < numOfFinalFrames; i++) {

            if (avPlayer.status == 1) {
               if (flag[i] == 1) {
                  if (i > 1) {
                     if (video[i - 1] != null) {
                        video[i - 1].flush();
                        video[i - 1] = null;
                     }
                  }
                  video[i] = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
                  readImageRGB(WIDTH, HEIGHT, videoDir + "frame" + i + ".rgb", video[i]);
                  avPlayer.lbIm1.setIcon(new ImageIcon(video[i]));

                  count++;
                  int j = count - pausedFrame;

                  //System.out.println(System.currentTimeMillis());
                  long end = avPlayer.startTime + (j / 3) * 100 + (j % 3 == 1 ? 34 : (j % 3 == 2 ? 67 : 0));
            
                  while (System.currentTimeMillis() < end) { //for synchronization

                  }
                  // if(count % 100 == 0)
                  //    System.out.println("Count: " + count + "  Time elapsed: " + (System.currentTimeMillis() - start) + "  should be: " + (count / 3 * 100 + (count % 3 == 1 ? 34 : (count % 3 == 2 ? 67 : 0))));
               }

            } else if (avPlayer.status == 0) {
               i--;
               pausedFrame = count;
               Thread.sleep(1);
            } else {
               avPlayer.lbIm1.setIcon(new ImageIcon(new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)));
               return;
            }

         }


         long end = System.currentTimeMillis();
         System.out.println("videoEnd " + end);
         System.out.println((end - start) / 1000.0);

         while (true) {
            if (avPlayer.status == 2) {
               System.out.println("Reset after Finishing");
               avPlayer.lbIm1.setIcon(new ImageIcon(new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)));
               return;
            }
         }

      } catch (Exception e) {
         // Throwing an exception
         System.out.println("Exception is caught");
         e.printStackTrace();
      }
   }

   /**
    * Read Image RGB
    * Reads the image of given width and height at the given imgPath into the provided BufferedImage.
    */
   private void readImageRGB(int width, int height, String path, BufferedImage img1) {
      try {
         int frameLength = width * height * 3;

         Path p1 = Paths.get(path);
         File file = new File(p1.toString());
         RandomAccessFile raf = new RandomAccessFile(file, "r");
         raf.seek(0);

         long len = frameLength;
         byte[] bytes = new byte[(int) len];
         float[] HSB = new float[(int) len];

         raf.read(bytes);

         int ind = 0;
         for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
               byte r = bytes[ind];
               byte g = bytes[ind + height * width];
               byte b = bytes[ind + height * width * 2];

               int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);

               img1.setRGB(x, y, pix);
               ind++;
            }
         }

      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void setFlag(int[] flag) {
      this.flag = flag;
   }

   public void setNumOfFinalFrames(int numOfFinalFrames) {
      this.numOfFinalFrames = numOfFinalFrames;
      System.out.println("numOfFinalFrames: " + this.numOfFinalFrames);
   }
}
