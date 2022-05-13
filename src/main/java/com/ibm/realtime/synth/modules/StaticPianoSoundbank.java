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
package com.ibm.realtime.synth.modules;

import com.ibm.realtime.synth.engine.*;

import javax.sound.sampled.*;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.ibm.realtime.synth.utils.AudioUtils.*;
import static com.ibm.realtime.synth.utils.Debug.debug;
import static com.ibm.realtime.synth.utils.Debug.debugNoNewLine;

/**
 * A very simple "soundbank" implementation based on a set of wave files. All
 * wave files must have the same audio parameters.
 *
 * @author florian
 *
 */
public class StaticPianoSoundbank implements Soundbank {

	public static boolean DEBUG_STATICSB = true;

	/**
	 * All wave files will be truncated to this length
	 */
	public static long MAX_WAVE_LENGTH = 1000; // milliseconds

	private StaticPatch[] patches = new StaticPatch[128];

	public StaticPianoSoundbank() {
		// nothing to do
	}

	public String getName() {
		return "Old Lady Static Soundbank";
	}

	public StaticPianoSoundbank(File directory, FilenameScheme getFilename) {
		this();
		load(directory, getFilename);
	}

	public NoteInput createNoteInput(Synthesizer.Params params, AudioTime time,
			MidiChannel channel, int note, int vel) {

		StaticPatch patch = patches[note];
		if (patch != null) {
			return new NoteInput(params, time, channel, patch,
					new StaticOscillator(patch.getFormat(),
							patch.getAudioData()), new StaticArticulation(time,
							patch, channel), note, vel);
		}
		return null;
	}

	public AudioFormat getPreferredAudioFormat() {
		for (StaticPatch patch : patches) {
			if (patch != null && patch.getFormat() != null) {
				return patch.getFormat();
			}
		}
		assert (false);
		return null;
	}

	public void load(File directory, FilenameScheme getFilename) {
		for (int note = 0; note <= 127; note++) {
			String filename = getFilename.getFilename(note, 127);
			File file = new File(directory, filename);
			patches[note] = new StaticPatch(file, note);
		}
		int replacements = 0;
		if (DEBUG_STATICSB) {
			debug("Note sample table replacements:");
		}
		// no go through all the patches and correct the ones without a waveform
		for (int note = 0; note <= 127; note++) {
			if (patches[note].getAudioData() == null) {
				// find a suitable other sample to be used
				// prefer lower adjacent samples, because upsampling is usually
				// of higher quality
				for (int t = 1; t < 127; t++) {
					int use = -1;
					if (note - (t * 2) + 1 >= 0
							&& !patches[note - (t * 2) + 1].sampleNeedsSamplerateConversion()) {
						use = note - (t * 2) + 1;
					} else if (note - (t * 2) >= 0
							&& !patches[note - (t * 2)].sampleNeedsSamplerateConversion()) {
						use = note - (t * 2);
					} else if (note + t < 128
							&& !patches[note + t].sampleNeedsSamplerateConversion()) {
						use = note + t;
					}
					if (use >= 0) {
						patches[note].useSampleFromPatch(patches[use]);
						if (DEBUG_STATICSB) {
							debugNoNewLine("" + note + " uses " + use + ", ");
							if ((replacements++) % 10 == 9) {
								debug("");
							}
						}
						break;
					}
				}
			}
		}
		if (DEBUG_STATICSB) {
			if ((replacements++) % 10 != 9) {
				debug("");
			}
			if (replacements == 0) {
				debug("(no replacements)");
			}
		}
	}

	public List<Bank> getBanks() {
		List<Bank> list = new ArrayList<Bank>();
		Instrument inst = new Instrument() {
			public int getMidiNumber() {
				return 0;
			}

			public String getName() {
				return "Old Lady Piano";
			}
		};
		final List<Instrument> instList = new ArrayList<Instrument>();
		instList.add(inst);
		list.add(new Bank() {
			public int getMidiNumber() {
				return 0;
			}

			public List<Instrument> getInstruments() {
				return instList;
			}
		});
		return list;
	}

