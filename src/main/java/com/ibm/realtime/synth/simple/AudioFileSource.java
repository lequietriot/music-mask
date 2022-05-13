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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * An AudioInput implementation that provides
 * its data from a file loaded into memory
 * 
 * @author florian
 *
 */
public class AudioFileSource implements AudioInput {

	/**
	 * The audio data
	 */
	private byte[] audioData;
	
	/**
	 * the format of the audioData
	 */
	private AudioFormat format;
	
	/**
	 * Current read position in audioData
	 */
	private int pos;
	
	/**
	 * Construct a new instance of the AudiOFileSource.
	 * The file passed as parameter will be loaded completely
	 * into a byte array. 
	 * @param inputFile the file to be loaded and played by this stream
	 */
	public AudioFileSource(File inputFile) throws IOException, UnsupportedAudioFileException {
		Debug.debug("Loading "+inputFile);
		AudioInputStream ais = AudioSystem.getAudioInputStream(inputFile);
		format = ais.getFormat();
		
		// read the file into an array
		// very inefficient implementation, can be improved!
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] temp = new byte[16384];
		while (true) {
			int read = ais.read(temp);
			if (read > 0) {
				baos.write(temp, 0, read);
			}
			else if (read == 0) {
				Thread.yield();
			} else {
				break;
			}
		}
		audioData = baos.toByteArray();
		pos = 0;
	}
	
	/**
	 * construct an AudioFileSource from a preloaded array.
	 * The array is not copied.
	 */
	public AudioFileSource(byte[] buffer, AudioFormat format) {
		this.audioData = buffer;
		this.format = format;
		pos = 0;
	}

	
	public void read(FloatSampleBuffer buffer) {
		int readBytes = buffer.getByteArrayBufferSize(format);
		if (readBytes > audioData.length - pos) {
			readBytes = audioData.length - pos;
		}
		
		if (readBytes == 0) {
			buffer.makeSilence();
			return;
		}
		
		//Debug.debug("read at pos="+pos+" length="+readBytes);
		
		// store the current buffer's format
		int channels = buffer.getChannelCount();
		float sampleRate = buffer.getSampleRate();
		int samples = buffer.getSampleCount();
		
		// convert audioData to the buffers format
		buffer.initFromByteArray(audioData, pos, readBytes, format);
		pos += readBytes;

		// now apply the original buffer's data
		if (buffer.getChannelCount()<channels) {
			// fixme: will not work if buffer.getChannelCount()!=1
			buffer.expandChannel(channels);  
		}
		while (buffer.getChannelCount()>channels) {
			buffer.removeChannel(buffer.getChannelCount()-1);
		}
		buffer.setSampleRate(sampleRate);
		buffer.changeSampleCount(samples, true);
	}
	
	public AudioFileSource makeClone() {
		return new AudioFileSource(audioData, format);
	}
	
	public AudioFormat getFormat() {
		return format;
	}
	
	public boolean done() {
		return pos>=audioData.length;
	}
	
}
