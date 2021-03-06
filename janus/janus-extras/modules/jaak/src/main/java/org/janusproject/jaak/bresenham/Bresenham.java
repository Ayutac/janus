/* 
 * $Id$
 * 
 * Janus platform is an open-source multiagent platform.
 * More details on <http://www.janus-project.org>
 * Copyright (C) 2010-2011 Janus Core Developers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.janusproject.jaak.bresenham;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.arakhne.afc.math.discrete.object2d.Point2i;

/** This class provides 2D Bresenham algorithms.
 * <p>
 * The label "Bresenham" is used today for a whole family of algorithms extending 
 * or modifying Bresenham's original algorithm. See further functions below.
 * 
 * @author $Author: sgalland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
public class Bresenham {

	/** The Bresenham line algorithm is an algorithm which determines which points in 
	 * an n-dimensional raster should be plotted in order to form a close 
	 * approximation to a straight line between two given points. It is 
	 * commonly used to draw lines on a computer screen, as it uses only 
	 * integer addition, subtraction and bit shifting, all of which are 
	 * very cheap operations in standard computer architectures. It is one of the 
	 * earliest algorithms developed in the field of computer graphics. A minor extension 
	 * to the original algorithm also deals with drawing circles.
	 * <p>
	 * While algorithms such as Wu's algorithm are also frequently used in modern 
	 * computer graphics because they can support antialiasing, the speed and 
	 * simplicity of Bresenham's line algorithm mean that it is still important. 
	 * The algorithm is used in hardware such as plotters and in the graphics 
	 * chips of modern graphics cards. It can also be found in many software 
	 * graphics libraries. Because the algorithm is very simple, it is often 
	 * implemented in either the firmware or the hardware of modern graphics cards.
	 * 
	 * @param x0 is the x-coordinate of the first point of the Bresenham line.
	 * @param y0 is the y-coordinate of the first point of the Bresenham line.
	 * @param x1 is the x-coordinate of the last point of the Bresenham line.
	 * @param y1 is the y-coordinate of the last point of the Bresenham line.
	 * @return an iterator on the points along the Bresenham line.
	 */
	public static Iterator<Point2i> line(int x0, int y0, int x1, int y1) {
		return new LineIterator(x0, y0, x1, y1);
	}

	/** Replies the points on a rectangle.
	 * 
	 * @param x0 is the x-coordinate of the lowest point of the rectangle.
	 * @param y0 is the y-coordinate of the lowest point of the rectangle.
	 * @param width is the width of the rectangle.
	 * @param height is the height of the rectangle.
	 * @param firstSide indicates the first rectangle side to reply: <code>0</code>
	 * for top, <code>1</code> for right, <code>2</code> for bottom, or
	 * <code>3</code> for left.
	 * @return an iterator on the points along the rectangle.
	 */
	public static Iterator<Point2i> rectangle(int x0, int y0, int width, int height, int firstSide) {
		return new RectangleIterator(x0, y0, width, height, firstSide);
	}

	/** The Bresenham line algorithm is an algorithm which determines which points in 
	 * an n-dimensional raster should be plotted in order to form a close 
	 * approximation to a straight line between two given points. It is 
	 * commonly used to draw lines on a computer screen, as it uses only 
	 * integer addition, subtraction and bit shifting, all of which are 
	 * very cheap operations in standard computer architectures. It is one of the 
	 * earliest algorithms developed in the field of computer graphics. A minor extension 
	 * to the original algorithm also deals with drawing circles.
	 * <p>
	 * While algorithms such as Wu's algorithm are also frequently used in modern 
	 * computer graphics because they can support antialiasing, the speed and 
	 * simplicity of Bresenham's line algorithm mean that it is still important. 
	 * The algorithm is used in hardware such as plotters and in the graphics 
	 * chips of modern graphics cards. It can also be found in many software 
	 * graphics libraries. Because the algorithm is very simple, it is often 
	 * implemented in either the firmware or the hardware of modern graphics cards.
	 * 
	 * @author St&eacute;phane GALLAND &lt;stephane.galland@utbm.fr&gt;
	 * @version $FullVersion$
	 * @mavengroupid org.janus-project.jaak
	 * @mavenartifactid util
	 */
	private static class LineIterator implements Iterator<Point2i> {

		private final boolean steep;
		private final int ystep;
		private final int xstep;
		private final int deltax;
		private final int deltay;
		private final int x1;
		private int y, x;
		private int error;

		/**
		 * @param x0 is the x-coordinate of the first point of the Bresenham line.
		 * @param y0 is the y-coordinate of the first point of the Bresenham line.
		 * @param x1 is the x-coordinate of the last point of the Bresenham line.
		 * @param y1 is the y-coordinate of the last point of the Bresenham line.
		 */
		public LineIterator(int x0, int y0, int x1, int y1) {
			int _x0 = x0;
			int _y0 = y0;
			int _x1 = x1;
			int _y1 = y1;

			this.steep = Math.abs(_y1 - _y0) > Math.abs(_x1 - _x0);

			int swapv;
			if (this.steep) {
				//swap(x0, y0);
				swapv = _x0;
				_x0 = _y0;
				_y0 = swapv;
				//swap(x1, y1);
				swapv = _x1;
				_x1 = _y1;
				_y1 = swapv;
			}
			/*if (_x0 > _x1) {
				//swap(x0, x1);
				swapv = _x0;
				_x0 = _x1;
				_x1 = swapv;
				//swap(y0, y1);
				swapv = _y0;
				_y0 = _y1;
				_y1 = swapv;
			}*/

			this.deltax = Math.abs(_x1 - _x0);
			this.deltay = Math.abs(_y1 - _y0);
			this.error = this.deltax / 2;
			this.y = _y0;

			if (_x0 < _x1) this.xstep = 1;
			else this.xstep = -1;
			
			if (_y0 < _y1) this.ystep = 1;
			else this.ystep = -1;
			
			this.x1 = _x1;
			this.x = _x0;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return ((this.xstep>0) && (this.x <= this.x1))
				   ||((this.xstep<0) && (this.x1 <= this.x));
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Point2i next() {
			Point2i p = new Point2i();
			
			if (this.steep) {
				p.set(this.y, this.x);
			}
			else {
				p.set(this.x, this.y);
			}
			
			this.error = this.error - this.deltay;
			
			if (this.error < 0) {
				this.y = this.y + this.ystep;
				this.error = this.error + this.deltax;
			}

			this.x += this.xstep;
			
			return p;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	} // class LineIterator

	/** Iterates on points on a rectangle.
	 * 
	 * @author St&eacute;phane GALLAND &lt;stephane.galland@utbm.fr&gt;
	 * @version $FullVersion$
	 * @mavengroupid org.janus-project.jaak
	 * @mavenartifactid util
	 */
	private static class RectangleIterator implements Iterator<Point2i> {

		private final int x0;
		private final int y0;
		private final int x1;
		private final int y1;
		private final int firstSide;
		
		private int currentSide;
		private int i;
		
		/**
		 * @param x
		 * @param y
		 * @param width
		 * @param height
		 * @param firstSide
		 */
		public RectangleIterator(int x, int y, int width, int height, int firstSide) {
			assert(firstSide>=0 && firstSide<4);
			this.firstSide = firstSide;
			this.x0 = x;
			this.y0 = y;
			this.x1 = x+width-1;
			this.y1 = y+height-1;
			
			this.currentSide = (width>0 && height>0) ? this.firstSide : -1;
			this.i = 0;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return this.currentSide!=-1;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Point2i next() {
			int x, y;
			
			switch(this.currentSide) {
			case 0: // top
				x = this.x0+this.i;
				y = this.y0;
				break;
			case 1: // right
				x = this.x1;
				y = this.y0+this.i;
				break;
			case 2: // bottom
				x = this.x1-this.i;
				y = this.y1;
				break;
			case 3: // left
				x = this.x0;
				y = this.y1-this.i;
				break;
			default:
				throw new NoSuchElementException();
			}
			
			++ this.i;
			int newSide = -1;
			
			switch(this.currentSide) {
			case 0: // top
				if (x>=this.x1) {
					newSide = 1;
					this.i = 0;
				}
				break;
			case 1: // right
				if (y>=this.y1) {
					newSide = 2;
					this.i = 0;
				}
				break;
			case 2: // bottom
				if (x<=this.x0) {
					newSide = 3;
					this.i = 0;
				}
				break;
			case 3: // left
				if (y<=this.y0) {
					newSide = 0;
					this.i = 0;
				}
				break;
			default:
				throw new NoSuchElementException();
			}

			if (newSide!=-1) {
				this.currentSide = (this.firstSide==newSide) ? -1 : newSide;
			}
			
			return new Point2i(x,y);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	} // class RectangleIterator
	
}