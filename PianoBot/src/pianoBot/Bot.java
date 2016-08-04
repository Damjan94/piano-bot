package pianoBot;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;

import org.sikuli.script.Match;
import org.sikuli.script.Pattern;
import org.sikuli.script.Screen;

/**
 * This program tries to play the game piano tiles on facebook
 * Currently it only works on 1680x1050 resolution
 * It uses external library called sikuliX found on {@link http://sikulix.com/}
 * 
 * @author damjan
 * @version 1.0
 * 
 * 
 * class bot is what main function instantiates
 */
public class Bot {
	protected static final boolean DEBUG = false;
	private static Rectangle rec;
	private Color piano_black;
	private Color piano_gray;
	private Color lost;
	private boolean running;
	private Robot bot;
	private WhiteChecker[] threads;
	private Thread clicker;
	final long SLEEP = 40;
	final int PIANO_TILE_SIZE = 149; //in pixels
	int clicks = 0;
	int score = 0;
	int scoreOld = 0;
	int frame = 0;
	private LinkedList<Data> s;
	Screen screen;
	Pattern menu;
	Pattern arcade;
	Pattern relay;
	
	
	public static void main(String[] args) throws InterruptedException{
		
		Bot b = new Bot();
		b.initialise();
		Thread.sleep(500);
		b.startThreads();
		b.play();
		
		b.killThreads();
	}
	
