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
package com.ibm.realtime.synth.simple;

import com.ibm.realtime.synth.utils.AudioUtils;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that reads from an AudioInput and writes the data to the soundcard.
 * 
 * @author florian
 * 
 */
public class SoundcardSink implements Runnable {

	/**
	 * the AudioInput object where this sink gets its data from
	 */
	private AudioInput stream;

	/**
	 * The SourceDataLine used to access the soundcard
	 */
	private SourceDataLine sdl;

	/**
	 * flag to notify the write thread to stop execution
	 */
	private volatile boolean stopped;

	/**
	 * The thread for the actual writing
	 */
	private Thread runner;

	/**
	 * List of usable audio devices, i.e. they provide a SourceDataLine (line
	 * out/speaker).
	 */
	private static List<Mixer.Info> devList;

	/**
	 * Constructor for this sink
	 */
	public SoundcardSink() {
		setupAudioDevices();
	}

	/**
	 * Open the soundcard with the given format
	 */
	public void open(int devIndex, int bufferSizeInMillis, AudioFormat format,
			AudioInput stream) throws LineUnavailableException, Exception {
		if (devIndex < -1 || devIndex >= devList.size()) {
			throw new Exception("illegal audio device index: " + devIndex);
		}
		assert (stream != null);
		this.stream = stream;
		if (devIndex < 0) {
			Debug.debug("Opening default soundcard");
			sdl = AudioSystem.getSourceDataLine(format);
		} else {
			Mixer.Info info = devList.get(devIndex);
			Debug.debug("Opening soundcard " + info);
			sdl = AudioSystem.getSourceDataLine(format, info);
		}
		sdl.open(format, AudioUtils.millis2bytes(bufferSizeInMillis, format));
		System.out.println("Buffer size = " + sdl.getBufferSize() + " bytes = "
				+ (sdl.getBufferSize() / format.getFrameSize()) + " samples = "
				+ AudioUtils.bytes2millis(sdl.getBufferSize(), format)
				+ " millis");
	}

	public void start() {
		runner = new Thread(this);
		runner.start();
	}

	public void stop() {
		if (runner != null && !stopped) {
			stopped = true;
			synchronized (runner) {
				try {
					runner.join();
				} catch (InterruptedException ie) {
				}
			}
		}
	}

	public void close() {
		stop();
		if (sdl != null) {
			sdl.close();
		}
		Debug.debug("closed soundcard");
	}

	/**
	 * The actual loop of reading from the AudioInput and writing it to the
	 * soundcard
	 */
	public void run() {
		// Debug.debug("in soundcard writing thread");
		stopped = false;
		sdl.start();
		AudioFormat format = sdl.getFormat();
		// set up the temporary buffer for reading
		FloatSampleBuffer buffer = new FloatSampleBuffer(format.getChannels(),
				sdl.getBufferSize() / format.getFrameSize(), format
						.getSampleRate());
		// set up the temporary buffer that receives the converted
		// samples in bytes
		byte[] byteBuffer = new byte[buffer.getByteArrayBufferSize(format)];

		while (!stopped) {
			stream.read(buffer);
			buffer.convertToByteArray(byteBuffer, 0, format);
			sdl.write(byteBuffer, 0, byteBuffer.length);
		}
		sdl.stop();
		// Debug.debug("left soundcard writing thread");
	}

	@SuppressWarnings("unchecked")
	public static List getDeviceList() {
		setupAudioDevices();
		return devList;
	}

	private static void setupAudioDevices() {
		if (devList == null) {
			System.out.print("Gathering Audio devices...");
			devList = new ArrayList<Mixer.Info>();
			Mixer.Info[] infos = AudioSystem.getMixerInfo();
			// go through all audio devices and see if they provide input
			// line(s)
			for (Mixer.Info info : infos) {
				// try {
				Mixer m = AudioSystem.getMixer(info);
				Line.Info[] lineInfos = m.getSourceLineInfo();
				for (Line.Info lineInfo : lineInfos) {
					if (lineInfo instanceof DataLine.Info) {
						// we found a source data line, so we cann this mixer
						// to the list of supported devices
						devList.add(info);
						break;
					}
				}
				// } catch (MidiUnavailableException mue) {
				// Debug.debug(mue);
				// }
			}
			System.out.println("done (" + devList.size()
					+ " devices available).");
		}
	}
}