	private static class StaticPatch extends Patch {
		private AudioFormat format;
		private byte[] audioData;
		private int note;

		public StaticPatch(File file, int note) {
			this.note = note;
			if (file.exists()) {
				try {
					loadFile(file);
					// since we managed to load the correct note,
					// the data's note and the to be played note are the
					// same
					this.rootKey = note;
				} catch (IOException ioe) {
					debug(ioe);
				} catch (UnsupportedAudioFileException uafe) {
					debug(uafe);
				}
			}
			bank = 0;
			program = 0;
		}

		private void loadFile(File file) throws IOException,
				UnsupportedAudioFileException {
			debug("Loading " + file);
			AudioFileFormat aff = AudioSystem.getAudioFileFormat(file);
			long frameLength = aff.getFrameLength();
			if (frameLength <= 0) {
				throw new UnsupportedAudioFileException(
						"audio file does not provide length");
			}
			AudioInputStream ais = AudioSystem.getAudioInputStream(file);
			this.format = ais.getFormat();

			// truncate to x seconds
			if (samples2millis(frameLength, (double) format.getSampleRate()) > MAX_WAVE_LENGTH) {
				frameLength =
						millis2samples(MAX_WAVE_LENGTH,
								(double) format.getSampleRate());
			}

			// read the file into the array
			long bytes = frameLength * format.getFrameSize();
			this.audioData = new byte[(int) bytes];
			DataInputStream dis = new DataInputStream(ais);
			dis.readFully(audioData);
		}

		private AudioFormat getFormat() {
			return format;
		}

		/**
		 * @return Returns the audioData.
		 */
		private byte[] getAudioData() {
			return audioData;
		}

		private boolean sampleNeedsSamplerateConversion() {
			return this.note != this.rootKey;
		}

		private void useSampleFromPatch(StaticPatch sp) {
			this.audioData = sp.getAudioData();
			this.rootKey = sp.getRootKey();
			this.format = sp.getFormat();
			// this.volumeFactor = sp.volumeFactor;
		}
	}

	private static class StaticOscillator extends Oscillator {

		/**
		 * Simple Constructor (used for StaticSoundBank)
		 * 
		 * @param nativeFormat
		 * @throws IllegalArgumentException if the nativeFormat is not supported
		 */
		public StaticOscillator(AudioFormat nativeFormat, byte[] nativeSamples) {
			setNativeAudioFormat(nativeFormat);
			this.nativeSamples = nativeSamples;
			this.nativeSamplesStartPos = 0;
			this.nativeSamplesEndPos = nativeSamples.length / nativeSampleSize;
			init();
		}

		protected void convertOneBlock(AudioBuffer buffer, int offset, int count) {
			// only use the left channel
			ConversionTool.byte2doubleGenericLSRC(nativeSamples, 0,
					nativeSampleSize, nativePos, nativePosDelta,
					buffer.getChannel(0), offset, count, nativeFormatCode);

			// old version for stereo feed
			/*
			 * int inByteOffset = 0; int channels = nativeChannels; if (channels >
			 * buffer.getChannelCount()) { channels = buffer.getChannelCount(); }
			 * for (int channel = 0; channel < channels; channel++) {
			 * ConversionTool.byte2doubleGenericLSRC(nativeSamples,
			 * inByteOffset, nativeSampleSize, nativePos, nativePosDelta,
			 * buffer.getChannel(channel), offset, count, nativeFormatCode);
			 * inByteOffset += nativeSampleSize / nativeChannels; }
			 */
		}
	}

	private static class StaticArticulation extends Articulation {

		/**
		 * An envelope generator
		 */
		private Envelope eg;

		public StaticArticulation(AudioTime time, Patch patch,
				MidiChannel channel) {
			init(time, patch, channel);
			eg = new ADSREnvelope(time);
		}

		public void calculate(AudioTime time) {
			eg.calculate(time);
			calcEffectiveVolumeFactor();
		}

		public void process(AudioBuffer buffer) {
			// nothing to do
		}

