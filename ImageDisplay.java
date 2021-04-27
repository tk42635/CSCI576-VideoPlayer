
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.lang.model.util.ElementScanner6;
import javax.swing.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;


public class ImageDisplay extends Thread{

	JFrame frame;
	JLabel lbIm1;
	BufferedImage[] video;
	int width = 320;
	int height = 180;

	String videodir;

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String path, BufferedImage img1)
	{
		try
		{
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
			for(int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++) {
					byte r = bytes[ind];
					byte g = bytes[ind + height * width];
					byte b = bytes[ind + height * width * 2];

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				
					img1.setRGB(x, y, pix);
					ind++;
				}
			}
			
			
			
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public void showIms(String videodir){


		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);


		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		lbIm1 = new JLabel(new ImageIcon());
		frame.getContentPane().add(lbIm1);

		video = new BufferedImage[16200];
		this.videodir = videodir;
	

	}

	public void run()
    {
        try {
            long start = System.currentTimeMillis();
			// Read in the specified image
			for(int i = 0; i < 16200; i++)
			{
				video[i] = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				readImageRGB(width, height, videodir + "frame" + i + ".rgb", video[i]);
				//System.out.println("Frame " + i + " done!");
				lbIm1.setIcon(new ImageIcon(video[i]));
				frame.pack();
				frame.setVisible(true);
				System.out.println("Frame " + i + " showed!");
				long end = start + (i / 3) * 100 + (i % 3 == 1? 34 : (i % 3 == 2? 33 : 0));
				while(System.currentTimeMillis() < end)
				{

				}
			}
        }
        catch (Exception e) {
            // Throwing an exception
            System.out.println("Exception is caught");
        }
    }


}
