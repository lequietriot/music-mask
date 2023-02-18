/*
 * Copyright (c) 2023, Rodolfo Ruiz-Velasco <ruizvelascorodolfo@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package rs.musicmask.midisynth;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Arrays;

/**
 * A class that connects to your own sound device in order to play audio out loud.
 */
public class DevicePcmPlayer {

	/**
	 * The Audio Format of the sound being output.
	 */
	public AudioFormat format;

	/**
	 * The device being used to output sound to.
	 */
	public SourceDataLine line;

	/**
	 * A byte array to write audio data samples to.
	 */
	public byte[] byteSamples;

	/**
	 * An integer array that is filled with data samples.
	 */
	public int[] samples;

	/**
	 * The stream used for sound.
	 */
	public MidiAudioStream stream;

	/**
	 * An integer value determining the default sample rate for output audio.
	 */
	public static int sampleRate = 44100;

	/**
	 * A boolean value determining if the audio is stereo or not.
	 */
	public static boolean stereo = true;

	/**
	 * A method to set the default output audio format, as well as the empty byte array that sound will be written to later.
	 */
	public void init() {
		this.format = new AudioFormat((float) sampleRate, 16, stereo ? 2 : 1, true, false);
		this.byteSamples = new byte[256 << (stereo ? 2 : 1)];
	}

	/**
	 * A method to get the default audio output device, open it, and start using it.
	 * @throws LineUnavailableException Try to re-open the device for sound output. If not possible, an error occurs.
	 */
	public void open() throws LineUnavailableException {
		try {
			Info deviceInfo = new Info(SourceDataLine.class, this.format, 4096);
			this.line = (SourceDataLine) AudioSystem.getLine(deviceInfo);
			this.line.open();
			this.line.start();
		} catch (LineUnavailableException lineUnavailableException) {
			if (this.line.available() != -1) {
				this.open();
			} else {
				this.line = null;
				throw lineUnavailableException;
			}
		}
	}

	/**
	 * A method to set the default stream for the sound output.
	 * @param audioStream The stream to set for playback.
	 */
	public final void setStream(MidiAudioStream audioStream) {
		this.stream = audioStream;
	}

	/**
	 * A method that takes the currently set stream and fills an empty integer array with audio data.
	 * @param samplesToWrite The sample integer array to write values to.
	 * @param amount The amount of samples to write to the integer array.
	 */
	public final void fill(int[] samplesToWrite, int amount) {
		Arrays.fill(samplesToWrite, 0);
		if (this.stream != null) {
			this.stream.fill(samplesToWrite, amount);
		}
	}

	/**
	 * A method to write audio data to the selected output sound device, which plays the sound out loud.
	 */
	public void write() {
		int length = 256;
		if (DevicePcmPlayer.stereo) {
			length <<= 1;
		}

		for (int index = 0; index < length; ++index) {
			int sample = samples[index];
			if ((sample + 8388608 & -16777216) != 0) {
				sample = 8388607 ^ sample >> 31;
			}

			this.byteSamples[index * 2] = (byte) (sample >> 8);
			this.byteSamples[index * 2 + 1] = (byte) (sample >> 16);
		}
		this.line.write(this.byteSamples, 0, length << 1);
	}

}
