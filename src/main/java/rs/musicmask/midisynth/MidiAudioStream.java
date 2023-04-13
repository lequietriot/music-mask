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

import rs.musicmask.MusicMaskPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Objects;

/**
 * A class which handles synthesizer methods for the MIDI sequence.
 */
public class MidiAudioStream {

	/**
	 * A table containing the loaded Sound Bank patches with their respective integer ID.
	 */
	Hashtable<Integer, MusicPatch> musicPatches;

	/**
	 * An integer value representing the overall volume for the output audio.
	 */
	int volume;

	/**
	 * An integer value representing the default tempo division amount.
	 */
	int division;

	/**
	 * An array of integers for MIDI Volume values.
	 */
	int[] volumeControls;

	/**
	 * An array of integers for MIDI Pan values.
	 */
	int[] panControls;

	/**
	 * An array of integers for MIDI Expression values.
	 */
	int[] expressionControls;

	/**
	 * An array of integers for MIDI Program Change values.
	 */
	int[] programConstants;

	/**
	 * An array of integers for MIDI Program Change values.
	 */
	int[] patch;

	/**
	 * An array of integers for MIDI Bank Select values.
	 */
	int[] bankControls;

	/**
	 * An array of integers for MIDI Pitch Bend values.
	 */
	int[] pitchBendControls;

	/**
	 * An array of integers for MIDI Modulation values.
	 */
	int[] modulationControls;

	/**
	 * An array of integers for MIDI Portamento time values.
	 */
	int[] portamentoTimeControls;

	/**
	 * An array of integers for MIDI switch on/off values to the exclusive RuneScape effects.
	 */
	int[] switchControls;

	/**
	 * An array of integers for MIDI Data Entry MSB values.
	 */
	int[] dataEntriesMSB;

	/**
	 * An array of integers for MIDI Data Entry LSB values.
	 */
	int[] dataEntriesLSB;

	/**
	 * An array of integers for MIDI Sample Loop Modification values (Exclusive to RuneScape).
	 */
	int[] sampleLoopControls;

	/**
	 * An array of integers for the MIDI ReTrigger Control values (Exclusive to RuneScape).
	 */
	int[] reTriggerControls;

	/**
	 * An array of integers for the MIDI ReTrigger Effect values (Exclusive to RuneScape).
	 */
	int[] reTriggerEffects;

	/**
	 * An array of Sound Bank patch voices that do not loop.
	 */
	MusicPatchVoice[][] oneShotVoices;

	/**
	 * An array of Sound Bank patch voices that do loop.
	 */
	MusicPatchVoice[][] continuousVoices;

	/**
	 * A long value representing the length of audio expressed in microseconds.
	 */
	long microsecondLength;

	/**
	 * A long value representing the current position in the audio, expressed in microseconds.
	 */
	long microsecondPosition;

	/**
	 * Another stream that aids in synthesizing the music, mixing all the active voices.
	 */
	MusicPatchAudioStream patchStream;

	/**
	 * An integer that represents the MIDI file resolution.
	 */
	int resolution;

	/**
	 * A string value to determine what custom sound bank we are using.
	 * Classic = RuneScape 2 sounds
	 * Old School = Old School RuneScape sounds
	 * High Detail = RuneScape: High Detail sounds
	 * Custom = Use your own!
	 */
	public String soundBankVersion;

