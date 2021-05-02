import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class VideoPlayer {
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("usage: java VideoPlayer [VideoFileDirectory(.rgb)] [AudioFile]");
			return;
		}
//		String audiofilename = args[1];
//		// opens the inputStream
//		FileInputStream inputStream;
//		try {
//			inputStream = new FileInputStream(audiofilename);
//			// inputStream = this.getClass().getResourceAsStream(filename);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//			return;
//		}
//		// initializes the playSound Object
//		PlaySound playSound = new PlaySound(inputStream);
//		// plays the sound
//		try {
//			playSound.play();
//		} catch (PlayWaveException e) {
//			e.printStackTrace();
//			return;
//		}

		ImageDisplay ren = new ImageDisplay();
		ren.initialize(args[0]);
		ren.detectShots();

		//ren.start();
		//playSound.start();

	}
}
