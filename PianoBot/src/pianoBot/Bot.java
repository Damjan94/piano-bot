package pianoBot;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.sikuli.script.Match;
import org.sikuli.script.Pattern;
import org.sikuli.script.Screen;


public class Bot {
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
	private PriorityBlockingQueue<Data> s;
	Screen screen;
	Pattern menu;
	Pattern arcade;
	Pattern relay;
	
	
	public static void main(String[] args) throws InterruptedException{
		
		
		System.out.println("new Stage");
		Bot b = new Bot();
		//Thread.sleep(5000);
		b.initialise();
		Thread.sleep(50);
		b.startThreads();
		Thread.sleep(50);
		b.play();
		
		b.killThreads();
	}
	
	Bot(){
		piano_black = new Color(17, 17, 17);
		piano_gray = new Color(77, 77, 77);
		lost = new Color(251, 62, 56);
		s = new PriorityBlockingQueue<>();
		
		clicker = new Thread(){
			
				public void run() {
					Data d = null;
					while(running){
						for(WhiteChecker thread : threads){
							
							d = thread.s.poll();
							
							if(d!=null){//if instead of while originaly
								s.add(d);
								//d = thread.s.poll();
								//System.out.println("found cickable"+thread.getName());
							}
						}
						
						d = s.poll();
						while(d!=null){//if instead of while originaly
							bot.keyPress(d.keyPress);
							d = s.poll();
							//score++;
							//System.out.println("clicking "+ d.keyPress);
						}
						/*try {
							Thread.sleep(70);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}*/
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
		try {
			menu = new Pattern(ImageIO.read(new File("res"+File.separator+"main_menu.PNG")));
			arcade = new Pattern(ImageIO.read(new File("res"+File.separator+"arcade.PNG")));
			relay = new Pattern(ImageIO.read(new File("res"+File.separator+"relay.PNG")));
		} catch (IOException e) {
			e.printStackTrace();
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
		Thread.sleep(50);
		while(running){
			

			BufferedImage scr = bot.createScreenCapture(rec);
			for(WhiteChecker t : threads){
				
				t.setImage(scr);
				
			}
			takeScreen("pic",scr);
			clicks++;
			Thread.sleep(SLEEP);
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
	
	private class WhiteChecker extends Thread{
		int offsetY;
		int offsetX;
		int keyToPress;
		Color piano_black;
		Color c;
		private PriorityBlockingQueue<Data> s;
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
		WhiteChecker(int offsetX, int keyToPress, Color c, Rectangle rec){
			this.offsetX = offsetX;
			this.offsetY = 0;
			this.keyToPress = keyToPress;
			this.rec = rec;
			clickedTime = System.currentTimeMillis();
			foundClickable = false;
			changeImg = false;
			s = new PriorityBlockingQueue<>();
			piano_black = c;
			bfi = null;
			temp = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
			System.out.println("created thread " + getName());
		}
		
		
		public void run(){
			while(running){
				synchronized (tempBool) {
					changeImg = tempBool;
				}
				if(changeImg){
					synchronized (temp) {
						bfi = temp;//deepCopy(temp);
					}
					bfi.getHeight();
					//temp = null;
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
				//d = bfi.getGraphics();
				forcedOnce = false;
				force = false;
				while((force&&!forcedOnce) || offsetY>(rec.getHeight()-(PIANO_TILE_SIZE*1.5))){// used to be offsetY>(rec.getHeight()-(PIANO_TILE_SIZE*1.5)
					//TODO this does not account for the case if the tile is cut
					force = false;
					c = new Color(bfi.getRGB(offsetX,offsetY));
					if(c.equals(lost)){
						running = false;
						break;
					}
					if(c.equals(piano_gray)){

							offsetY-=PIANO_TILE_SIZE;
							force = true;
							forcedOnce = true;
							continue;
					}
					if(c.equals(piano_black)){
						if(new Color(bfi.getRGB(offsetX, offsetY-(PIANO_TILE_SIZE/2))).equals(piano_gray)){
							offsetY-=PIANO_TILE_SIZE;
							continue;
						}else{
							if(new Color(bfi.getRGB(offsetX, offsetY+1)).equals(piano_black) 
									|| new Color(bfi.getRGB(offsetX, offsetY-1)).equals(piano_black)){
								foundClickable = true;
								break;
							}
						}
					}
					offsetY--;
				}
				//d.setColor(Color.CYAN);
				//d.drawLine(offsetX, rec.height-1, offsetX, offsetY);
				//d.dispose();
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
		
		private BufferedImage deepCopy(BufferedImage bi) {
			 ColorModel cm = bi.getColorModel();
			 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
			 WritableRaster raster = bi.copyData(null);
			 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
			}

		
		protected synchronized void setImage(BufferedImage img){
			long timeNow = System.currentTimeMillis();
			synchronized (clickedTime) {
				//System.out.println(timeNow - clickedTime);
				if(timeNow - clickedTime < SLEEP*2) return;
			}
			synchronized (this.temp) {
				this.temp = img;
			}
			synchronized (tempBool) {
				tempBool = true;
			}
			//System.out.println("notifying " + getName());
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
	        if(yPosition>d.yPosition) return -1; //reversed
	        
	        if(yPosition<d.yPosition) return 1; //reversed
	        
	        return 0;
	    }
	}
}