	/**
	 * Constructs a new MidiAudioStream with default values, loading all music patches as well.
	 */
	public MidiAudioStream(String soundBankName) {
		this.volume = 256;
		this.division = 1000000;
		this.volumeControls = new int[16];
		this.panControls = new int[16];
		this.expressionControls = new int[16];
		this.programConstants = new int[16];
		this.patch = new int[16];
		this.bankControls = new int[16];
		this.pitchBendControls = new int[16];
		this.modulationControls = new int[16];
		this.portamentoTimeControls = new int[16];
		this.switchControls = new int[16];
		this.dataEntriesMSB = new int[16];
		this.dataEntriesLSB = new int[16];
		this.sampleLoopControls = new int[16];
		this.reTriggerControls = new int[16];
		this.reTriggerEffects = new int[16];
		this.oneShotVoices = new MusicPatchVoice[16][128];
		this.continuousVoices = new MusicPatchVoice[16][128];
		this.patchStream = new MusicPatchAudioStream(this);
		this.musicPatches = new Hashtable<>();
		this.soundBankVersion = soundBankName;
		try {
			this.loadMusicPatches();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.systemReset();
	}

	/**
	 * A method to set the default volume level.
	 * @param volumeLevel An integer to represent the volume level.
	 */
	public synchronized void setPcmStreamVolume(int volumeLevel) {
		this.volume = volumeLevel;
	}

	public synchronized int getVolume() {
		return this.volume;
	}

	/**
	 * A method that loads all the music patches.
	 */
	public synchronized void loadMusicPatches() throws IOException {
		for (int key = 0; key < 384; key++) {
			MusicPatch musicPatch = this.musicPatches.get(key);
			if (musicPatch == null) {
				if (MusicMaskPlugin.class.getResourceAsStream(soundBankVersion + "/patches/" + key + ".txt") != null) {
					InputStream inputStream = Objects.requireNonNull(MusicMaskPlugin.class.getResourceAsStream(soundBankVersion + "/patches/" + key + ".txt"));
					if (inputStream != null) {
						musicPatch = new MusicPatch(inputStream, soundBankVersion);
						inputStream.close();
					}

					if (musicPatch == null) {
						continue;
					}

					this.musicPatches.put(key, musicPatch);
				}
			}
		}

	}

	/**
	 * A method that fills the sample array with data.
	 * @param samples The integer array to fill with audio data.
	 * @param length An integer representing the size of audio.
	 */
	protected synchronized void fill(int[] samples, int length) {
		int offset = 0;
		int tempoRate = this.resolution * this.division / DevicePcmPlayer.sampleRate;
		do {
			long microsecondTimeLength = this.microsecondLength + (long) tempoRate * (long) length;
			if (this.microsecondPosition - microsecondTimeLength >= 0L) {
				this.microsecondLength = microsecondTimeLength;
				break;
			}

			int position = (int) ((this.microsecondPosition - this.microsecondLength + (long) tempoRate - 1L) / (long) tempoRate);
			this.microsecondLength += (long) position * (long) tempoRate;
			this.patchStream.fill(samples, offset, position);
			offset += position;
			length -= position;
		} while (true);
		this.patchStream.fill(samples, offset, length);
	}

	/**
	 * A method to set the default patch for a channel, if data does not already exist in the MIDI sequence.
	 * @param channel The MIDI Channel number (0-15).
	 * @param patch An integer representing a Sound Bank Patch ID.
	 */
	public synchronized void setInitialPatch(int channel, int patch) {
		this.setPatch(channel, patch);
	}

	/**
	 * A method to set the patch for a channel.
	 * @param channel The MIDI Channel number (0-15).
	 * @param patch An integer representing a Sound Bank Patch ID.
	 */
	void setPatch(int channel, int patch) {
		this.programConstants[channel] = patch;
		this.bankControls[channel] = patch & -128;
		this.programChange(channel, patch);
	}

	/**
	 * A method to issue a program change event.
	 * @param channel The MIDI Channel number (0-15).
	 * @param program An integer representing the program change value.
	 */
	void programChange(int channel, int program) {
		if (program != this.patch[channel]) {
			this.patch[channel] = program;
			for (int note = 0; note < 128; ++note) {
				this.continuousVoices[channel][note] = null;
			}
		}

	}

	/**
	 * A method to issue a note on event.
	 * @param channel The MIDI Channel number (0-15).
	 * @param data1 The first data value, representing a note pitch.
	 * @param data2 The second data value, representing the velocity of the note.
	 */
	void noteOn(int channel, int data1, int data2) {
		this.noteOff(channel, data1);
		if ((this.switchControls[channel] & 2) != 0) {
			int index = 0;
			for (MusicPatchVoice musicPatchVoice = this.patchStream.musicPatchVoices.get(index); musicPatchVoice != null; musicPatchVoice = this.patchStream.musicPatchVoices.get(index++)) {
				if (musicPatchVoice.midiChannel == channel && musicPatchVoice.releasePosition < 0) {
					this.oneShotVoices[channel][musicPatchVoice.midiNote] = null;
					this.oneShotVoices[channel][data1] = musicPatchVoice;
					int currentPitch = (musicPatchVoice.portamentoOffset * musicPatchVoice.pitchShiftOffset >> 12) + musicPatchVoice.soundTransposition;
					musicPatchVoice.soundTransposition += data1 - musicPatchVoice.midiNote << 8;
					musicPatchVoice.pitchShiftOffset = currentPitch - musicPatchVoice.soundTransposition;
					musicPatchVoice.portamentoOffset = 4096;
					musicPatchVoice.midiNote = data1;
					return;
				}
			}
		}

		MusicPatch musicPatch = this.musicPatches.get(this.patch[channel]);
		if (musicPatch != null) {
			AudioDataSource audioDataSource = musicPatch.audioDataSources[data1];
			if (audioDataSource != null) {
				MusicPatchVoice musicPatchVoice = new MusicPatchVoice();
				musicPatchVoice.midiChannel = channel;
				musicPatchVoice.patch = musicPatch;
				musicPatchVoice.audioDataSource = audioDataSource;
				musicPatchVoice.musicPatchEnvelope = musicPatch.musicPatchEnvelopes[data1];
				musicPatchVoice.loopType = musicPatch.loopOffset[data1];
				musicPatchVoice.midiNote = data1;
				musicPatchVoice.midiNoteVolume = data2 * data2 * musicPatch.volumeOffset[data1] * musicPatch.volume + 1024 >> 11;
				musicPatchVoice.midiNotePan = musicPatch.panOffset[data1] & 255;
				musicPatchVoice.soundTransposition = (data1 << 8) - (musicPatch.pitchOffset[data1] & 32767);
				musicPatchVoice.decayEnvelopePosition = 0;
				musicPatchVoice.attackEnvelopePosition = 0;
				musicPatchVoice.positionOffset = 0;
				musicPatchVoice.releasePosition = -1;
				musicPatchVoice.releaseOffset = 0;
				if (this.sampleLoopControls[channel] == 0) {
					musicPatchVoice.stream = RawAudioStream.createSampledAudioStream(audioDataSource, this.calculatePitch(musicPatchVoice), this.calculateVolume(musicPatchVoice), this.calculatePanning(musicPatchVoice));
				} else {
					musicPatchVoice.stream = RawAudioStream.createSampledAudioStream(audioDataSource, this.calculatePitch(musicPatchVoice), 0, this.calculatePanning(musicPatchVoice));
					this.modifySampleLoopStart(musicPatchVoice, musicPatch.pitchOffset[data1] < 0);
				}

				if (musicPatch.pitchOffset[data1] < 0) {
					if (musicPatchVoice.stream != null && musicPatchVoice.audioDataSource.isLooping) {
						musicPatchVoice.stream.setNumLoops(-1);
					}
				}

				if (musicPatchVoice.loopType >= 0) {
					MusicPatchVoice loopedVoice = this.continuousVoices[channel][musicPatchVoice.loopType];
					if (loopedVoice != null && loopedVoice.releasePosition < 0) {
						this.oneShotVoices[channel][loopedVoice.midiNote] = null;
						loopedVoice.releasePosition = 0;
					}

					this.continuousVoices[channel][musicPatchVoice.loopType] = musicPatchVoice;
				}

				this.patchStream.musicPatchVoices.add(musicPatchVoice);
				this.oneShotVoices[channel][data1] = musicPatchVoice;
			}
		}
	}

	/**
	 * A method to modify the sample loop, a special effect exclusive to RuneScape.
	 * @param musicPatchVoice The synthesized sound, also called a voice.
	 * @param validPitch True if the sound is a valid note (0-127), otherwise false.
	 */
	void modifySampleLoopStart(MusicPatchVoice musicPatchVoice, boolean validPitch) {
		int audioDataLength = musicPatchVoice.audioDataSource.audioData.length;
		int newLoopStart;
		if (validPitch && musicPatchVoice.audioDataSource.isLooping) {
			int newLoopStartPosition = audioDataLength + audioDataLength - musicPatchVoice.audioDataSource.loopStart;
			newLoopStart = (int) ((long) this.sampleLoopControls[musicPatchVoice.midiChannel] * (long) newLoopStartPosition >> 6);
			audioDataLength <<= 8;
			if (newLoopStart >= audioDataLength) {
				newLoopStart = audioDataLength + audioDataLength - 1 - newLoopStart;
				musicPatchVoice.stream.processSamplePitch();
			}
		} else {
			newLoopStart = (int) ((long) audioDataLength * (long) this.sampleLoopControls[musicPatchVoice.midiChannel] >> 6);
		}

		musicPatchVoice.stream.setNewLoopStartPosition(newLoopStart);
	}

	/**
	 * A method to issue a note off event.
	 * @param channel The MIDI Channel number (0-15).
	 * @param data1 The first data value, representing a note pitch.
	 */
	void noteOff(int channel, int data1) {
		MusicPatchVoice musicPatchVoice = this.oneShotVoices[channel][data1];
		if (musicPatchVoice != null) {
			this.oneShotVoices[channel][data1] = null;
			if ((this.switchControls[channel] & 2) != 0) {
				int index = 0;
				for (MusicPatchVoice patchVoice = this.patchStream.musicPatchVoices.get(index); patchVoice != null; patchVoice = this.patchStream.musicPatchVoices.get(index++)) {
					if (musicPatchVoice.midiChannel == patchVoice.midiChannel && patchVoice.releasePosition < 0 && musicPatchVoice != patchVoice) {
						musicPatchVoice.releasePosition = 0;
						break;
					}
				}
			} else {
				musicPatchVoice.releasePosition = 0;
			}

		}
	}

	/**
	 * A method to issue a pitch bend event.
	 * @param channel The MIDI Channel number (0-15).
	 * @param data The data value, calculated by (data1 + data2) * 128.
	 */
	void pitchBend(int channel, int data) {
		this.pitchBendControls[channel] = data;
	}

	/**
	 * A method to issue an all sound off event.
	 * @param channel The MIDI Channel number (0-15).
	 */
	void allSoundOff(int channel) {
		if (this.patchStream.musicPatchVoices.size() != 0) {
			int index = 0;
			for (MusicPatchVoice musicPatchVoice = this.patchStream.musicPatchVoices.get(index); musicPatchVoice != null; musicPatchVoice = this.patchStream.musicPatchVoices.get(index++)) {
				if (channel < 0 || musicPatchVoice.midiChannel == channel) {
					if (musicPatchVoice.stream != null) {
						musicPatchVoice.stream.reset(DevicePcmPlayer.sampleRate / 100);
						musicPatchVoice.reset();
					}

					if (musicPatchVoice.releasePosition < 0) {
						this.oneShotVoices[musicPatchVoice.midiChannel][musicPatchVoice.midiNote] = null;
					}

					musicPatchVoice.reset();
				}
			}
		}

	}

	/**
	 * A method to issue a reset all controllers' event.
	 * @param channel The MIDI Channel number (0-15).
	 */
	void resetAllControllers(int channel) {
		if (channel >= 0) {
			this.volumeControls[channel] = 12800;
			this.panControls[channel] = 8192;
			this.expressionControls[channel] = 16383;
			this.pitchBendControls[channel] = 8192;
			this.modulationControls[channel] = 0;
			this.portamentoTimeControls[channel] = 8192;
			this.setPortamentoSwitch(channel);
			this.setReTriggerSwitch(channel);
			this.switchControls[channel] = 0;
			this.dataEntriesMSB[channel] = 32767;
			this.dataEntriesLSB[channel] = 256;
			this.sampleLoopControls[channel] = 0;
			this.reTrigger(channel, 8192);
		} else {
			for (channel = 0; channel < 16; ++channel) {
				this.resetAllControllers(channel);
			}

		}
	}

	/**
	 * A method to issue a system reset event.
	 */
	public void systemReset() {
		this.allSoundOff(-1);
		this.resetAllControllers(-1);

		int channel;
		for (channel = 0; channel < 16; ++channel) {
			this.patch[channel] = this.programConstants[channel];
		}

		for (channel = 0; channel < 16; ++channel) {
			this.bankControls[channel] = this.programConstants[channel] & -128;
		}

	}

	/**
	 * A method to set the Portamento switch on/off.
	 * @param channel The MIDI Channel number (0-15).
	 */
	void setPortamentoSwitch(int channel) {
		if ((this.switchControls[channel] & 2) != 0) {
			int index = 0;
			for (MusicPatchVoice musicPatchVoice = this.patchStream.musicPatchVoices.get(index); musicPatchVoice != null; musicPatchVoice = this.patchStream.musicPatchVoices.get(index++)) {
				if (musicPatchVoice.midiChannel == channel && this.oneShotVoices[channel][musicPatchVoice.midiNote] == null && musicPatchVoice.releasePosition < 0) {
					musicPatchVoice.releasePosition = 0;
				}
			}
		}

	}

	/**
	 * A method to set the ReTrigger Effect switch on/off.
	 * @param channel The MIDI Channel number (0-15).
	 */
	void setReTriggerSwitch(int channel) {
		if ((this.switchControls[channel] & 4) != 0) {
			int index = 0;
			for (MusicPatchVoice musicPatchVoice = this.patchStream.musicPatchVoices.get(index); musicPatchVoice != null; musicPatchVoice = this.patchStream.musicPatchVoices.get(index++)) {
				if (musicPatchVoice.midiChannel == channel) {
					musicPatchVoice.reTriggerAmount = 0;
				}
			}
		}

	}

	/**
	 * A method to set the ReTrigger Effect values.
	 * @param channel The MIDI Channel number (0-15).
	 * @param data The data value.
	 */
	void reTrigger(int channel, int data) {
		this.reTriggerControls[channel] = data;
		this.reTriggerEffects[channel] = (int) (2097152.0D * Math.pow(2.0D, 5.4931640625E-4D * (double) data) + 0.5D);
	}

	/**
	 * A method used to calculate pitch.
	 * @param musicPatchVoice The synthesized sound, also called a voice.
	 */
	int calculatePitch(MusicPatchVoice musicPatchVoice) {
		int shiftAmount = (musicPatchVoice.portamentoOffset * musicPatchVoice.pitchShiftOffset >> 12) + musicPatchVoice.soundTransposition;
		shiftAmount += (this.pitchBendControls[musicPatchVoice.midiChannel] - 8192) * this.dataEntriesLSB[musicPatchVoice.midiChannel] >> 12;
		MusicPatchEnvelope musicPatchEnvelope = musicPatchVoice.musicPatchEnvelope;
		int pitch;
		if (musicPatchEnvelope.vibratoFrequencyHertz > 0 && (musicPatchEnvelope.vibratoPitchModulatorCents > 0 || this.modulationControls[musicPatchVoice.midiChannel] > 0)) {
			pitch = musicPatchEnvelope.vibratoPitchModulatorCents << 2;
			int vibratoDelay = musicPatchEnvelope.vibratoDelayMilliseconds << 1;
			if (musicPatchVoice.delayOffset < vibratoDelay) {
				pitch = pitch * musicPatchVoice.delayOffset / vibratoDelay;
			}

			pitch += this.modulationControls[musicPatchVoice.midiChannel] >> 7;
			double frequency = Math.sin(0.01227184630308513D * (double)(musicPatchVoice.frequencyOffset & 511));
			shiftAmount += (int) (frequency * (double) pitch);
		}

		pitch = (int) ((double) (musicPatchVoice.audioDataSource.sampleRate * 256) * Math.pow(2.0D, (double) shiftAmount * 3.255208333333333E-4D) / (double) DevicePcmPlayer.sampleRate + 0.5D);
		return Math.max(pitch, 1);
	}

	/**
	 * A method used to calculate volume.
	 * @param musicPatchVoice The synthesized sound, also called a voice.
	 */
	int calculateVolume(MusicPatchVoice musicPatchVoice) {
		MusicPatchEnvelope musicPatchEnvelope = musicPatchVoice.musicPatchEnvelope;
		int overallVolume = this.expressionControls[musicPatchVoice.midiChannel] * this.volumeControls[musicPatchVoice.midiChannel] + 4096 >> 13;
		overallVolume = overallVolume * overallVolume + 16384 >> 15;
		overallVolume = overallVolume * musicPatchVoice.midiNoteVolume + 16384 >> 15;
		overallVolume = overallVolume * this.volume + 128 >> 8;
		if (musicPatchEnvelope.decay > 0) {
			overallVolume = (int)((double) overallVolume * Math.pow(0.5D, (double) musicPatchEnvelope.decay * (double) musicPatchVoice.decayEnvelopePosition * 1.953125E-5D) + 0.5D);
		}

		int position;
		int offset;
		int currentValue;
		int nextValue;
		if (musicPatchEnvelope.array0 != null) {
			position = musicPatchVoice.attackEnvelopePosition;
			offset = musicPatchEnvelope.array0[musicPatchVoice.positionOffset + 1];
			if (musicPatchVoice.positionOffset < musicPatchEnvelope.array0.length - 2) {
				currentValue = (musicPatchEnvelope.array0[musicPatchVoice.positionOffset] & 255) << 8;
				nextValue = (musicPatchEnvelope.array0[musicPatchVoice.positionOffset + 2] & 255) << 8;
				offset += (position - currentValue) * (musicPatchEnvelope.array0[musicPatchVoice.positionOffset + 3] - offset) / (nextValue - currentValue);
			}

			overallVolume = overallVolume * offset + 32 >> 6;
		}

		if (musicPatchVoice.releasePosition > 0 && musicPatchEnvelope.array1 != null) {
			position = musicPatchVoice.releasePosition;
			offset = musicPatchEnvelope.array1[musicPatchVoice.releaseOffset + 1];
			if (musicPatchVoice.releaseOffset < musicPatchEnvelope.array1.length - 2) {
				currentValue = (musicPatchEnvelope.array1[musicPatchVoice.releaseOffset] & 255) << 8;
				nextValue = (musicPatchEnvelope.array1[musicPatchVoice.releaseOffset + 2] & 255) << 8;
				offset += (musicPatchEnvelope.array1[musicPatchVoice.releaseOffset + 3] - offset) * (position - currentValue) / (nextValue - currentValue);
			}

			overallVolume = offset * overallVolume + 32 >> 6;
		}

		return overallVolume;
	}

	/**
	 * A method used to calculate panning.
	 * @param musicPatchVoice The synthesized sound, also called a voice.
	 */
	int calculatePanning(MusicPatchVoice musicPatchVoice) {
		int panValue = this.panControls[musicPatchVoice.midiChannel];
		return panValue < 8192 ? panValue * musicPatchVoice.midiNotePan + 32 >> 6 : 16384 - ((128 - musicPatchVoice.midiNotePan) * (16384 - panValue) + 32 >> 6);
	}

	/**
	 * A method used to determine whether the voice is inactive or not.
	 * @param musicPatchVoice The synthesized sound, also called a voice.
	 */
	boolean isInactive(MusicPatchVoice musicPatchVoice) {
		if (musicPatchVoice.stream == null) {
			if (musicPatchVoice.releasePosition >= 0) {
				musicPatchVoice.reset();
				if (musicPatchVoice.loopType > 0 && musicPatchVoice == this.continuousVoices[musicPatchVoice.midiChannel][musicPatchVoice.loopType]) {
					this.continuousVoices[musicPatchVoice.midiChannel][musicPatchVoice.loopType] = null;
				}
			}
			return false;
		} else {
			return true;
		}
	}

	/**
	 * A method used to determine whether the voice is active or not.
	 * @param musicPatchVoice The synthesized sound, also called a voice.
	 */
	boolean isActive(MusicPatchVoice musicPatchVoice, int[] samples, int offset, int length) {
		musicPatchVoice.samplesInMs = DevicePcmPlayer.sampleRate / 100;
		if (musicPatchVoice.releasePosition < 0 || musicPatchVoice.stream != null && !musicPatchVoice.stream.isLoopValid()) {
			int slideAmount = musicPatchVoice.portamentoOffset;
			if (slideAmount > 0) {
				slideAmount -= (int) (16.0D * Math.pow(2.0D, 4.921259842519685E-4D * (double) this.portamentoTimeControls[musicPatchVoice.midiChannel]) + 0.5D);
				if (slideAmount < 0) {
					slideAmount = 0;
				}

				musicPatchVoice.portamentoOffset = slideAmount;
			}

			musicPatchVoice.stream.setSampleBasePitch(this.calculatePitch(musicPatchVoice));
			MusicPatchEnvelope musicPatchEnvelope = musicPatchVoice.musicPatchEnvelope;
			boolean reachedEndOfArray = false;
			++musicPatchVoice.delayOffset;
			musicPatchVoice.frequencyOffset += musicPatchEnvelope.vibratoFrequencyHertz;
			double pitch = 5.086263020833333E-6D * (double) ((musicPatchVoice.midiNote - 60 << 8) + (musicPatchVoice.pitchShiftOffset * musicPatchVoice.portamentoOffset >> 12));
			if (musicPatchEnvelope.decay > 0) {
				if (musicPatchEnvelope.sustain > 0) {
					musicPatchVoice.decayEnvelopePosition += (int) (128.0D * Math.pow(2.0D, (double) musicPatchEnvelope.sustain * pitch) + 0.5D);
				} else {
					musicPatchVoice.decayEnvelopePosition += 128;
				}
			}

			if (musicPatchEnvelope.array0 != null) {
				if (musicPatchEnvelope.attack > 0) {
					musicPatchVoice.attackEnvelopePosition += (int) (128.0D * Math.pow(2.0D, pitch * (double) musicPatchEnvelope.attack) + 0.5D);
				} else {
					musicPatchVoice.attackEnvelopePosition += 128;
				}

				while (musicPatchVoice.positionOffset < musicPatchEnvelope.array0.length - 2 && musicPatchVoice.attackEnvelopePosition > (musicPatchEnvelope.array0[musicPatchVoice.positionOffset + 2] & 255) << 8) {
					musicPatchVoice.positionOffset += 2;
				}

				if (musicPatchEnvelope.array0.length - 2 == musicPatchVoice.positionOffset && musicPatchEnvelope.array0[musicPatchVoice.positionOffset + 1] == 0) {
					reachedEndOfArray = true;
				}
			}

			if (musicPatchVoice.releasePosition >= 0 && musicPatchEnvelope.array1 != null && (this.switchControls[musicPatchVoice.midiChannel] & 1) == 0 && (musicPatchVoice.loopType < 0 || musicPatchVoice != this.continuousVoices[musicPatchVoice.midiChannel][musicPatchVoice.loopType])) {
				if (musicPatchEnvelope.release > 0) {
					musicPatchVoice.releasePosition += (int) (128.0D * Math.pow(2.0D, pitch * (double) musicPatchEnvelope.release) + 0.5D);
				} else {
					musicPatchVoice.releasePosition += 128;
				}

				while (musicPatchVoice.releaseOffset < musicPatchEnvelope.array1.length - 2 && musicPatchVoice.releasePosition > (musicPatchEnvelope.array1[musicPatchVoice.releaseOffset + 2] & 255) << 8) {
					musicPatchVoice.releaseOffset += 2;
				}

				if (musicPatchEnvelope.array1.length - 2 == musicPatchVoice.releaseOffset) {
					reachedEndOfArray = true;
				}
			}

			if (reachedEndOfArray) {
				musicPatchVoice.stream.reset(musicPatchVoice.samplesInMs);
				if (samples != null) {
					musicPatchVoice.stream.fill(samples, offset, length);
				}

				musicPatchVoice.reset();
				if (musicPatchVoice.releasePosition >= 0) {
					musicPatchVoice.reset();
					if (musicPatchVoice.loopType > 0 && musicPatchVoice == this.continuousVoices[musicPatchVoice.midiChannel][musicPatchVoice.loopType]) {
						this.continuousVoices[musicPatchVoice.midiChannel][musicPatchVoice.loopType] = null;
					}
				}
				return false;
			} else {
				musicPatchVoice.stream.setDefaultVolumeAndPanning(musicPatchVoice.samplesInMs, this.calculateVolume(musicPatchVoice), this.calculatePanning(musicPatchVoice));
				return true;
			}
		} else {
			musicPatchVoice.reset();
			if (musicPatchVoice.loopType > 0 && musicPatchVoice == this.continuousVoices[musicPatchVoice.midiChannel][musicPatchVoice.loopType]) {
				this.continuousVoices[musicPatchVoice.midiChannel][musicPatchVoice.loopType] = null;
			}
			return false;
		}
	}

}