		protected void calcEffectiveVolumeFactor() {
			for (int i = 0; i < effectiveLinearVolume.length; i++) {
				effectiveLinearVolume[i] = eg.getCurrentValue();
			}
		}

		protected double getRuntimePitchOffset() {
			return 0.0;
		}

		public void release(AudioTime time) {
			eg.release(time);
		}

		public boolean endReached() {
			return eg.endReached();
		}

		public void controlChange(int controller, int value) {
			// nothing to do
		}

		public void pitchWheelChange() {
			// nothing to do
		}
	}
	

	/**
	 * An envelope generator.
	 * 
	 * @author florian
	 */
	public interface Envelope {

		/**
		 * Called after all parameters are gathered for playback of this instrument.
		 * 
		 * @param note the effective key of this voice to be played
		 * @param vel the effective velocity of this voice to be played
		 */
		public void setup(int note, int vel);

		/**
		 * At realtime, calculate the current value of this envelope at the
		 * specified <code>time</code>. The time should increase in a monotonic
		 * fashion.
		 * 
		 * @param time the current time to calculate this envelope's value for
		 */
		public void calculate(AudioTime time);

		/**
		 * This is the current value of the envelope. During the life time of the
		 * envelope, this value starts at 0.0, goes up to a maximum of 1.0 and
		 * eventually goes down to 0.0 again.
		 * 
		 * @return the current linear value of the envelope, [0...1]
		 */
		public double getCurrentValue();

		/**
		 * @return true if this envelope has reached the end of its cycle <i>and</i>
		 *         the current value is at 0.0 (and will under no circumstances go
		 *         up again)
		 */
		public boolean endReached();

		/**
		 * Enter the release segment
		 * 
		 * @param time
		 */
		public void release(AudioTime time);
	}

	/**
	 * An Envelope implementation based on the ADSR model.
	 * <p>
	 * <b>This class is not fully implemented!</b>
	 * 
	 * @author florian
	 * 
	 */
	public static class ADSREnvelope implements Envelope {
		// private final static int SEGMENT_ATTACK = 0;
		// private final static int SEGMENT_DECAY = 1;
		// private final static int SEGMENT_SUSTAIN = 2;
		private final static int SEGMENT_RELEASE = 3;

		/**
		 * at which factor does this envelope declare end of note?
		 */
		private final static double CUT_OFF = 1e-6;

		private int currSegment;

		private long currSegmentStartTime; // in nanos

		// in dB
		private double currDecibelValue;

		// as a factor
		private double currLinearValue;

		public ADSREnvelope(AudioTime time) {
			// TODO: implement correctly
			currDecibelValue = 0.0;
			currLinearValue = decibel2linear(currDecibelValue);
		}

		public void setup(int note, int vel) {
			// nothing to do
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.ibm.realtime.synth.engine.Envelope#calculate(com.ibm.realtime.synth.engine.AudioTime)
		 */
		public void calculate(AudioTime time) {
			if (currSegment == SEGMENT_RELEASE) {
				// for now, every 100 milliseconds, reduce by 10 decibel
				long millisSinceReleaseStart =
						(time.getNanoTime() - currSegmentStartTime) / 1000000L;
				currDecibelValue = -10.0 * millisSinceReleaseStart / 200;
				currLinearValue = decibel2linear(currDecibelValue);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.ibm.realtime.synth.engine.Envelope#getCurrentValue()
		 */
		public double getCurrentValue() {
			return currLinearValue;
		}

		public double getCurrentValueDecibels() {
			return currDecibelValue;
		}

		public boolean endReached() {
			return (currSegment == SEGMENT_RELEASE) && (currLinearValue < CUT_OFF);
		}

		public void release(AudioTime time) {
			if (currSegment < SEGMENT_RELEASE) {
				currSegmentStartTime = time.getNanoTime();
				currSegment = SEGMENT_RELEASE;
			}
		}

	}
	

	public interface FilenameScheme {
		/**
		 * Returns the filename, without directory part, of the file containing
		 * the sample for the specified note.
		 * 
		 * @param note [0..127] the MIDI note number
		 * @return the filename of the specified note.
		 */
		public String getFilename(int note, int vel);
	}

}
