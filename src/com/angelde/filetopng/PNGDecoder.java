/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s): Alexandre Iline.
 *
 * The Original Software is the Jemmy library.
 * The Initial Developer of the Original Software is Alexandre Iline.
 * All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 *
 *
 * $Id$ $Revision$ $Date$
 *
 */

package com.angelde.filetopng;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * PNGDecoder taken from http://www.java2s.com/Code/Java/2D-Graphics-GUI/PNGDecoder.htm and modified 
 * to FileToPNG's benefits.
 */
public class PNGDecoder extends Object {
	
	private InputStream in;
	
	public PNGDecoder(InputStream in) {
		this.in = in;
	}

	private byte read() throws IOException {
		byte b = (byte)in.read();
		return(b);
	}
	
	private int readInt() throws IOException {
		byte b[] = read(4);
		return(((b[0]&0xff)<<24) +
				((b[1]&0xff)<<16) +
				((b[2]&0xff)<<8) +
				((b[3]&0xff)));
	}
	
	private byte[] read(int count) throws IOException {
		byte[] result = new byte[count];
		for(int i = 0; i < count; i++) {
			result[i] = read();
		}
		return(result);
	}
	
	private boolean compare(byte[] b1, byte[] b2) {
		if(b1.length != b2.length) {
			return(false);
		}
		for(int i = 0; i < b1.length; i++) {
			if(b1[i] != b2[i]) {
				return(false);
			}
		}
		return(true);
	}
	
	void checkEquality(byte[] b1, byte[] b2) {
		if(!compare(b1, b2)) {
			throw(new RuntimeException("Format error"));
		}
	}
	
	public Image decode() throws IOException {
		
		byte[] id = read(12);
		checkEquality(id, new byte[] {-119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13});
		
		byte[] ihdr = read(4);
		checkEquality(ihdr, "IHDR".getBytes());
		
		int width = readInt();
		int height = readInt();
		
		Image result = new Image(width, height);
		
		byte[] head = read(5);
		if(!compare(head, new byte[]{8, 2, 0, 0, 0})) {
			throw(new RuntimeException("Format error"));
		}
		
		readInt();
		
		int size = readInt();
		
		byte[] idat = read(4);
		checkEquality(idat, "IDAT".getBytes());
		
		byte[] data = read(size);
		
		
		Inflater inflater = new Inflater();
		inflater.setInput(data, 0, size);
		
		int color;
		
		try {
			byte[] row = new byte[width * 3];
			for (int y = 0; y < height; y++) {
				inflater.inflate(new byte[1]);
				inflater.inflate(row);
				for (int x = 0; x < width; x++) {
					result.setPixel(x, y, 
							((row[x * 3 + 0]&0xff) << 16) +
							((row[x * 3 + 1]&0xff) << 8) +
							((row[x * 3 + 2]&0xff)));
				}
			}
		} catch(DataFormatException e) {
			throw(new RuntimeException("ZIP error"+e));
		}
		
		readInt();
		readInt();
		
		byte[] iend = read(4);
		checkEquality(iend, "IEND".getBytes());
		
		readInt();
		in.close();
		
		return result;
	}
	
}
