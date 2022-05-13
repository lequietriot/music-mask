/*
 * (C) Copyright IBM Corp. 2005, 2008
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ibm.realtime.synth.soundfont2;

import com.ibm.realtime.synth.utils.AudioUtils;

import java.io.FileOutputStream;

/**
 * A class with utility methods for the SoundFont synth.
 * 
 * @author florian
 */
public class SoundFontUtils {

	/**
	 * The default value for many delay values, in seconds. This is equal to
	 * timecents2seconds(-12000).
	 */
	public static final double DEFAULT_ARTICULATION_DELAY = 0.0009765625;

	/**
	 * Calculate the transform according to the parameters. Value needs to be
	 * normalized 0..1 for unipolar and -1...+1 for bipolar transforms.
	 * 
	 * @param value the input value, [min...max]
	 * @param negative if true, the transform is negative
	 * @param unipolar if true, the transform is unipolar or bipolar otherwise.
	 * @param convex if true, calculate the convex transform, otherwise concave
	 * @return the transformed value
	 */
	public static final double transform(double value, boolean negative,
			boolean unipolar, boolean convex) {
		// TODO: use a table
		double factor;
		double sig;
		if (!unipolar) {
			// bipolar
			sig = Math.signum(value);
			if (!convex) {
				value = sig - value;
			}
			factor = sig * -20.0 / 96.0;
			if (negative) {
				factor = -factor;
				sig = -sig;
			}
		} else {
			// unipolar
			factor = -20.0 / 96.0;
			sig = 1.0;
			if (negative == convex) {
				// need to reverse the value if
				// - positive, and if
				// - convex
				// So if it is positive *and* convex, it would be reversed
				// twice, with no effect, so it only needs to be reversed
				// if (!negative && !convex) or (convex && negative).
				value = sig - value;
			}
		}
		double ret = factor * Math.log10((value * value));
		if (convex) {
			ret = sig - ret;
		}

		if (unipolar) {
			if (ret < 0.0) {
				ret = 0.0;
			} else if (ret > 1.0) {
				ret = 1.0;
			}
		} else {
			if (ret < -1.0) {
				ret = -1.0;
			} else if (ret > 1.0) {
				ret = 1.0;
			}
		}
		return ret;
	}

	/**
	 * @param timecents [-32768...+32767]
	 * @return the converted timecents, in seconds
	 */
	public static final double timecents2seconds(int timecents) {
		if (timecents <= -32768) {
			return 0.0;
		}
		// TODO: use a table
		return Math.pow(2.0, timecents / 1200.0);
	}

	/**
	 * @param seconds the time in seconds
	 * @return the converted timecents [-32768...+32767]
	 */
	public static final int seconds2timecents(double seconds) {
		if (seconds <= 0) {
			return -32768;
		} else if (seconds >= 165910888) {
			return 32767;
		}
		// TODO: use a table
		return (int) (1200.0 * Math.log(seconds) / Math.log(2));
	}

	/**
	 * Convert cents to hertz, for LFO frequency.
	 * 
	 * @param cents [-32768...+32767]
	 * @return the converted frequency, in Hz
	 */
	public static final double cents2hertzLFO(int cents) {
		// TODO: use a table
		return Math.pow(2.0, cents / 1200.0) * 8.176;
	}

	/**
	 * Convert cents to hertz, for filter cutoff frequency.
	 * 
	 * @param cents [-32768...+32767]
	 * @return the converted frequency, in Hz
	 */
	public static final double cents2hertzCutoff(int cents) {
		// TODO: use a table
		return Math.pow(2.0, (cents - 6900) / 1200.0) * 440.0;
	}

