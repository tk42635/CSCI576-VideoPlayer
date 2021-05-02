
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import javax.lang.model.util.ElementScanner6;
import javax.swing.*;
import javax.swing.tree.DefaultTreeCellEditor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ImageDisplay extends Thread {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage[] video;
	int width = 320;
	int height = 180;

	ArrayList<Integer> frameDiffs;
	static final int BLOCK_HEIGHT = 36;
	static final int BLOCK_WIDTH = 64;
	static final int SEARCH_RADIUS = 6;

	String videodir;

	/**
	 * Read Image RGB Reads the image of given width and height at the given imgPath
	 * into the provided BufferedImage.
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
			raf.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void initialize(String videodir) {

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

		frameDiffs = new ArrayList<>();
	}

	/**
	 * Calculate frame differences of every pair of adjacent frames
	 * divide frames into shots based on frame differences.
	 */
	public void detectShots() {
		try {
			double frameDiffMean = 0;
			for (int i = 0; i < 16200; i++) {
				if (i > 1) {
					// avoid out of memory error
					video[i - 2].flush();
					video[i - 2] = null;
				}
				video[i] = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				readImageRGB(width, height, videodir + "frame" + i + ".rgb", video[i]);

				if (i > 0) {
					int frameDiff = calcFrameDiff(i);
					frameDiffMean += frameDiff;
					frameDiffs.add(frameDiff);
				}
			}

			// Divide frames into shots based on frame diff;
			// Define threshold as (mean + 2 * standard deviation)
			frameDiffMean /= 16199.0;
			double frameDiffStandardDeviation = 0;
			for (int frameDiff : frameDiffs) {
				int deviation = (int) (frameDiff - frameDiffMean);
				frameDiffStandardDeviation += deviation * deviation;
			}
			frameDiffStandardDeviation = (long) Math.sqrt(frameDiffStandardDeviation / 16199.0);


			double threshold = frameDiffMean + 2 * frameDiffStandardDeviation;
			System.out.println("threshold: " + threshold);
			for (int i = 0; i < frameDiffs.size(); i++) {
				if (frameDiffs.get(i) > threshold) {
					System.out.println(i + " : " + frameDiffs.get(i));
				}
			}

		} catch (Exception e) {
			// Throwing an exception
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Divide the current frame into blocks, for every single block,
	 * search for the most similar block within a search area defined by SEARCH_RADIUS in the prev frame,
	 * add the sum of absolute difference (SAD) to the frame difference.
	 * Return the frame difference.
	 *
	 * @param currFrameIdx the current frame index
	 * @return the absolute difference between currFrame and prevFrame
	 */
	public int calcFrameDiff(int currFrameIdx) {
		BufferedImage prevFrame = video[currFrameIdx - 1];
		BufferedImage currFrame = video[currFrameIdx];

		int frameDiff = 0;

		// Traverse every block in the curr frame
		// x1, y1: the top left coordinate of a block in the curr frame
		for (int y1 = 0; y1 + BLOCK_HEIGHT < height; y1 += BLOCK_HEIGHT) {
			for (int x1 = 0; x1 + BLOCK_WIDTH < width; x1 += BLOCK_WIDTH) {

				int minBlockDiff = Integer.MAX_VALUE;

				// Calc the difference between a given block in the curr frame and candidate blocks in the prev frame
				// x0, y0: the top left coordinate of a block in the prev frame
				for (int y0 = y1 - SEARCH_RADIUS; y0 <= y1 + SEARCH_RADIUS; y0++) {
					for (int x0 = x1 - SEARCH_RADIUS; x0 <= x1 + SEARCH_RADIUS; x0++) {
						if (x0 < 0 || x0 >= width - BLOCK_WIDTH || y0 < 0 || y0 >= width - BLOCK_HEIGHT) {
							// out of bounds
							continue;
						}

						int blockDiff = 0;
						for (int deltaX = 0; deltaX < BLOCK_WIDTH; deltaX++) {
							for (int deltaY = 0; deltaY < BLOCK_HEIGHT; deltaY++) {
								int prevPixel = prevFrame.getRGB(x0 + deltaX, y0 + deltaY);
								int prevBlue = prevPixel & 0xff;
								int prevGreen = (prevPixel & 0xff00) >> 8;
								int prevRed = (prevPixel & 0xff0000) >> 16;

								int currPixel = currFrame.getRGB(x1 + deltaX, y1 + deltaY);
								int currBlue = currPixel & 0xff;
								int currGreen = (currPixel & 0xff00) >> 8;
								int currRed = (currPixel & 0xff0000) >> 16;

								blockDiff = Math.abs(currBlue - prevBlue) + Math.abs(currGreen - prevGreen)
								+ Math.abs(currRed - prevRed);
							}
						}

						minBlockDiff = Math.min(minBlockDiff, blockDiff);
					}
				}

				frameDiff += minBlockDiff;
			}
		}

		// System.out.println(frameDiff);
		return frameDiff;
	}

	public void run() {
		try {
			long start = System.currentTimeMillis();

			// Read in the specified image
			for (int i = 0; i < 16200; i++) {
				// System.out.println("Frame " + i + " done!");
				if (i > 0) {
					video[i - 1].flush();
					video[i - 1] = null;
				}
				video[i] = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				readImageRGB(width, height, videodir + "frame" + i + ".rgb", video[i]);
				// if (i > 0) {
				// detectShots(i);
				// }

				lbIm1.setIcon(new ImageIcon(video[i]));
				frame.pack();
				frame.setVisible(true);
				// System.out.println("Frame " + i + " showed!");
				long end = start + (i / 3) * 100 + (i % 3 == 1 ? 34 : (i % 3 == 2 ? 33 : 0));
				while (System.currentTimeMillis() < end) {

				}
			}
		} catch (Exception e) {
			// Throwing an exception
			System.out.println(e.getMessage());
			System.out.println("Exception is caught");
		}
	}

}
