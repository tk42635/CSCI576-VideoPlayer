
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
	ArrayList<Integer> shotBoundaryIdx;
	double[] frameAggregatedWeights;
	double[] frameMotionWeights;
	double[] frameHueWeights;
	Map<Double, int[]> weightedShots;
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

		shotBoundaryIdx = new ArrayList<>();
		frameDiffs = new ArrayList<>();
		frameAggregatedWeights = new double[16199];
		frameMotionWeights = new double[16199];
		frameHueWeights = new double[16199];
		weightedShots = new TreeMap<>(Collections.reverseOrder());
	}

	/**
	 * Calculate frame differences of every pair of adjacent frames
	 * divide frames into shots based on frame differences.
	 */
	public void detectShots() {
		try {
			double frameDiffMean = 0;
			double maxMotionWeight = 0;
			for (int i = 0; i < 16200; i++) {
				if (i > 1) {
					// avoid out of memory error
					video[i - 2].flush();
					//video[i - 2] = null;
				}
				video[i] = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				readImageRGB(width, height, videodir + "frame" + i + ".rgb", video[i]);

				if (i > 0) {
					// int frameDiff = calcFrameDiff(i);
					int[] res = calcFrameDiffBasedOnBlocks(i);
					frameDiffMean += res[0];
					frameDiffs.add(res[0]);
					maxMotionWeight = Math.max(maxMotionWeight, res[1]);
					frameMotionWeights[i-1] = (double)res[1];
					frameHueWeights[i-1] = getHueWeight(video[i]);
				}
			}

			for(int i = 0;  i < 16199; i++)
			{
				frameMotionWeights[i] /= maxMotionWeight;
				frameAggregatedWeights[i] = frameMotionWeights[i] * 70 + frameHueWeights[i] * 30;
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
			FileWriter boundary = new FileWriter("./boundary.txt");
			for (int i = 0; i < frameDiffs.size(); i++) {
				if (frameDiffs.get(i) > threshold) {
					shotBoundaryIdx.add(i + 1);
					boundary.write((i + 1) + "\n");
					System.out.println((i + 1) + " : " + frameDiffs.get(i));
				}
			}
			boundary.close();

			createWeightedShots();

			int[] flag = new int[16200];
			FileWriter weights = new FileWriter("./weights.txt");
			int count = 0;
			for(Map.Entry entry : weightedShots.entrySet())
			{
				if(count >= 2700) break;
				System.out.println("weights: " + entry.getKey());
				int[] tmp = weightedShots.get(entry.getKey());
				weights.write("shot: " + Arrays.toString(tmp) + "  weight: " + entry.getKey() + "\n");
				for(int i = tmp[0]; i < tmp[1]; i++, count++)
					flag[i] = 1;
			}
			weights.write("All the other shots will not be listed here (not included in the trailer either) due to low weights\n");
			weights.close();

			long start = System.currentTimeMillis();
			count = 0;
			for(int i = 0; i < 16200 && count < 2700; i++)
			{
				if(flag[i] == 1)
				{
					System.out.println("frame: " + i);
					lbIm1.setIcon(new ImageIcon(video[i]));
					frame.pack();
					frame.setVisible(true);

					long end = start + (count / 3) * 100 + (count % 3 == 1 ? 34 : (count % 3 == 2 ? 67 : 0));
					count++;
					while (System.currentTimeMillis() < end) {

					}
				}
			}

			
		} catch (Exception e) {
			// Throwing an exception
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public double getHueWeight(BufferedImage img) {
		Set<Integer> hueSet = new HashSet<>();
		for(int x = 0; x < width; x++)
			for(int y = 0; y < height; y++)
			{
				int RGB = img.getRGB(x, y);
				float[] hsbvals = new float[3];
				Color.RGBtoHSB((RGB >> 16) & 0xff, (RGB >> 8) & 0xff, RGB & 0xff, hsbvals);
				hueSet.add((int)((hsbvals[0] - (float)Math.floor(hsbvals[0])) * 360.0f));
			}

		return hueSet.size() / 359.0;
	}

	public void createWeightedShots() {
		int preIdx = 0;
		for(int endIdx : shotBoundaryIdx)
		{
			int count = 0;
			double shotWeight = 0;
			for(int i = preIdx; i < endIdx; i++)
			{
				shotWeight += frameAggregatedWeights[i];
				count++;
			}
			int[] tmp = new int[2];
			tmp[0] = preIdx;
			tmp[1] = endIdx;
			double weight = shotWeight / count;
			weightedShots.put(weight, tmp.clone());
			//System.out.println(Arrays.toString(weightedShots.get(weight)));
			preIdx = endIdx;
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
	 * 			the sum of motion vectors' magnitude of all blocks in this frame
	 */
	public int[] calcFrameDiffBasedOnBlocks(int currFrameIdx) {
		BufferedImage prevFrame = video[currFrameIdx - 1];
		BufferedImage currFrame = video[currFrameIdx];

		int frameDiff = 0;
		int frameVectorSum = 0;

		// Traverse every block in the curr frame
		// x1, y1: the top left coordinate of a block in the curr frame
		for (int y1 = 0; y1 + BLOCK_HEIGHT < height; y1 += BLOCK_HEIGHT) {
			for (int x1 = 0; x1 + BLOCK_WIDTH < width; x1 += BLOCK_WIDTH) {

				int minBlockDiff = Integer.MAX_VALUE;
				int minVectorMag = 0;
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
						if(blockDiff < minBlockDiff)
						{
							minBlockDiff = blockDiff;
							minVectorMag = (int)Math.sqrt(Math.pow(x1-x0, 2) + Math.pow(y1-y0, 2));
						}
					}
				}
				frameVectorSum += minVectorMag;
				frameDiff += minBlockDiff;
			}
		}
		int[] res = new int[2];
		res[0] = frameDiff;
		res[1] = frameVectorSum;
		// System.out.println(frameDiff);
		return res;
	}

	/**
	 * histograms:
	 * tables that contain (for each color within a frame) the number of pixels that are shaded in that color.
	 *
	 * Reference:
	 * https://www-nlpir.nist.gov/projects/tvpubs/tvpapers03/ramonlull.paper.pdf
	 *
	 * @param currFrameIdx the current frame index
	 * @return the absolute difference between currFrame and prevFrame
	 */
	public int calcFrameHistogramDiff(int currFrameIdx) {
		int frameDiff = 0;

		int[][] prevFrameHistograms = calcFrameRGBHistograms(currFrameIdx - 1);
		int[][] currFrameHistograms = calcFrameRGBHistograms(currFrameIdx);

//		int[] prevFrameHistograms = calcFrameHSVHistograms(currFrameIdx - 1);
//		int[] currFrameHistograms = calcFrameHSVHistograms(currFrameIdx);

		for (int component = 0; component < 3; component++) {
			for (int bin = 0; bin < 8; bin++) {
				frameDiff += Math.abs(currFrameHistograms[component][bin]
										- prevFrameHistograms[component][bin]);
			}
		}

//		for (int hue = 0; hue <= 360; hue++) {
//			frameDiff += Math.abs(currFrameHistograms[hue] - prevFrameHistograms[hue]);
//		}

		return frameDiff;
	}

	/**
	 * Considering space and also limited response of human visual system,
	 * quantization is done by eliminating four least significant bits of each channel.
	 */
	public int[][] calcFrameRGBHistograms(int frameIdx) {
		BufferedImage frame = video[frameIdx];
		int[][] histograms = new int[3][8];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int pixel = frame.getRGB(x, y);
				int quantizedR = (pixel & 0xf00000) >> 16;
				histograms[0][getBin(quantizedR)]++;

				int quantizedG = (pixel & 0xf000) >> 8;
				histograms[1][getBin(quantizedG)]++;

				int quantizedB = pixel & 0xf0;
				histograms[2][getBin(quantizedB)]++;
			}
		}
		return histograms;
	}

	public int getBin(int val) {
		if (val <= 16) {
			return 0;
		} else if (val <= 48) {
			return 1;
		} else if (val <= 80) {
			return 2;
		} else if (val <= 112) {
			return 3;
		} else if (val <= 144) {
			return 4;
		} else if (val <= 176) {
			return 5;
		} else if (val <= 208) {
			return 6;
		} else {
			return 7;
		}
	}

	public int[] calcFrameHSVHistograms(int frameIdx) {
		BufferedImage frame = video[frameIdx];
		int[] histograms = new int[360];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int pixel = frame.getRGB(x, y);
				float R = ((pixel & 0xff0000) >> 16) / 255.0f;
				float G = ((pixel & 0xff00) >> 8) / 255.0f;
				float B = (pixel & 0xff) / 255.0f;

				float[] hsv = RGBtoHSV(R, G, B);
				histograms[(int)(hsv[0])]++;
			}
		}
		return histograms;
	}

	float[] RGBtoHSV(float r, float g, float b) {
		float[] hsv = new float[3];
		// h, s, v = hue, saturation, value
		double cmax = Math.max(r, Math.max(g, b)); // maximum of r, g, b
		double cmin = Math.min(r, Math.min(g, b)); // minimum of r, g, b
		double diff = cmax - cmin; // diff of cmax and cmin.
		double h = -1, s = -1;

		// if cmax and cmax are equal then h = 0
		if (cmax == cmin) {
			h = 0;
		} else if (cmax == r) {
			// if cmax equal r then compute h
			h = (60 * ((g - b) / diff) + 360) % 360;
		} else if (cmax == g) {
			// if cmax equal g then compute
			h = (60 * ((b - r) / diff) + 120) % 360;
		} else if (cmax == b) {
			// if cmax equal b then compute h
			h = (60 * ((r - g) / diff) + 240) % 360;
		}

		// if cmax equal zero
		if (cmax == 0) {
			s = 0;
		} else {
			s = (diff / cmax) * 100;
		}

		// compute v
		double v = cmax * 100;
		hsv[0] = (float) h;
		hsv[1] = (float) s;
		hsv[2] = (float) v;

		return hsv;
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
				long end = start + (i / 3) * 100 + (i % 3 == 1 ? 34 : (i % 3 == 2 ? 67 : 0));
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
