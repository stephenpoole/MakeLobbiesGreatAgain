package mlga;

import java.awt.Color;

public class Pixel {
	public Color color;
	public int x;
	public int y;
	
	public Pixel(int _x, int _y, Color _color) {
		this.color = _color;
		this.x = _x;
		this.y = _y;
	}
	
	public Boolean equals(Pixel pixel) {
		return pixel.color.equals(this.color);
	}
}