	Bot(){
		//color of a black piano tile
		piano_black = new Color(17, 17, 17);
		//color of a gray piano tile
		piano_gray = new Color(77, 77, 77);
		//color of a red color when you click on wrong tile
		lost = new Color(251, 62, 56);
		s = new LinkedList<>();
		/**
		 * a thread that polls WhiteChecker threads for button to push
		 */
		clicker = new Thread(){
			boolean run;
			int x = 0;
				public void run() {
					Data d = null;
					while(running){
						for(int i = 0; i < 4; i++){
							synchronized (threads[i].tempBool) {
								run = threads[i].tempBool;
							}
							if(run){
								try {
									Thread.sleep(SLEEP/10);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								i = 0;
								if(DEBUG){
									System.out.println("sleeping");
								}
							}
						}
						
						for(WhiteChecker thread : threads){
							
							d = thread.s.poll();
							while(d!=null){
								s.add(new Data(d.keyPress, d.yPosition));
								d = thread.s.poll();
							}
						}
						s.sort(null);
						if(DEBUG){
							for(Data p : s){
								System.out.println(p.yPosition);
							}
							System.out.println("______________SEPERATOR______________");
						}
						
						d = s.poll();
						while(d!=null){
							bot.keyPress(d.keyPress);
							if(DEBUG){
								System.out.print("clicking on ");
								switch(d.keyPress){
									case 65 :
										x = 80;
										System.out.print("A");
										break;
									case 83 :
										x = 160;
										System.out.print("S");
										break;
									case 68 :
										x = 240;
										System.out.print("D");
										break;
									case 70 :
										x = 320;
										System.out.print("F");
										break;
								}
								bot.mouseMove(rec.x+x, rec.y+d.yPosition);
								System.out.println(" on frame "+clicks);
								
							}
								
							d = s.poll();
						}
					}
				}
		};
		
		threads = new WhiteChecker[4];
		
		try {
			bot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
		if(!new File(System.getProperty("user.home")+File.separator+"pic").exists()){
			new File(System.getProperty("user.home")+File.separator+"pic").mkdir();
		}
		//TODO let user specify these images
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = (int)screenSize.getWidth();
		int height = (int)screenSize.getHeight();
		String folder = width+"x"+height;
		try {
			menu = new Pattern(ImageIO.read(new File("res"+File.separator+folder+File.separator+"main_menu.PNG")));
			arcade = new Pattern(ImageIO.read(new File("res"+File.separator+folder+File.separator+"arcade.PNG")));
			relay = new Pattern(ImageIO.read(new File("res"+File.separator+folder+File.separator+"relay.PNG")));
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("unsuported resolution "+width+" x "+height);
			System.exit(1);
		}
	}
	
	private void initialise() throws InterruptedException{
		do{
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//TODO let user specify area of rec
			screen = new Screen();
			menu.similar((float) 0.95);
			Match m = screen.exists(menu);
			Match action;
				if(m!=null){
					action = screen.exists(arcade);
					rec = m.getRect();
					BufferedImage bm = bot.createScreenCapture(rec);
					int y = 0;
					int x = 0;
					while(!new Color(bm.getRGB(50, y)).equals(Color.WHITE)){
						y++;
					}
					while(!new Color(bm.getRGB(x, y)).equals(Color.BLACK)){
						x++;
					}
					rec.x = rec.x+x;
					rec.y = rec.y+y;
					rec.height-=y;
					rec.width-=x;
					bm = bot.createScreenCapture(rec);
					int offset;
					offset = rec.height-1;
					while(!new Color(bm.getRGB(10, offset)).equals(Color.BLACK)){
						offset--;
					}
					rec.height = offset+1;
					offset = rec.width-1;
					while(!new Color(bm.getRGB(offset, 10)).equals(Color.BLACK)){
						offset--;
					}
					rec.width = offset+1;
					
					bm = bot.createScreenCapture(rec);
					if(action!=null){
						action.click();
						break;
					}
					
				}
			System.out.println("not found piano main manu. try again in 2 sec? true/false");
		}while(new Scanner(System.in).nextBoolean());
		
		if(rec == null){
			System.out.println("couldn't find piano main manu match. exiting");
			System.exit(1);
		}
		
		threads[0] = new WhiteChecker((rec.width/8)*1, KeyEvent.VK_A, piano_black, rec); //65
		threads[1] = new WhiteChecker((rec.width/8)*3, KeyEvent.VK_S, piano_black, rec); //83
		threads[2] = new WhiteChecker((rec.width/8)*5, KeyEvent.VK_D, piano_black, rec); //68
		threads[3] = new WhiteChecker((rec.width/8)*7, KeyEvent.VK_F, piano_black, rec); //70
	}
	private void startThreads(){
		if(running) return;
		running = true;
		for(Thread t : threads){
			if(!t.isAlive()){
				if(!new File(System.getProperty("user.home")+File.separator+t.getName()).exists()){
					new File(System.getProperty("user.home")+File.separator+t.getName()).mkdir();
				}
				t.setDaemon(true);
				t.start();
			}
		}
		if(!clicker.isAlive()){
			clicker.setDaemon(true);
			clicker.start();
		}
	}
	private void killThreads() throws InterruptedException{
		if(!running) return;
		running = false;
		Thread.sleep(100);
		for(Thread t : threads){
			if(t.isAlive()){
				t.join();
				System.out.println("Killed "+ t.getName());
			}
		}
		if(clicker.isAlive()){
			clicker.join();
		}
	}
	
	private void play() throws InterruptedException{
		LinkedList<BufferedImage> bi = new LinkedList<>();
		BufferedImage scr;
		while(running){
			

			scr = bot.createScreenCapture(rec);
			for(WhiteChecker t : threads){
				
				t.setImage(scr);
				
			}
			if(DEBUG){
				bi.add(scr);
			}
			clicks++;
			Thread.sleep(SLEEP);
		}
		if(DEBUG){
			int size = bi.size();
			while (size > 0){
				scr = bi.remove();
				if(size>15 && size < 50){
					takeScreen("pic",scr);
				}
				size--;
			}
		}
	}
	
	
	
	private void takeScreen(String name, BufferedImage scr){
		int b = 0;
		File f = new File(System.getProperty("user.home")+File.separator+name+File.separator+"img"+b+".png");
		while(f.exists()){
			b++;
			f = new File(System.getProperty("user.home")+File.separator+name+File.separator+"img"+b+".png");
		}
		try {
			ImageIO.write(scr, "PNG", f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * WhiteChecker checks for tiles to press and adds them to concurrent linked queue
	 *
	 */
	private class WhiteChecker extends Thread{
		int offsetY;
		int offsetX;
		int keyToPress;
		int tempy1;
		Color piano_black;
		Color c;
		private ConcurrentLinkedQueue<Data> s;
		boolean foundClickable;
		boolean changeImg = false;
		boolean force;
		boolean forcedOnce;
		Boolean tempBool = false;
		BufferedImage bfi;
		BufferedImage temp;
		Long clickedTime = null;
		Graphics d;
		Rectangle rec;
		/**
		 * 
		 * @param offsetX specifies what row should this instance of WhiteChecker check for black tile(it should be in the middle of said row)
		 * @param keyToPress what key does this instance of WhiteChecker need to press
		 * @param c used to initialise local piano black variable, and later its used to check if current pixel is equal to piano black 
		 * @param rec is the bounds on the screen  of piano tiles game
		 */
		WhiteChecker(int offsetX, int keyToPress, Color c, Rectangle rec){
			this.offsetX = offsetX;
			this.offsetY = 0;
			this.keyToPress = keyToPress;
			this.rec = rec;
			clickedTime = System.currentTimeMillis();
			foundClickable = false;
			changeImg = false;
			s = new ConcurrentLinkedQueue<>();
			piano_black = c;
			bfi = null;
			temp = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
			System.out.println("created thread " + getName());
		}
		
		
		public void run(){
			//wait a bit before start
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			//TODO set running to false if the game is lost by letting a tile fall through
			while(running){
				
				// check to see if image has changed
				// so we dont process the sam image twice
				synchronized (tempBool) {
					changeImg = tempBool;
				}
				if(changeImg){
					synchronized (temp) {
						bfi = temp;
					}
					synchronized (tempBool) {
						tempBool = false;
					}
				}else{
					try {
						fun();
						continue;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				foundClickable = false;
				offsetY = rec.height-1;
				if(DEBUG){
					d = bfi.getGraphics();
				}
				forcedOnce = false;
				force = false;
				//check only for 1.5 tile length from the bottom of an image for clickable tile
				while((force&&!forcedOnce) || offsetY>(rec.getHeight()-(PIANO_TILE_SIZE*2))){
					force = false;
					tempy1 = offsetY;
					c = new Color(bfi.getRGB(offsetX,offsetY));
					if(c.equals(lost)){
						running = false;
						break;
					}
					//if we found a gray tile
					if(c.equals(piano_gray)){
						//and if we are above 1 tile height
						if(offsetY<rec.height-PIANO_TILE_SIZE){
							//skip this tile
							offsetY-=PIANO_TILE_SIZE;
							force = true;
							forcedOnce = true;
							continue;
						}else{
							//if we are below 1 tile height
							//go up pixel by pixel
							//until the color is not gray
							while(c.equals(piano_gray)){
								offsetY--;
								c = new Color(bfi.getRGB(offsetX,offsetY));
							}
							//go up one pixel (there is a pixel between black and gray that is of nether color)
							offsetY--;
							c = new Color(bfi.getRGB(offsetX,offsetY));
							//while its black go up by one pixel
							while(c.equals(piano_black)){
								c = new Color(bfi.getRGB(offsetX,offsetY));
								offsetY--;
							}
							offsetY--;
							
							
						}
					}
					c = new Color(bfi.getRGB(offsetX,offsetY));
					//ignore pixels below 1/3 of piano tile height (so we don't click on previously clicked
					//tile whose gray area is below the screen)
					if(offsetY<rec.height-(PIANO_TILE_SIZE/3) && c.equals(piano_black)){
						//sometimes, if no black tiles are touching a seperator line, it is the same color as piano_black
						//we continue to the next pixel if this is the case
						if(!new Color(bfi.getRGB(offsetX, tempy1-1)).equals(piano_black) && !new Color(bfi.getRGB(offsetX, tempy1+1)).equals(piano_black)){
							offsetY--;
							continue;
						}
						//if the pixel is higher than piano tile size 
						if(offsetY<rec.height-PIANO_TILE_SIZE){
							//we can check its middle to see if it has been clicked
							if(new Color(bfi.getRGB(offsetX, offsetY-(PIANO_TILE_SIZE/2))).equals(piano_gray)){
								offsetY-=PIANO_TILE_SIZE;
								continue;
							}else{
								//if it has not been clicked we set it to clickable and break
									foundClickable = true;
									break;
							}
						}else{
							//if its below  piano tile size
							while(c.equals(piano_black)){
								//we continue till it is black
								offsetY--;
								c = new Color(bfi.getRGB(offsetX,offsetY));
							}
							//this is for a pixel betwin gray and black color
							offsetY--;
							//if its not gray, that means its not been clicked, so we set it to be clicked and break
							if(!new Color(bfi.getRGB(offsetX,offsetY)).equals(piano_gray)){
								foundClickable = true;
								break;
							}else{
								//if its gray, we go up one pixel and continue
								offsetY--;
								continue;
							}
						}
					}
					offsetY--;
				}
				if(DEBUG){
					d.setColor(Color.CYAN);
					d.drawLine(offsetX, rec.height-1, offsetX, offsetY);
					d.dispose();
				}
				if(foundClickable){
					s.add(new Data(keyToPress, offsetY));
					//takeScreen(getName(),bfi);
					bfi = null;
					
					
				}
				synchronized (clickedTime) {
					clickedTime = System.currentTimeMillis();
				}
				
			}
			
		}
	
		
		private void takeScreen(String name, BufferedImage scr){
			int b = 0;
			File f = new File(System.getProperty("user.home")+File.separator+name+File.separator+"img"+b+".png");
			while(f.exists()){
				b++;
				f = new File(System.getProperty("user.home")+File.separator+name+File.separator+"img"+b+".png");
			}
			try {
				ImageIO.write(scr, "PNG", f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * 
		 * sets the image and wakes the thread
		 * if it has been sleaping for SLEEP*2
		 * 
		 * @param img the img to be checked next
		 */
		protected synchronized void setImage(BufferedImage img){
			long timeNow = System.currentTimeMillis();
			synchronized (clickedTime) {
				if(timeNow - clickedTime < SLEEP*2) return;
			}
			synchronized (this.temp) {
				this.temp = img;
			}
			synchronized (tempBool) {
				tempBool = true;
			}
			if(DEBUG){
				System.out.println("notifying " + getName()+" after sleeping for "+ (timeNow - clickedTime));
			}
			this.notify();
		}
		protected synchronized void fun() throws InterruptedException{
			wait();
		}
		
	}
	
	private class Data implements Comparable<Data>{
		int keyPress;
		int yPosition;
		
		Data(int keyPress, int yPosition){
			this.keyPress = keyPress;
			this.yPosition = yPosition;
		}
		
		public int compareTo(Data d)
	    {
	        if(yPosition> d.yPosition) return -1; //reversed
	        
	        if(yPosition< d.yPosition) return 1; //reversed
	        
	        return 0;
	    }
	}
	}
