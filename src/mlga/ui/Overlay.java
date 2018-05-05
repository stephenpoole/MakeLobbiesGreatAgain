package mlga.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import mlga.Boot;
import mlga.ScreenState;
import mlga.io.FileUtil;
import mlga.io.Settings;
import mlga.io.peer.PeerTracker;

public class Overlay extends JPanel {
	private static final long serialVersionUID = -470849574354121503L;

	private boolean frameMove = false;
	private boolean mouseEntered = false;
	public ScreenState gameState;
	private int noteEdit = -1;

	private CopyOnWriteArrayList<Peer> peers = new CopyOnWriteArrayList<Peer>();
	private Font roboto;
	private BufferedImage iconHeart;
	private BufferedImage iconBan;
	private BufferedImage iconHeartHovered;
	private BufferedImage iconBanHovered;

	/** idx & fh are updated by listener and rendering events. <br>They track hovered index and font height.*/
	private int idx = -1, fh = 0;
	
	private final PeerTracker peerTracker;
	
	private final JWindow frame;
	
	public Overlay() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, FontFormatException, IOException{
		this.gameState = ScreenState.NONE;
		peerTracker = new PeerTracker();
		peerTracker.start();
		
		InputStream is = FileUtil.localResource("Roboto-Medium.ttf");
		roboto = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(15f);
		is.close();
		
		iconHeart = ImageIO.read(FileUtil.localResource("heart.png"));
		iconBan = ImageIO.read(FileUtil.localResource("ban.png"));
		iconHeartHovered = ImageIO.read(FileUtil.localResource("heart-hovered.png"));
		iconBanHovered = ImageIO.read(FileUtil.localResource("ban-hovered.png"));

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		this.setOpaque(false);
		
		frame = new JWindow();
		frame.setBackground(new Color(0, 0, 0, 0));
		frame.setFocusableWindowState(false);

		frame.add(this);
		frame.setAlwaysOnTop(true);
		Overlay self = this;
		frame.addMouseListener(new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(SwingUtilities.isRightMouseButton(e)){
				} else {
					if (e.getClickCount() >= 2){
						frameMove = !frameMove;
						Settings.set("frame_x", frame.getLocationOnScreen().x);
						Settings.set("frame_y", frame.getLocationOnScreen().y);
					} else {
						if(idx < 0 || idx >= peers.size() || peers.isEmpty() || e.getX() < 0 || e.getY() < 0)
							return;

						Peer p = peers.get(idx);
						if(!p.saved()){
							p.rate(true);
						}else if(p.blocked()){
							p.rate(false);
						}else{
							p.unsave();
						}
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				mouseEntered = true;
			}

			@Override
			public void mouseExited(MouseEvent e) {
				idx = -1;
				mouseEntered = false;
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}
		});
		frame.addMouseMotionListener(new MouseMotionListener(){
			@Override
			public void mouseDragged(MouseEvent e) {
				if(frameMove)
					frame.setLocation(e.getXOnScreen() - (getPreferredSize().width / 2), e.getYOnScreen() - 6);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				idx = Math.min(peers.size() - 1, (int) Math.floor(e.getY() / (fh)));
			}

		});

		frame.pack();
		frame.setLocation((int)Settings.getDouble("frame_x", 5), (int)Settings.getDouble("frame_y", 400));
		frame.setVisible(true);

		Timer cleanTime = new Timer();
		cleanTime.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run(){
				peers.stream().filter(p -> p.age() >= 1000).forEach(p ->{
					Boot.active.remove(p.getID().hashCode());
					peers.remove(p);
				});
			}
		}, 0, 2500);

		Thread t = new Thread("UIPainter"){
			public void run() {
				try{
					while(true){
						frame.toFront(); //Fix for window sometime hiding behind others
						if(!frameMove){
							Thread.sleep(400);
						}else{
							Thread.sleep(10);
						}
						Overlay.this.repaint();
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	private void addPeer(Inet4Address addr, long rtt){
		peers.add(new Peer(addr, rtt, peerTracker.getPeer(addr)));
	}

	/** Sets a peer's ping, or creates their object. */
	public void setPing(Inet4Address id, long ping){
		Peer p = this.getPeer(id);
		if(p != null){
			p.setPing(ping);
		}else{
			this.addPeer(id, ping);
		}
	}

	public int numPeers(){
		return peers.size();
	}

	public void clearPeers(){
		peers.clear();
	}

	public void removePeer(Inet4Address i){
		Peer p = this.getPeer(i);
		if(p != null)
			peers.remove(p);
	}

	/** Finds a Peer connection by its ID. */
	private Peer getPeer(Inet4Address id){
		return peers.stream().filter(p -> p.getID().equals(id)).findFirst().orElse(null);
	}

	/** Dispose this Overlay's Window. */
	public void close(){
		this.frame.dispose();
	}
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(150, 100);
	}

	@Override
	protected void paintComponent(Graphics gr) {
		super.paintComponent(gr);
		Graphics2D gi = (Graphics2D) gr.create();
		Graphics2D g = (Graphics2D) gr.create();
		
		if (gameState != ScreenState.NONE) {
			drawInit(g, gi);
		}

		if (gameState == ScreenState.LOBBY || (gameState == ScreenState.INGAME && mouseEntered)) {
			drawPeers(g, gi);
		} else if (gameState == ScreenState.INGAME) {
			drawHandle(g);
		}

		g.dispose();
		gi.dispose();
	}
	
	public void drawInit(Graphics2D g, Graphics2D gi) {
		gi.setColor(getBackground());
		g.setColor(getBackground());
		g.setFont(roboto);
        g.setColor(new Color(0,0,0,0));
		g.fillRect(0, 0, getWidth(), getHeight());

		if(!frameMove){
			g.setColor(new Color(0f,0f,0f,.5f));
			gi.setColor(new Color(0f,0f,0f,.5f));
		}else{
			g.setColor(new Color(0f,0f,0f,1f));
			gi.setColor(new Color(0f,0f,0f,1f));
		}
		
		fh = g.getFontMetrics().getAscent();//line height. Can use getHeight() for more padding between.
	}
	
	public void drawHandle(Graphics2D g) {
		g.fillRect(0, 0, fh, fh );
		
		int xP[] = {2, 12, 2};
		int yP[] = {2, 2, 12};
		
		GeneralPath line = new GeneralPath(GeneralPath.WIND_EVEN_ODD, xP.length);
		line.moveTo(xP[0], yP[0]);
		
		for (int index = 1; index < xP.length; index++) {
			line.lineTo(xP[index], yP[index]);
		};
		line.closePath();
		g.fill(line);
	}
	
	public void drawPeers(Graphics2D g, Graphics2D gi) {
		g.fillRect(0, 0, getPreferredSize().width, fh*Math.max(1, peers.size())+2 );
		
		if(!peers.isEmpty()){
			short i = 0;
			for(Peer p : peers){
				if(idx == i){
					g.setColor(new Color(0f,0f,0f));
					g.fillRect(1, fh*i+1, getPreferredSize().width, fh+1);//Pronounce hovered Peer.
				}
				long rtt = p.getPing();
				
				if (noteEdit == i) {
					g.setColor(Color.CYAN);
				} else {
					if(rtt <= 140){
						g.setColor(Color.GREEN);
					}else if(rtt > 140 && rtt <= 190){
						g.setColor(Color.YELLOW);
					}else{
						g.setColor(Color.RED);
					}
				}

				String render = p.getID().getHostAddress();
				if(p.saved())
					if (p.blocked()) {
						if (idx == i) {
							gi.drawImage(iconBanHovered, 5, fh*i + 3, null);
						} else {
							gi.drawImage(iconBan, 5, fh*i + 3, null);
						}
					} else {
						if (idx == i) {
							gi.drawImage(iconHeartHovered, 5, fh*i + 4, null);
						} else {
							gi.drawImage(iconHeart, 5, fh*i + 4, null);
						}
					}
				
				if(p.hasNote() || noteEdit == i) {
					render = "";
					if (noteEdit == i)
						render = "(";
					
					render += p.getNote();
					if (noteEdit == i)
						render += ")";
				}
				
				g.drawString(render, 19, fh*(i+1));
				++i;
			}
		}else{
			g.setColor(Color.RED);
			g.drawString("No Players", 3, fh);
		}
	}
}
