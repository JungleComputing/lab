package dach.gui;

import ibis.smartsockets.viz.UniqueColor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;

import dach.gui.NodeStatistics.Event;

public class WorkMap extends JComponent 
implements MouseInputListener, MouseWheelListener {

	// Generated
	private static final long serialVersionUID = 1L;

	private static final boolean SITES_TRANSPARENT = true;

	private static final int BLINK_TIME = 10 * 1000;

	private static final int GRID_CIRCLE_SIZE = 30;
	private static final int DOT_CIRCLE_SIZE = 6;

	private static final int MAX_SCALE  = 300;
	private static final int SCALE_STEP = 10;
	private static       int MIN_SCALE  = 10; // This one depends on image size!   

	private static final BasicStroke stroke = new BasicStroke(1.5f);
	private static final BasicStroke thinStroke = new BasicStroke(0.75f);

	private UniqueColor colorGenerator;

	private BufferedImage map;

	private int imageW; 
	private int imageH;

	private int posX = 4200;
	private int posY = 950;

	private int preferredSizeW = 0;
	private int preferredSizeH = 0;

	private int scaleW = 0;
	private int scaleH = 0;

	private int mouseLocationX = -1;
	private int mouseLocationY = -1;

	private double scale = 100;

	private boolean old;

	public WorkMap(boolean old, LinkedList<ClusterStatistics> stats, long appEndTime) {

		this.old = old;

		int totalLines = 0;
		
		for (ClusterStatistics c : stats) { 
			totalLines += c.cores * c.nodes.size() + c.nodes.size() * 4;
		}
		
		totalLines += stats.size() * 4;
		
		long endTime = 0;
		long jobEndTime = 0;

		for (ClusterStatistics c : stats) { 
			totalLines += c.getNodes();

			jobEndTime = Math.max(jobEndTime, c.getLatestJobEndTime());
			endTime = Math.max(endTime, c.getLatestEndTime());
		}

		imageW = (int) Math.max(jobEndTime, endTime) + 100;
		imageH = totalLines;

		//scaleW = scale(imageW, 1200);
		//scaleH = scale(imageH, 800);

		scaleW = 1;
		scaleH = 1;

		preferredSizeW = imageW / scaleW;
		preferredSizeH = imageH / scaleH;

		System.out.println("Image size " + imageW + "x" + imageH);

		BufferedImage bi2 = new BufferedImage(imageW, imageH, BufferedImage.TYPE_INT_RGB);
		Graphics big = bi2.getGraphics();
//		big.drawImage(map, 0, 0, null);

		big.setColor(Color.BLACK);
		big.fillRect(0, 0, imageW, imageH);

		int line = 4;

		for (ClusterStatistics c : stats) { 

			if (old) { 
				line += drawClusterOld(big, c, line);
				line += 4;
			} else { 
				line += drawCluster(big, c, line);
				line += 4;
			}

		}

		big.setColor(Color.RED);
		big.fillRect((int)appEndTime-scaleW, 0, 2*scaleW, imageH);

		System.out.println("FillRect " + appEndTime + " 0 " + scaleW + " " + imageH);
		 
		try {
			ImageIO.write(bi2, "png", new File("out.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		System.exit(1);
		
		map = bi2;

		imageW = map.getWidth(null);
		imageH = map.getHeight(null);

		int minScaleX = (100 * preferredSizeW) / imageW;
		int minScaleY = (100 * preferredSizeH) / imageH;

		if (minScaleX < minScaleY) { 
			MIN_SCALE = minScaleX;
		} else { 
			MIN_SCALE = minScaleY;
		}

		if (MIN_SCALE <= 0) { 
			MIN_SCALE = 1;
		}

		addMouseMotionListener(this);
		addMouseListener(this);
		addMouseWheelListener(this);

		colorGenerator = new UniqueColor();
	}

	private int scale(int orig, int max) { 

		if (orig < max) { 
			return 1;
		}

		int tmp = orig / max;

		if (orig % max != 0) { 
			tmp++;
		}

		return tmp;
	}

	private int drawNodeMC(Graphics big, int cores, NodeStatistics n, final int line) { 
		
		int myLine = line;
		
		int start = (int) n.getEarliestStartTime();
		int end = (int) n.getLatestEndTime();

		big.setColor(Color.DARK_GRAY);

		big.fillRect(0, myLine, imageW, cores);

		big.setColor(Color.GRAY);
		big.fillRect(start, myLine, (end-start), cores);
		
		for (NodeStatistics.Event e : n.events) { 

			switch (e.type) { 
			case STEAL_REQUEST:
			case STEAL_REPLY_EMPTY:			
			case STEAL_REPLY_DONE:			
			case STEAL_REPLY_FULL:
			case START_COMPUTE:
				break;
			case JOB_RESULT:
			
				long [] data = (long[]) e.data;
				
				int jobTime = (int) Math.round(data[0] / 1000.0);
				int computeTime = (int) Math.round(data[4] / 1000.0);
				int transferTime = (int) Math.round(data[1] / 1000.0);
				
				int startT = (int) (e.time - jobTime);
				int computeT = (int) (e.time - computeTime);
				
				big.setColor(Color.RED);
				big.fillRect(startT, myLine, transferTime, cores);
				
				big.setColor(Color.YELLOW);
				big.fillRect(computeT, myLine, computeTime, cores);
	
				System.out.println("t" + e.ID + "(x) = " + startT + "<=x && x<=" 
						+ computeT + " ? " + myLine + " : 1/0");
			
				System.out.println("c" + e.ID + "(x) = " + computeT + "<=x && x<=" 
						+ (e.time) + " ? " + myLine + " : 1/0");
				
				break;
			}
		}
		
		myLine += cores;
		
		return (myLine - line);	
	}
	
	private static class TimeSorter implements Comparator<NodeStatistics.Event> {

		private long getStartTime(Event e) { 
			long [] data = (long[]) e.data;
			int jobTime = (int) Math.round(data[0] / 1000.0);
			return (e.time - jobTime);
		}
		
		public int compare(Event arg0, Event arg1) {
			
			long t0 = getStartTime(arg0);
			long t1 = getStartTime(arg1);
			
			return (int) (t0-t1);
		}
	}
	
	private int drawNodeSC(Graphics big, int cores, NodeStatistics n, final int line) { 

		long [] activity = new long[cores];
		
		Arrays.fill(activity, Long.MIN_VALUE);
		
		int myLine = line;
		
		int start = (int) n.getEarliestStartTime();
		int end = (int) n.getLatestEndTime();
		
		big.setColor(Color.DARK_GRAY);

		big.fillRect(0, myLine, imageW, cores);

		big.setColor(Color.GRAY);
		big.fillRect(start, myLine, (end-start), cores);
		
		LinkedList<NodeStatistics.Event> tmp = new LinkedList<NodeStatistics.Event>();
		
		for (NodeStatistics.Event e : n.events) { 
			if (e.type == NodeStatistics.EventType.JOB_RESULT) { 
				tmp.add(e);
			}
		}

		Collections.sort(tmp, new TimeSorter()); 
		
		for (NodeStatistics.Event e : tmp) { 
			long [] data = (long[]) e.data;

			int jobTime = (int) Math.round(data[0] / 1000.0);
			int computeTime = (int) Math.round(data[4] / 1000.0);
			int transferTime = (int) Math.round(data[1] / 1000.0);

			int startT = (int) (e.time - jobTime);
			int computeT = (int) (e.time - computeTime);

			int index = -1;

			if (startT < 0) { 
				startT = 0;
			}
			
			for (int i=0;i<cores;i++) { 

				if (activity[i] <= startT) { 
					activity[i] = startT + jobTime-3;
					index = i;
					break;
				}
			}

			if (index == -1) { 
				System.err.println("EEP failed to find empty core slot! " + startT + " " 
						+ Arrays.toString(activity) + " " + e.ID);
				System.exit(1);
			}

			big.setColor(Color.RED);
			big.fillRect(startT, myLine+index, transferTime, 1);

			big.setColor(Color.YELLOW);
			big.fillRect(computeT, myLine+index, computeTime, 1);
			
			System.out.println("t" + e.ID + "(x) = " + startT + "<=x && x<=" 
					+ computeT + " ? " + (myLine+index) + " : 1/0");
		
			System.out.println("c" + e.ID + "(x) = " + computeT + "<=x && x<=" 
					+ (e.time) + " ? " + (myLine+index) + " : 1/0");
			
		}
		
		myLine += cores;
		
		return (myLine - line);	
	}
	
	private int drawCluster(Graphics big, ClusterStatistics c, final int line) { 

		int myLine = line;

		for (NodeStatistics n : c.nodes) { 
			
			if (c.runMultiCore) { 
				myLine += drawNodeMC(big, c.cores, n, myLine);
				myLine +=4;
			} else { 
				myLine += drawNodeSC(big, c.cores, n, myLine);
				myLine +=4;
			}
		}

		return (myLine - line);
	}

	private int drawClusterOld(Graphics big, ClusterStatistics c, int line) { 

		int myLine = line;

		for (NodeStatistics n : c.nodes) { 

			int start = (int) n.getEarliestStartTime();
			int end = (int) n.getLatestEndTime();

			big.setColor(Color.DARK_GRAY);
			big.fillRect(0, myLine, imageW, 1);

			big.setColor(Color.GRAY);
			big.fillRect(start, myLine, (end-start), 1);

			long lastStart = -1;

			for (NodeStatistics.Event e : n.events) { 

				switch (e.type) { 
				case STEAL_REQUEST:
					big.setColor(Color.WHITE);
					big.fillRect((int) e.time, myLine, scaleW, 1);
					break;
				case STEAL_REPLY_EMPTY:			
					big.setColor(Color.BLUE);
					big.fillRect((int) e.time-scaleW, myLine, 2*scaleW, 1);
					break;
				case STEAL_REPLY_DONE:			
					big.setColor(Color.RED);
					big.fillRect((int) e.time-scaleW, myLine, 2*scaleW, 1);
					break;
				case START_COMPUTE:
					if (lastStart != -1) { 
						big.setColor(Color.ORANGE);
						big.fillRect((int) lastStart+scaleW, myLine, (int) (e.time-lastStart), 1);
					} else { 
						System.out.println("EEP " + lastStart);
					}
					lastStart = e.time;
					break;
				case STEAL_REPLY_FULL:
					lastStart = e.time;
					big.setColor(Color.MAGENTA);
					big.fillRect((int) e.time-scaleW, myLine, 2*scaleW, 1);
					break;
				case JOB_RESULT:
					if (lastStart != -1) { 
						big.setColor(Color.YELLOW);
						big.fillRect((int) lastStart, myLine, (int) (e.time-lastStart), 1);
					} else { 
						System.out.println("EEP " + lastStart);
					}

					myLine++;

					lastStart = -1;
					break;
				}
			}

			myLine++;
			myLine++;

		}

		return (myLine - line);
	}

	public Dimension getPreferredSize() {
		return new Dimension(preferredSizeW, preferredSizeH);
	}

	/*
    private String getSiteStateString(ComputeResource c) {
        String res = "IDLE";

        ArrayList<Job> jobList = c.getJobList();

        for (int x = 0; x < jobList.size(); x++) {

            Job j = jobList.get(x);




            String stateString = j.getStateString();

            if (stateString.equals("RUNNING")) {
                if (res.equals("IDLE")) {
                    res = "RUNNING";
                }
            } else if (stateString.equals("SUBMITTING")) {
                res = "SUBMITTING";
            }
        }

        return res;
    }
	 */

	private Color selectColor() { 

		Color color = colorGenerator.getUniqueColor();

		if (SITES_TRANSPARENT) {
			color = new Color(color.getRed(), color.getGreen(), 
					color.getBlue(), 200);
		}

		return color;  
	}

	public void paint(Graphics g) {

		//    System.out.println("SCALE " + scale);

		double resize = scale / 100.0;

		int w = (int) (preferredSizeW / (2 * resize));
		int h = (int) (preferredSizeH / (2 * resize));

		int startX = posX - w;
		int startY = posY - h;

		int endX = posX + w;
		int endY = posY + h;

		//   System.out.println("Frawing map area: (" + startX + "," + startY 
		//            + ") to (" + endX + ", " + endY + ")");

		//     drawMap((Graphics2D) g, startX, startY, endX, endY);
		//    drawSites((Graphics2D) g, startX, startY, endX, endY);

		System.out.println("Paint");

		g.drawImage(map, 0, 0, preferredSizeW, preferredSizeH, 0, 0, imageW, imageH, null);
		/*        
                borderW, borderH, 
                preferredSizeW-borderW, preferredSizeH-borderH,// dst rectangle 
                startX, startY, endX, endY,             // src area of image
                null);
		 */
	} 

	/*
   private void drawMap(Graphics2D g, int startX, int startY, int endX, 
            int endY) {

           g.setColor(Color.black);
           g.fill(new Rectangle2D.Double(0, 0, preferredSizeW, preferredSizeH));


           g.drawImage(map,
                borderW, borderH, 
                preferredSizeW-borderW, preferredSizeH-borderH,// dst rectangle 
                startX, startY, endX, endY,             // src area of image
                null);
    }

    private void drawSite(Graphics2D g, Slot s, int x, int y, 
            FontRenderContext frc, Font f, ComputeResource m) { 

        RoundRectangle2D rect = new RoundRectangle2D.Double(s.x, s.y, borderW, 
                borderW, 10, 10);

        Ellipse2D elipse = 
            new Ellipse2D.Double(x-DOT_CIRCLE_SIZE/2.0, y-DOT_CIRCLE_SIZE/2.0, 
                    DOT_CIRCLE_SIZE, DOT_CIRCLE_SIZE);

        Color c = m.getColor();

        if (c == null) { 
            c = selectColor();
            m.setColor(c);
        }

        g.setColor(c);

        g.fill(elipse);
        g.fill(rect);

        int lx = -1;
        int ly = -1;


        if (s.y == 0) { 
            // top row
            lx = s.x + borderW/2;
            ly = borderH;

        } else if (s.y == preferredSizeH-borderH) { 
            // bottom row
            lx = s.x + borderW/2;
            ly = preferredSizeH-borderH;
        } else { 

            if (s.x == 0) { 
                // left column
                lx = borderW;
                ly = s.y + borderH/2;
             } else {
                // right colunm
                 lx = preferredSizeW-borderW;
                 ly = s.y + borderH/2;
             }
        }

        g.setStroke(stroke);
        g.drawLine(lx, ly, x, y);

        g.setColor(Color.darkGray);        
        g.setStroke(thinStroke);
        g.draw(elipse);
        g.draw(rect);

        // Draw the text into the slot
        TextLayout tl = new TextLayout(m.getFriendlyName(), f, frc);

        float sw = (float) tl.getBounds().getWidth();
        float sh = (float) tl.getBounds().getHeight();

        double scale = (borderW-4) / sw;

        if (scale > 1.0) { 
            scale = 1.0;
        }

        AffineTransform t1 = new AffineTransform();
        t1.setToScale(scale, scale);

        AffineTransform t2 = new AffineTransform();
        t2.setToTranslation(s.x, s.y+borderW/2+(sh/2)*scale);

        t2.concatenate(t1);

        Shape sha = tl.getOutline(t2);

        g.setColor(Color.black);
        g.draw(sha);
        g.setColor(Color.white);
        g.fill(sha);
    }

    private void drawEmptySlot(Graphics2D g, int x, int y, int w, int h) { 
        g.setColor(Color.black);
        g.fill(new Rectangle(x, y, w, h));
    } */

	/*  
    private void drawSites(Graphics2D g2, int startX, int startY,
            int endX, int endY) {

        // reset the used slots
        for (Slot s: used) { 
            s.owner = null;
        }

        used.clear();

        double resize = (preferredSizeW-borderW*2.0)/(endX - startX); 

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);

        FontRenderContext frc = g2.getFontRenderContext();
        Font f = new Font("SansSerif", Font.BOLD, 20); 

        ComputeResource [] machines = deploy.getComputeResources();

        LinkedList<ComputeResource> visible = new LinkedList<ComputeResource>();

        for (int i = 0; i < machines.length; i++) {
            ComputeResource m = machines[i];

            int x = m.getX();
            int y = m.getY();

            if (x <= startX || x >= endX) { 
   //             System.out.println("ComputeResource: " + m.getFriendlyName() 
   //                     + " not visible (" + x + "!, " + y + ")");
                continue;
            }

            if (y <= startY || y >= endY) { 
   //             System.out.println("ComputeResource: " + m.getFriendlyName() 
   //                     + " not visible (" + x + ", " + y + "!)");
                continue;
            }

    //        System.out.println("ComputeResource: " + m.getFriendlyName() 
   //                 + " is visible!");

            visible.addLast(m);
        }

        while (visible.size() > 0) { 

          //  System.out.println("Size " + visible.size());

            ComputeResource tmp = visible.removeFirst();

            int x = tmp.getX();
            int y = tmp.getY();

            x = borderW + (int) ((x - startX) * resize);
            y = borderH + (int) ((y - startY) * resize);

            // Find the best slot for this resource....
            Slot best = null;
            double distance = Double.MAX_VALUE;

            for (Slot s : slots) {
                double d = Math.sqrt( (s.x - x) * (s.x - x) + (s.y - y) * (s.y - y));  

                if (d < distance) {

                    // Check if the slot is aready used...
                    if (s.owner != null) { 
                        // Check if we are closer..
                        if (d + 0.00001 < s.distance ) { 
                            // We may steal this slot
                            best = s; 
                            distance = d;
            //     System.out.println("option " + d + " S");

                        } else { 
                            // we may not steal this slot
                        }
                    } else { 
                        // unused slot
                        best = s;
                        distance = d;
            //    System.out.println("option " + d + " S");

                    }
                }
            }

            if (best != null) { 

                // See if we have stolen a slot ...
                if (best.owner != null) { 
                    visible.addLast(best.owner);
                    best.owner = tmp;
                    best.distance = distance;

             //       System.out.println("BEST " + distance + " S");

                } else { 
                    best.owner = tmp;
                    best.distance = distance;

            //        System.out.println("BEST " + distance);

                    used.addLast(best);
                }
            }
        }

        for (Slot s : used) { 

            ComputeResource m = s.owner;

            int x = borderW + (int) ((m.getX() - startX) * resize);
            int y = borderH + (int) ((m.getY() - startY) * resize);

            drawSite(g2, s, x, y, frc, f, m);

        }

    }

    private ComputeResource getResourceFromUsedSlot(int x, int y) { 

        for (Slot s : used) { 

            if (x > s.x && x < s.x+borderW && y > s.y && y < s.y+borderH) { 
                if (s.owner != null) { 
                    return s.owner;
                }
            }
        }

        return null;

    }
	 */ 

	public void mouseClicked(MouseEvent e) {
		/*     
        if (used.size() == 0) { 
            return;
        }

        ComputeResource r = getResourceFromUsedSlot(e.getX(), e.getY());

        if (r != null) { 
            System.out.println("Clicked on " + r.getFriendlyName());
            deploy.deployApplication(r);
        }
		 */        
	}

	public void mouseEntered(MouseEvent e) {
		// Unused
	}

	public void mouseExited(MouseEvent arg0) {
		// Unused
	}

	public void mousePressed(MouseEvent e) {
		mouseLocationX = e.getX();
		mouseLocationY = e.getY(); 
	}

	public void mouseReleased(MouseEvent e) {
		mouseLocationX = -1;
		mouseLocationY = -1;
	}

	public void mouseDragged(MouseEvent e) {

		if (mouseLocationX == -1) { 
			System.out.println("EEP!");
			return;
		}

		int currentX = e.getX();
		int currentY = e.getY();

		int dx = currentX - mouseLocationX;
		int dy = currentY - mouseLocationY;

		int newX = posX - dx; 
		int newY = posY - dy;

		if (newX < (imageW - preferredSizeW/2) && newX > (preferredSizeW /2)) { 
			posX = newX;
		}

		if (newY < (imageH - preferredSizeH/2) && newY > (preferredSizeH /2)) { 
			posY = newY;
		}

		mouseLocationX = currentX;
		mouseLocationY = currentY;

		repaint();
	}

	public void mouseMoved(MouseEvent arg0) {
		// Unused
	}

	public void mouseWheelMoved(MouseWheelEvent e) {

		//  System.out.println("Mouse wheel: " + e);

		int rotation = e.getWheelRotation();

		if (rotation > 0) {
			scale += SCALE_STEP;

			if (scale > MAX_SCALE) { 
				scale = MAX_SCALE;
			}

			repaint();
		} else if (rotation < 0) {
			scale -= SCALE_STEP;

			if (scale < MIN_SCALE) { 
				scale = MIN_SCALE;
			}

			repaint();
		}

		// System.out.println("scale = " + scale + " rotation " + rotation);
	}
}
