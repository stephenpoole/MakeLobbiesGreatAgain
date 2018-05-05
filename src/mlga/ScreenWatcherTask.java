package mlga;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.TimerTask;

class ScreenWatcherTask extends TimerTask{
	private Consumer<ScreenState> callback;
	
	
	public ScreenWatcherTask(Consumer<ScreenState> callback) {
		this.callback = callback;
	}
	
	@Override
	public void run() {
		Robot robot;
		try {
			robot = new Robot();
			Rectangle area = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		    BufferedImage capture = robot.createScreenCapture(area);
		    
		    // sampled from the strikethrough representing whether or not the killer is ready
		    Screen lobbyScreenUnreadied = new Screen(ScreenState.LOBBY);
		    lobbyScreenUnreadied.addPixel(1763, 930, new Color(93, 90, 85));
		    lobbyScreenUnreadied.addPixel(1792, 938, new Color(93, 90, 85));
		    
		    Screen lobbyScreenReadied = new Screen(ScreenState.LOBBY);
		    lobbyScreenReadied.addPixel(1766, 931, new Color(240, 0, 0));
		    lobbyScreenReadied.addPixel(1794, 938, new Color(240, 0, 0));
		    
		    if (lobbyScreenUnreadied.match(capture) || lobbyScreenReadied.match(capture)) {
		    	this.callback.call(ScreenState.LOBBY);
		    	return;
		    }
		    
		    Screen inGameScreen = new Screen(ScreenState.INGAME);
		    //inGameScreen.addPixel();
		    if (inGameScreen.match(capture)) {
		    	this.callback.call(ScreenState.INGAME);
		    	return;
		    }
		    
		    this.callback.call(ScreenState.NONE);
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}
	
	class Screen{
		public ArrayList<Pixel> pixels;
		public ScreenState name;
		private int refWidth = 1920;
		private int refHeight = 1080;
		
		public Screen(ScreenState name, ArrayList<Pixel> pixels) {
			this.name = name;
			this.pixels = pixels;
		}
		
		public Screen(ScreenState name) {
			this.name = name;
			this.pixels = new ArrayList<Pixel>();
		}
		
		public boolean match(BufferedImage target) {
			for (Pixel pixel : this.pixels) {
		    	if (this.sampleVicinity(target, pixel, 1)) {
		    		return true;
		    	}
		    }
			return false;
		}
		
		public void addPixel(int x, int y, Color color) {
			this.pixels.add(new Pixel(x, y, color));
		}
		
		private boolean samplePixel(BufferedImage capture, Pixel pixel) {
			int width = capture.getWidth();
			int height = capture.getHeight();
			int x = (int)((pixel.x / (double)refWidth) * width);
			int y = (int)((pixel.y / (double)refHeight) * height);
			Color sample = new Color(capture.getRGB(x, y));
			return sample.equals(pixel.color);
		}
		
		private boolean sampleVicinity(BufferedImage capture, Pixel pixel, int radius) {
			if (this.samplePixel(capture, pixel)) {
				return true;
			}
			for (int i = pixel.x - radius; i <= pixel.x + radius; i++) {
				for (int j = pixel.y - radius; j <= pixel.y + radius; j++) {
					if (this.samplePixel(capture, new Pixel(i, j, pixel.color))) {
						return true;
					}
				}
			}
			return false;
		}
	}
}
