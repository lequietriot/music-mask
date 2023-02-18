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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A class which holds the data and information for the Sound Bank's individual Music Patch, essentially an instrument.
 */
public class MusicPatch {

	/**
	 * An integer value for the overall volume of this music patch.
	 */
	public int volume;

	/**
	 * An array of the samples used in this music patch.
	 */
	public AudioDataSource[] audioDataSources;

	/**
	 * An array of short values containing the precise root pitches for each sample.
	 */
	public short[] pitchOffset;

	/**
	 * An array of byte values containing the default volume for each sample.
	 */
	public byte[] volumeOffset;

	/**
	 * An array of byte values containing the default pan data for each sample.
	 */
	public byte[] panOffset;

	/**
	 * An array of envelope values for each sample in the music patch.
	 */
	public MusicPatchEnvelope[] musicPatchEnvelopes;

	/**
	 * An array of byte values containing the default loop mode for each sample.
	 */
	public byte[] loopOffset;

	/**
	 * A HashMap of all the available samples. Used to check for if a sample already exists, to avoid loading duplicates.
	 */
	public HashMap<AudioDataSource, String> availableSources;

	/**
	 * A method that reads a text file and maps the values, constructing a music patch.
	 * @param inputStream The input stream of a loaded .txt file resource to be read.
	 * @param soundBankVersion The name of the sound bank we are using.
	 */
	public MusicPatch(InputStream inputStream, String soundBankVersion) {

		this.audioDataSources = new AudioDataSource[128];
		this.pitchOffset = new short[128];
		this.volumeOffset = new byte[128];
		this.panOffset = new byte[128];
		this.musicPatchEnvelopes = new MusicPatchEnvelope[128];
		this.loopOffset = new byte[128];

		this.availableSources = new HashMap<>();

		List<String> lines = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.toList());
		for (String line : lines) {
			String[] values = line.split("_");
			if (values[0].equals(values[1])) {
				int index = Integer.parseInt(values[0]);
				if (!availableSources.containsValue(values[2]) && !values[2].equals("-1")) {
					this.audioDataSources[index] = new AudioDataSource(values[2], soundBankVersion);
					availableSources.put(this.audioDataSources[index], values[2]);
				} else {
					for (Map.Entry<AudioDataSource, String> entry : availableSources.entrySet()) {
						if (entry.getValue().equals(values[2])) {
							this.audioDataSources[index] = entry.getKey();
						}
					}
				}
				this.pitchOffset[index] = (short) ((((Integer.parseInt(values[3]) * 256)) + Integer.parseInt(values[4])) - Short.MIN_VALUE);
				this.volume = Integer.parseInt(values[5]);
				this.volumeOffset[index] = (byte) Integer.parseInt(values[6]);
				this.panOffset[index] = (byte) Integer.parseInt(values[7]);

				this.musicPatchEnvelopes[index] = new MusicPatchEnvelope();

				this.musicPatchEnvelopes[index].attack = Integer.parseInt(values[8]);
				this.musicPatchEnvelopes[index].decay = Integer.parseInt(values[9]);
				this.musicPatchEnvelopes[index].release = Integer.parseInt(values[10]);
				this.musicPatchEnvelopes[index].sustain = Integer.parseInt(values[11]);
				this.musicPatchEnvelopes[index].vibratoPitchModulatorCents = Integer.parseInt(values[12]);
				this.musicPatchEnvelopes[index].vibratoFrequencyHertz = Integer.parseInt(values[13]);
				this.musicPatchEnvelopes[index].vibratoDelayMilliseconds = Integer.parseInt(values[14]);

				String[] envelope0 = values[15].replace("[", "").replace("]", "").replace(" ", "").split(",");
				String[] envelope1 = values[16].replace("[", "").replace("]", "").replace(" ", "").split(",");

				this.musicPatchEnvelopes[index].array0 = new byte[envelope0.length];
				this.musicPatchEnvelopes[index].array1 = new byte[envelope1.length];

				for (int stringIndex = 0; stringIndex < envelope0.length; stringIndex++) {
					if (!envelope0[stringIndex].equals("null")) {
						this.musicPatchEnvelopes[index].array0[stringIndex] = Byte.parseByte(envelope0[stringIndex]);
					} else {
						this.musicPatchEnvelopes[index].array0 = null;
					}
				}

				for (int stringIndex = 0; stringIndex < envelope1.length; stringIndex++) {
					if (!envelope1[stringIndex].equals("null")) {
						this.musicPatchEnvelopes[index].array1[stringIndex] = Byte.parseByte(envelope1[stringIndex]);
					} else {
						this.musicPatchEnvelopes[index].array1 = null;
					}
				}

				if (this.audioDataSources[index] != null) {
					if (this.audioDataSources[index].isLooping) {
						this.loopOffset[index] = -1;
					}
					else {
						this.loopOffset[index] = 0;
					}
				}
			} else {
				for (int index = Integer.parseInt(values[0]); index < Integer.parseInt(values[1]) + 1; index++) {
					if (!availableSources.containsValue(values[2]) && !values[2].equals("-1")) {
						this.audioDataSources[index] = new AudioDataSource(values[2], soundBankVersion);
						availableSources.put(this.audioDataSources[index], values[2]);
					} else {
						for (Map.Entry<AudioDataSource, String> entry : availableSources.entrySet()) {
							if (entry.getValue().equals(values[2])) {
								this.audioDataSources[index] = entry.getKey();
							}
						}
					}
					this.pitchOffset[index] = (short) ((((Integer.parseInt(values[3]) * 256)) + Integer.parseInt(values[4])) - Short.MIN_VALUE);
					this.volume = Integer.parseInt(values[5]);
					this.volumeOffset[index] = (byte) Integer.parseInt(values[6]);
					this.panOffset[index] = (byte) Integer.parseInt(values[7]);

					this.musicPatchEnvelopes[index] = new MusicPatchEnvelope();

					this.musicPatchEnvelopes[index].attack = Integer.parseInt(values[8]);
					this.musicPatchEnvelopes[index].decay = Integer.parseInt(values[9]);
					this.musicPatchEnvelopes[index].release = Integer.parseInt(values[10]);
					this.musicPatchEnvelopes[index].sustain = Integer.parseInt(values[11]);
					this.musicPatchEnvelopes[index].vibratoPitchModulatorCents = Integer.parseInt(values[12]);
					this.musicPatchEnvelopes[index].vibratoFrequencyHertz = Integer.parseInt(values[13]);
					this.musicPatchEnvelopes[index].vibratoDelayMilliseconds = Integer.parseInt(values[14]);

					String[] envelope0 = values[15].replace("[", "").replace("]", "").replace(" ", "").split(",");
					String[] envelope1 = values[16].replace("[", "").replace("]", "").replace(" ", "").split(",");

					this.musicPatchEnvelopes[index].array0 = new byte[envelope0.length];
					this.musicPatchEnvelopes[index].array1 = new byte[envelope1.length];

					for (int stringIndex = 0; stringIndex < envelope0.length; stringIndex++) {
						if (!envelope0[stringIndex].equals("null")) {
							this.musicPatchEnvelopes[index].array0[stringIndex] = Byte.parseByte(envelope0[stringIndex]);
						} else {
							this.musicPatchEnvelopes[index].array0 = null;
						}
					}

					for (int stringIndex = 0; stringIndex < envelope1.length; stringIndex++) {
						if (!envelope1[stringIndex].equals("null")) {
							this.musicPatchEnvelopes[index].array1[stringIndex] = Byte.parseByte(envelope1[stringIndex]);
						} else {
							this.musicPatchEnvelopes[index].array1 = null;
						}
					}

					if (this.audioDataSources[index] != null) {
						if (this.audioDataSources[index].isLooping) {
							this.loopOffset[index] = -1;
						}
						else {
							this.loopOffset[index] = 1;
						}
					}
				}
			}
		}
	}

}
