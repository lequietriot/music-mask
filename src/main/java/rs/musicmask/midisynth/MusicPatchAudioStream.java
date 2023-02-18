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

import java.util.ArrayList;

/**
 * A class which synthesizes the MIDI to audio with the Sound Bank patches.
 */
public class MusicPatchAudioStream {

	/**
	 * An array of Sound Bank patch voices.
	 */
	ArrayList<MusicPatchVoice> musicPatchVoices;

	/**
	 * The MIDI stream that this class is using.
	 */
	MidiAudioStream superStream;

	/**
	 * Constructs a new stream from the MIDI stream.
	 * @param midiAudioStream The MIDI stream to be set.
	 */
    MusicPatchAudioStream(MidiAudioStream midiAudioStream) {
		this.superStream = midiAudioStream;
		this.musicPatchVoices = new ArrayList<>();
	}

	/**
	 * A method that fills an array with audio samples.
	 * @param samples An array of integer values to be filled with audio samples.
	 * @param offset An integer representing the offset to start filling samples at.
	 * @param length An integer representing the length of audio to fill samples up to.
	 */
	protected void fill(int[] samples, int offset, int length) {
		if (this.musicPatchVoices.size() != 0) {
			int index = 0;
			while (index < this.musicPatchVoices.size()) {
				MusicPatchVoice musicPatchVoice = this.musicPatchVoices.get(index);
				if (this.superStream.isInactive(musicPatchVoice)) {
					int streamOffset = offset;
					int streamLength = length;
					do {
						if (streamLength <= musicPatchVoice.samplesInMs) {
							this.writeAudio(musicPatchVoice, samples, streamOffset, streamLength, streamLength + streamOffset);
							musicPatchVoice.samplesInMs -= streamLength;
							break;
						}

						this.writeAudio(musicPatchVoice, samples, streamOffset, musicPatchVoice.samplesInMs, streamLength + streamOffset);
						streamOffset += musicPatchVoice.samplesInMs;
						streamLength -= musicPatchVoice.samplesInMs;
					} while (this.superStream.isActive(musicPatchVoice, samples, streamOffset, streamLength));
				}
				index++;
			}
		}

	}

	/**
	 * A method that further handles writing the audio data.
	 * @param musicPatchVoice The current voice.
	 * @param samples The sample array.
	 * @param offset The position to start at.
	 * @param samplesLength The length of the samples.
	 * @param size The size of the audio to be output.
	 */
	void writeAudio(MusicPatchVoice musicPatchVoice, int[] samples, int offset, int samplesLength, int size) {
		if ((this.superStream.switchControls[musicPatchVoice.midiChannel] & 4) != 0 && musicPatchVoice.releasePosition < 0) {
			int effectAmount = this.superStream.reTriggerEffects[musicPatchVoice.midiChannel] / DevicePcmPlayer.sampleRate;

			while (true) {
				int length = (effectAmount + 1048575 - musicPatchVoice.reTriggerAmount) / effectAmount;
				if (length > samplesLength) {
					musicPatchVoice.reTriggerAmount += effectAmount * samplesLength;
					break;
				}

				musicPatchVoice.stream.fill(samples, offset, length);
				offset += length;
				samplesLength -= length;
				musicPatchVoice.reTriggerAmount += effectAmount * length - 1048576;
				int finalAmount = DevicePcmPlayer.sampleRate / 100;
				int amount = 262144 / effectAmount;
				if (amount < finalAmount) {
					finalAmount = amount;
				}

				RawAudioStream rawAudioStream = musicPatchVoice.stream;
				if (this.superStream.sampleLoopControls[musicPatchVoice.midiChannel] == 0) {
					musicPatchVoice.stream = RawAudioStream.createSampledAudioStream(musicPatchVoice.audioDataSource, rawAudioStream.getSampleBasePitch(), rawAudioStream.getSampleVolume(), rawAudioStream.getSamplePanning());
				} else {
					musicPatchVoice.stream = RawAudioStream.createSampledAudioStream(musicPatchVoice.audioDataSource, rawAudioStream.getSampleBasePitch(), 0, rawAudioStream.getSamplePanning());
					this.superStream.modifySampleLoopStart(musicPatchVoice, musicPatchVoice.patch.pitchOffset[musicPatchVoice.midiNote] < 0);
					musicPatchVoice.stream.setDefaultVolume(finalAmount, rawAudioStream.getSampleVolume());
				}

				if (musicPatchVoice.patch.pitchOffset[musicPatchVoice.midiNote] < 0) {
					if (musicPatchVoice.stream != null && musicPatchVoice.audioDataSource.isLooping) {
						musicPatchVoice.stream.setNumLoops(-1);
					}
				}

				rawAudioStream.reset(finalAmount);
				rawAudioStream.fill(samples, offset, size - offset);
			}
		}

		musicPatchVoice.stream.fill(samples, offset, samplesLength);
	}

}