	/**
	 * Calculate the linear factor corresponding to the given decibel
	 * attenuation. This separate function is necessary, because SoundFont
	 * synths use a non-standard formula to convert the attenuation to a linear
	 * factor. It's specifed as being in decibels, but tests showed that it uses
	 * a different scaling factor. This scaling factor is the one used in the
	 * open source projects timidity++ and fluidsynth.
	 * 
	 * @param attenuation the attenuation in CreativeLabs dB, [0...144]
	 * @return the corresponding value as a linear factor [1...0]
	 */
	public final static double attenuation2linear(double attenuation) {
		if (attenuation >= 144.0) {
			return 0.0;
		}
		//return Math.pow(10.0, attenuation / -531.509);
		// 0.037628713718864591192246979825365 =  (20.0/-531.509)
		return AudioUtils.decibel2linear(attenuation * 0.037628713718864591);
	}

	/**
	 * Test: dump the transforms to binary 16-bit signed little endian files
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		dump(false, true, false);
		dump(true, true, false);
		dump(false, false, false);
		dump(true, false, false);
		dump(false, true, true);
		dump(true, true, true);
		dump(false, false, true);
		dump(true, false, true);
		// negative unipolar concave transform for velocity
		// System.out.println("Attenuation of a velocity of
		// 1:"+(transform(1/128.0, true, true, false)*96)+"dB");
		// System.out.println("Attenuation of a velocity of
		// 96:"+(transform(96.0/127.0, true, true, false)*96)+"dB");
		// System.out.println("Attenuation of a velocity of
		// 110:"+(transform(110.0/128.0+(1/128.0), true, true, false)*96)+"dB");
		// System.out.println("Attenuation of a velocity of
		// 126:"+(transform(126.0/128.0+(1/128.0), true, true, false)*96)+"dB");
		// System.out.println("Attenuation of a velocity of
		// 127:"+(transform(127.0/128.0+(1/128.0), true, true, false)*96)+"dB");

		int vol = 120;
		int expr = 90;
		double dB1 =
				SoundFontUtils.transform(vol / 127.0, true, true, false) * 96.0;
		double dB2 =
				SoundFontUtils.transform(expr / 127.0, true, true, false) * 96.0;
		System.out.println("vol=" + vol + " expr=" + expr + " -> "
				+ (dB1 + dB2));
		double dB =
				SoundFontUtils.transform(vol * expr / (127.0 * 127.0), true,
						true, false) * 96.0;
		System.out.println("   ?=" + dB + "dB");

	}

	public static void dump(boolean neg, boolean uni, boolean convex) {
		double min = 0.0;
		if (!uni) {
			min = -1.0;
		}
		boolean PRINT = true;
		boolean SAVE = false;
		try {
			if (PRINT) {
				int COUNT = 10;
				System.out.println("" + (neg ? "negative" : "positive") + "  "
						+ (uni ? "unipolar" : "bipolar") + "  "
						+ (convex ? "convex" : "concave"));
				System.out.print("  ");
				for (int i = 0; i <= COUNT; i++) {
					double thisIndex = min + (((1.0 - min) * i) / COUNT);
					System.out.print(/* "i="+(Math.round(thisIndex*100)/100.0)+":"+ */""
							+ (Math.round(transform(thisIndex, neg, uni, convex) * 1000) / 1000.0)
							+ ", ");
				}
				System.out.println("");
			}
			if (SAVE) {
				int COUNT = 100;
				// create a 16-bit mono file, little endian
				byte[] data = new byte[(COUNT + 1) * 2];
				for (int i = 0; i <= COUNT; i++) {
					double thisIndex = min + (((1.0 - min) * i) / COUNT);
					double value = transform(thisIndex, neg, uni, convex);
					short val = (short) (value * 32767);
					data[i * 2] = (byte) (val & 0xFF);
					data[(i * 2) + 1] = (byte) ((val >> 8) & 0xFF);
				}
				FileOutputStream fos =
						new FileOutputStream((convex ? "convex" : "concave")
								+ "Transform_" + (neg ? "neg" : "pos") + "_"
								+ (uni ? "unipolar" : "bipolar") + ".pcm");
				fos.write(data);
				fos.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
