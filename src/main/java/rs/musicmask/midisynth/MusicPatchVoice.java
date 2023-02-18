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

/**
 * A class that represents a single voice in the music, essentially all the characteristics of a note being processed.
 */
public class MusicPatchVoice {

	/**
	 * An integer value for the current MIDI channel number (0-15).
	 */
	int midiChannel;

	/**
	 * An envelope for the current Music Patch.
	 */
	MusicPatchEnvelope musicPatchEnvelope;

	/**
	 * The current Music Patch.
	 */
	MusicPatch patch;

	/**
	 * An Audio source.
	 */
	AudioDataSource audioDataSource;

	/**
	 * An integer value containing the loop type for the sample.
	 */
	int loopType;

	/**
	 * An integer value containing the value of the key pitch (0-127).
	 */
	int midiNote;

	/**
	 * An integer value representing the overall calculated volume of this sample.
	 */
	int midiNoteVolume;

	/**
	 * An integer value representing the overall calculated panning of this sample.
	 */
	int midiNotePan;

	/**
	 * An integer value representing the overall distance of this sample from the base pitch.
	 */
	int soundTransposition;

	int pitchShiftOffset;

	int portamentoOffset;

	int decayEnvelopePosition;

	int attackEnvelopePosition;

	int positionOffset;

	int releasePosition;

	int releaseOffset;

	int delayOffset;

	int frequencyOffset;

	/**
	 * The raw audio stream.
	 */
	RawAudioStream stream;

	int samplesInMs;

	int reTriggerAmount;

	/**
	 * A method that nullifies the main variables of this class.
	 */
	void reset() {
		this.patch = null;
		this.audioDataSource = null;
		this.musicPatchEnvelope = null;
		this.stream = null;
	}
}