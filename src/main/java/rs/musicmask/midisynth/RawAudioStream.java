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
 * A utility class for computing the characteristics of the sound being written.
 */
public class RawAudioStream {

	/**
	 * The audio sample.
	 */
	AudioDataSource sound;

	/**
	 * An integer value representing a modifier amount for the looping sample.
	 */
	int loopStartModifier;

	/**
	 * An integer value representing the sample's pitch.
	 */
	int samplePitch;

	/**
	 * An integer value representing the sample's volume.
	 */
	int sampleVolume;

	/**
	 * An integer value representing the sample's panning.
	 */
	int samplePan;

	/**
	 * An integer value representing volume.
	 */
	int volume;

	/**
	 * An integer value representing the right channel's volume.
	 */
	int rightChannelVolume;

	/**
	 * An integer value representing the left channel's volume.
	 */
	int leftChannelVolume;

	/**
	 * An integer value representing the number of times a sample will loop.
	 */
	int numLoops;

	/**
	 * An integer value representing the sample's loop start.
	 */
	int start;

	/**
	 * An integer value representing the sample's loop end.
	 */
	int end;

	/**
	 * A boolean value representing the sample's loop status.
	 */
	boolean isLooping;

	/**
	 * An integer value representing the current position in the stream.
	 */
	int streamPosition;

	/**
	 * An integer value representing the master volume of the stream.
	 */
	int overallVolume;

	/**
	 * An integer value representing the overall right channel volume.
	 */
	int overallRightChannel;

	/**
	 * An integer value representing the overall left channel volume.
	 */
	int overallLeftChannel;

	/**
	 * Constructs a new raw audio stream.
	 * @param audioDataSource The audio source.
	 * @param pitch The pitch offset.
	 * @param volume The volume offset.
	 * @param pan The pan offset.
	 */
    RawAudioStream(AudioDataSource audioDataSource, int pitch, int volume, int pan) {
		this.sound = audioDataSource;
		this.start = audioDataSource.loopStart;
		this.end = audioDataSource.loopEnd;
		this.isLooping = audioDataSource.isLooping;
		this.samplePitch = pitch;
		this.sampleVolume = volume;
		this.samplePan = pan;
		this.loopStartModifier = 0;
	}

	/**
	 * A method that fills an array with audio samples.
	 * @param samples An array of integer values to be filled with audio samples.
	 * @param offset An integer representing the offset to start filling samples at.
	 * @param length An integer representing the length of audio to fill samples up to.
	 */
	public synchronized void fill(int[] samples, int offset, int length) {
		if (this.sampleVolume != 0 || this.streamPosition != 0) {
			AudioDataSource audioDataSource = this.sound;
			int loopStart = this.start << 8;
			int loopEnd = this.end << 8;
			int sampleSize = audioDataSource.audioData.length << 8;
			int loopDifference = loopEnd - loopStart;
			if (loopDifference <= 0) {
				this.numLoops = 0;
			}

			int position = offset;
			length += offset;
			if (this.loopStartModifier < 0) {
				if (this.samplePitch <= 0) {
					return;
				}

				this.loopStartModifier = 0;
			}

			if (this.loopStartModifier >= sampleSize) {
				if (this.samplePitch >= 0) {
					return;
				}

				this.loopStartModifier = sampleSize - 1;
			}

			if (this.numLoops < 0) {
				if (this.isLooping) {
					if (this.samplePitch < 0) {
						position = this.calculateBeginningOffset(samples, offset, loopStart, length, audioDataSource.audioData[this.start]);
						if (this.loopStartModifier >= loopStart) {
							return;
						}

						this.loopStartModifier = loopStart + loopStart - 1 - this.loopStartModifier;
						this.samplePitch = -this.samplePitch;
					}

					while (true) {
						position = this.calculateEndingOffset(samples, position, loopEnd, length, audioDataSource.audioData[this.end - 1]);
						if (this.loopStartModifier < loopEnd) {
							return;
						}

						this.loopStartModifier = loopEnd + loopEnd - 1 - this.loopStartModifier;
						this.samplePitch = -this.samplePitch;
						position = this.calculateBeginningOffset(samples, position, loopStart, length, audioDataSource.audioData[this.start]);
						if (this.loopStartModifier >= loopStart) {
							return;
						}

						this.loopStartModifier = loopStart + loopStart - 1 - this.loopStartModifier;
						this.samplePitch = -this.samplePitch;
					}
				} else if (this.samplePitch < 0) {
					while (true) {
						position = this.calculateBeginningOffset(samples, position, loopStart, length, audioDataSource.audioData[this.end - 1]);
						if (this.loopStartModifier >= loopStart) {
							return;
						}

						this.loopStartModifier = loopEnd - 1 - (loopEnd - 1 - this.loopStartModifier) % loopDifference;
					}
				} else {
					while (true) {
						position = this.calculateEndingOffset(samples, position, loopEnd, length, audioDataSource.audioData[this.start]);
						if (this.loopStartModifier < loopEnd) {
							return;
						}

						this.loopStartModifier = loopStart + (this.loopStartModifier - loopStart) % loopDifference;
					}
				}
			} else {
				if (this.numLoops > 0) {
					if (this.isLooping) {
						loopLabel: {
							if (this.samplePitch < 0) {
								position = this.calculateBeginningOffset(samples, offset, loopStart, length, audioDataSource.audioData[this.start]);
								if (this.loopStartModifier >= loopStart) {
									return;
								}

								this.loopStartModifier = loopStart + loopStart - 1 - this.loopStartModifier;
								this.samplePitch = -this.samplePitch;
								if (--this.numLoops == 0) {
									break loopLabel;
								}
							}

							do {
								position = this.calculateEndingOffset(samples, position, loopEnd, length, audioDataSource.audioData[this.end - 1]);
								if (this.loopStartModifier < loopEnd) {
									return;
								}

								this.loopStartModifier = loopEnd + loopEnd - 1 - this.loopStartModifier;
								this.samplePitch = -this.samplePitch;
								if (--this.numLoops == 0) {
									break;
								}

								position = this.calculateBeginningOffset(samples, position, loopStart, length, audioDataSource.audioData[this.start]);
								if (this.loopStartModifier >= loopStart) {
									return;
								}

								this.loopStartModifier = loopStart + loopStart - 1 - this.loopStartModifier;
								this.samplePitch = -this.samplePitch;
							} while(--this.numLoops != 0);
						}
					} else {
						int loopOffset;
						if (this.samplePitch < 0) {
							while (true) {
								position = this.calculateBeginningOffset(samples, position, loopStart, length, audioDataSource.audioData[this.end - 1]);
								if (this.loopStartModifier >= loopStart) {
									return;
								}

								loopOffset = (loopEnd - 1 - this.loopStartModifier) / loopDifference;
								if (loopOffset >= this.numLoops) {
									this.loopStartModifier += loopDifference * this.numLoops;
									this.numLoops = 0;
									break;
								}

								this.loopStartModifier += loopDifference * loopOffset;
								this.numLoops -= loopOffset;
							}
						} else {
							while (true) {
								position = this.calculateEndingOffset(samples, position, loopEnd, length, audioDataSource.audioData[this.start]);
								if (this.loopStartModifier < loopEnd) {
									return;
								}

								loopOffset = (this.loopStartModifier - loopStart) / loopDifference;
								if (loopOffset >= this.numLoops) {
									this.loopStartModifier -= loopDifference * this.numLoops;
									this.numLoops = 0;
									break;
								}

								this.loopStartModifier -= loopDifference * loopOffset;
								this.numLoops -= loopOffset;
							}
						}
					}
				}

				if (this.samplePitch < 0) {
					this.calculateBeginningOffset(samples, position, 0, length, 0);
					if (this.loopStartModifier < 0) {
						this.loopStartModifier = -1;
					}
				} else {
					this.calculateEndingOffset(samples, position, sampleSize, length, 0);
					if (this.loopStartModifier >= sampleSize) {
						this.loopStartModifier = sampleSize;
					}
				}

			}
		}
	}

	public synchronized void setNumLoops(int loopCount) {
		this.numLoops = loopCount;
	}

	synchronized void muteStream() {
		this.mute(0, this.getSamplePanning());
	}

	synchronized void mute(int volume, int panning) {
		this.sampleVolume = volume;
		this.samplePan = panning;
		this.streamPosition = 0;
	}

	public synchronized int getSampleVolume() {
		return this.sampleVolume == Integer.MIN_VALUE ? 0 : this.sampleVolume;
	}

	public synchronized int getSamplePanning() {
		return this.samplePan < 0 ? -1 : this.samplePan;
	}

	public synchronized void setNewLoopStartPosition(int newLoopStart) {
		int sampleDataLength = this.sound.audioData.length << 8;
		if (newLoopStart < -1) {
			newLoopStart = -1;
		}

		if (newLoopStart > sampleDataLength) {
			newLoopStart = sampleDataLength;
		}

		this.loopStartModifier = newLoopStart;
	}

	public synchronized void processSamplePitch() {
		this.samplePitch = (this.samplePitch ^ this.samplePitch >> 31) + (this.samplePitch >>> 31);
		this.samplePitch = -this.samplePitch;
	}

	public synchronized void setDefaultVolume(int value, int volume) {
		this.setDefaultVolumeAndPanning(value, volume, this.getSamplePanning());
	}

	public synchronized void setDefaultVolumeAndPanning(int value, int volume, int pan) {
		if (value == 0) {
			this.mute(volume, pan);
		} else {
			int rightChannel = calculateRightChannel(volume, pan);
			int leftChannel = calculateLeftChannel(volume, pan);
			if (rightChannel == this.rightChannelVolume && leftChannel == this.leftChannelVolume) {
				this.streamPosition = 0;
			} else {
				int overallVolume = volume - this.volume;
				if (this.volume - volume > overallVolume) {
					overallVolume = this.volume - volume;
				}

				if (rightChannel - this.rightChannelVolume > overallVolume) {
					overallVolume = rightChannel - this.rightChannelVolume;
				}

				if (this.rightChannelVolume - rightChannel > overallVolume) {
					overallVolume = this.rightChannelVolume - rightChannel;
				}

				if (leftChannel - this.leftChannelVolume > overallVolume) {
					overallVolume = leftChannel - this.leftChannelVolume;
				}

				if (this.leftChannelVolume - leftChannel > overallVolume) {
					overallVolume = this.leftChannelVolume - leftChannel;
				}

				if (value > overallVolume) {
					value = overallVolume;
				}

				this.streamPosition = value;
				this.sampleVolume = volume;
				this.samplePan = pan;
				this.overallVolume = (volume - this.volume) / value;
				this.overallRightChannel = (rightChannel - this.rightChannelVolume) / value;
				this.overallLeftChannel = (leftChannel - this.leftChannelVolume) / value;
			}
		}
	}

	public synchronized void reset(int value) {
		if (value == 0) {
			this.muteStream();
		} else if (this.rightChannelVolume == 0 && this.leftChannelVolume == 0) {
			this.streamPosition = 0;
			this.sampleVolume = 0;
			this.volume = 0;
		} else {
			int invertedVolume = -this.volume;
			if (this.volume > invertedVolume) {
				invertedVolume = this.volume;
			}

			if (-this.rightChannelVolume > invertedVolume) {
				invertedVolume = -this.rightChannelVolume;
			}

			if (this.rightChannelVolume > invertedVolume) {
				invertedVolume = this.rightChannelVolume;
			}

			if (-this.leftChannelVolume > invertedVolume) {
				invertedVolume = -this.leftChannelVolume;
			}

			if (this.leftChannelVolume > invertedVolume) {
				invertedVolume = this.leftChannelVolume;
			}

			if (value > invertedVolume) {
				value = invertedVolume;
			}

			this.streamPosition = value;
			this.sampleVolume = Integer.MIN_VALUE;
			this.overallVolume = -this.volume / value;
			this.overallRightChannel = -this.rightChannelVolume / value;
			this.overallLeftChannel = -this.leftChannelVolume / value;
		}
	}

	public synchronized void setSampleBasePitch(int pitch) {
		if (this.samplePitch < 0) {
			this.samplePitch = -pitch;
		} else {
			this.samplePitch = pitch;
		}
	}

	public synchronized int getSampleBasePitch() {
		return this.samplePitch < 0 ? -this.samplePitch : this.samplePitch;
	}

	public boolean isLoopValid() {
		return this.loopStartModifier < 0 || this.loopStartModifier >= this.sound.audioData.length << 8;
	}

	int calculateEndingOffset(int[] samples, int offset, int endPosition, int sampleLength, int sampleLoopEnd) {
		while (true) {
			if (this.streamPosition > 0) {
				int positionOffset = offset + this.streamPosition;
				if (positionOffset > sampleLength) {
					positionOffset = sampleLength;
				}

				this.streamPosition += offset;
				if (this.samplePitch == 256 && (this.loopStartModifier & 255) == 0) {
					if (DevicePcmPlayer.stereo) {
						offset = calculateUnmodifiedStereoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, this.overallRightChannel, this.overallLeftChannel, positionOffset, endPosition, this);
					} else {
						offset = calculateUnmodifiedMonoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, this.overallVolume, positionOffset, endPosition, this);
					}
				} else if (DevicePcmPlayer.stereo) {
					offset = calculateModifiedStereoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, this.overallRightChannel, this.overallLeftChannel, positionOffset, endPosition, this, this.samplePitch, sampleLoopEnd);
				} else {
					offset = calculateModifiedMonoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, this.overallVolume, positionOffset, endPosition, this, this.samplePitch, sampleLoopEnd);
				}

				this.streamPosition -= offset;
				if (this.streamPosition != 0) {
					return offset;
				}

				if (this.streamIsNotMuted()) {
					continue;
				}

				return sampleLength;
			}

			if (this.samplePitch == 256 && (this.loopStartModifier & 255) == 0) {
				if (DevicePcmPlayer.stereo) {
					return getUnmodifiedStereoEndOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, sampleLength, endPosition, this);
				}

				return getUnmodifiedMonoEndOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, sampleLength, endPosition, this);
			}

			if (DevicePcmPlayer.stereo) {
				return getModifiedStereoEndOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, sampleLength, endPosition, this, this.samplePitch, sampleLoopEnd);
			}

			return getModifiedMonoEndOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, sampleLength, endPosition, this, this.samplePitch, sampleLoopEnd);
		}
	}

	int calculateBeginningOffset(int[] samples, int offset, int startPosition, int sampleLength, int loopStartPosition) {
		while (true) {
			if (this.streamPosition > 0) {
				int volumeOffset = offset + this.streamPosition;
				if (volumeOffset > sampleLength) {
					volumeOffset = sampleLength;
				}

				this.streamPosition += offset;
				if (this.samplePitch == -256 && (this.loopStartModifier & 255) == 0) {
					if (DevicePcmPlayer.stereo) {
						offset = getUnmodifiedLoopStartStereoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, this.overallRightChannel, this.overallLeftChannel, volumeOffset, startPosition, this);
					} else {
						offset = getUnmodifiedLoopStartMonoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, this.overallVolume, volumeOffset, startPosition, this);
					}
				} else if (DevicePcmPlayer.stereo) {
					offset = getModifiedLoopStartStereoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, this.overallRightChannel, this.overallLeftChannel, volumeOffset, startPosition, this, this.samplePitch, loopStartPosition);
				} else {
					offset = getModifiedLoopStartMonoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, this.overallVolume, volumeOffset, startPosition, this, this.samplePitch, loopStartPosition);
				}

				this.streamPosition -= offset;
				if (this.streamPosition != 0) {
					return offset;
				}

				if (this.streamIsNotMuted()) {
					continue;
				}

				return sampleLength;
			}

			if (this.samplePitch == -256 && (this.loopStartModifier & 255) == 0) {
				if (DevicePcmPlayer.stereo) {
					return getFirstUnmodifiedStereoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, sampleLength, startPosition, this);
				}

				return getFirstUnmodifiedMonoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, sampleLength, startPosition, this);
			}

			if (DevicePcmPlayer.stereo) {
				return getFirstModifiedStereoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, sampleLength, startPosition, this, this.samplePitch, loopStartPosition);
			}

			return getFirstModifiedMonoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, sampleLength, startPosition, this, this.samplePitch, loopStartPosition);
		}
	}

	boolean streamIsNotMuted() {
		int sampleVolume = this.sampleVolume;
		int rightVolume;
		int leftVolume;
		if (sampleVolume == Integer.MIN_VALUE) {
			leftVolume = 0;
			rightVolume = 0;
			sampleVolume = 0;
		} else {
			rightVolume = calculateRightChannel(sampleVolume, this.samplePan);
			leftVolume = calculateLeftChannel(sampleVolume, this.samplePan);
		}

		if (sampleVolume == this.volume && rightVolume == this.rightChannelVolume && leftVolume == this.leftChannelVolume) {
			if (this.sampleVolume == Integer.MIN_VALUE) {
				this.sampleVolume = 0;
				return false;
			} else {
				return true;
			}
		} else {
			if (this.volume < sampleVolume) {
				this.overallVolume = 1;
				this.streamPosition = sampleVolume - this.volume;
			} else if (this.volume > sampleVolume) {
				this.overallVolume = -1;
				this.streamPosition = this.volume - sampleVolume;
			} else {
				this.overallVolume = 0;
			}

			if (this.rightChannelVolume < rightVolume) {
				this.overallRightChannel = 1;
				if (this.streamPosition == 0 || this.streamPosition > rightVolume - this.rightChannelVolume) {
					this.streamPosition = rightVolume - this.rightChannelVolume;
				}
			} else if (this.rightChannelVolume > rightVolume) {
				this.overallRightChannel = -1;
				if (this.streamPosition == 0 || this.streamPosition > this.rightChannelVolume - rightVolume) {
					this.streamPosition = this.rightChannelVolume - rightVolume;
				}
			} else {
				this.overallRightChannel = 0;
			}

			if (this.leftChannelVolume < leftVolume) {
				this.overallLeftChannel = 1;
				if (this.streamPosition == 0 || this.streamPosition > leftVolume - this.leftChannelVolume) {
					this.streamPosition = leftVolume - this.leftChannelVolume;
				}
			} else if (this.leftChannelVolume > leftVolume) {
				this.overallLeftChannel = -1;
				if (this.streamPosition == 0 || this.streamPosition > this.leftChannelVolume - leftVolume) {
					this.streamPosition = this.leftChannelVolume - leftVolume;
				}
			} else {
				this.overallLeftChannel = 0;
			}

			return true;
		}
	}

	static int calculateRightChannel(int volume, int pan) {
		return pan < 0 ? volume : (int) ((double) volume * Math.sqrt((double) (16384 - pan) * 1.220703125E-4D) + 0.5D);
	}

	static int calculateLeftChannel(int volume, int pan) {
		return pan < 0 ? -volume : (int) ((double)volume * Math.sqrt((double) pan * 1.220703125E-4D) + 0.5D);
	}

	public static RawAudioStream createSampledAudioStream(AudioDataSource sound, int pitchFactor, int volumeFactor, int panFactor) {
		return sound.audioData != null && sound.audioData.length != 0 ? new RawAudioStream(sound, pitchFactor, volumeFactor, panFactor) : null;
	}

	static int getUnmodifiedMonoEndOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int sampleLength, int endPosition, RawAudioStream rawAudioStream) {
		loopStartModifier >>= 8;
		endPosition >>= 8;
		volume <<= 2;
		int position;
		if ((position = offset + endPosition - loopStartModifier) > sampleLength) {
			position = sampleLength;
		}

		int index;
		for (position -= 3; offset < position; samples[index] += audioData[loopStartModifier++] * volume) {
			index = offset++;
			samples[index] += audioData[loopStartModifier++] * volume;
			index = offset++;
			samples[index] += audioData[loopStartModifier++] * volume;
			index = offset++;
			samples[index] += audioData[loopStartModifier++] * volume;
			index = offset++;
		}

		for (position += 3; offset < position; samples[index] += audioData[loopStartModifier++] * volume) {
			index = offset++;
		}

		rawAudioStream.loopStartModifier = loopStartModifier << 8;
		return offset;
	}

	static int getUnmodifiedStereoEndOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int length, int endPosition, RawAudioStream rawAudioStream) {
		loopStartModifier >>= 8;
		endPosition >>= 8;
		rightChannelVolume <<= 2;
		leftChannelVolume <<= 2;
		int position;
		if ((position = offset + endPosition - loopStartModifier) > length) {
			position = length;
		}

		offset <<= 1;
		position <<= 1;

		int index;
		byte audioDataByte;
		for (position -= 6; offset < position; samples[index] += audioDataByte * leftChannelVolume) {
			audioDataByte = audioData[loopStartModifier++];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
			audioDataByte = audioData[loopStartModifier++];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
			audioDataByte = audioData[loopStartModifier++];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
			audioDataByte = audioData[loopStartModifier++];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			index = offset++;
		}

		for (position += 6; offset < position; samples[index] += audioDataByte * leftChannelVolume) {
			audioDataByte = audioData[loopStartModifier++];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			index = offset++;
		}

		rawAudioStream.loopStartModifier = loopStartModifier << 8;
		return offset >> 1;
	}

	static int getFirstUnmodifiedMonoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int sampleLength, int startPosition, RawAudioStream rawAudioStream) {
		loopStartModifier >>= 8;
		startPosition >>= 8;
		volume <<= 2;
		int position;
		if ((position = offset + loopStartModifier - (startPosition - 1)) > sampleLength) {
			position = sampleLength;
		}

		int index;
		for (position -= 3; offset < position; samples[index] += audioData[loopStartModifier--] * volume) {
			index = offset++;
			samples[index] += audioData[loopStartModifier--] * volume;
			index = offset++;
			samples[index] += audioData[loopStartModifier--] * volume;
			index = offset++;
			samples[index] += audioData[loopStartModifier--] * volume;
			index = offset++;
		}

		for (position += 3; offset < position; samples[index] += audioData[loopStartModifier--] * volume) {
			index = offset++;
		}

		rawAudioStream.loopStartModifier = loopStartModifier << 8;
		return offset;
	}

	static int getFirstUnmodifiedStereoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int length, int startPosition, RawAudioStream rawAudioStream) {
		loopStartModifier >>= 8;
		startPosition >>= 8;
		rightChannelVolume <<= 2;
		leftChannelVolume <<= 2;
		int position;
		if ((position = loopStartModifier + offset - (startPosition - 1)) > length) {
			position = length;
		}

		offset <<= 1;
		position <<= 1;

		int index;
		byte audioDataByte;
		for (position -= 6; offset < position; samples[index] += audioDataByte * leftChannelVolume) {
			audioDataByte = audioData[loopStartModifier--];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
			audioDataByte = audioData[loopStartModifier--];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
			audioDataByte = audioData[loopStartModifier--];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
			audioDataByte = audioData[loopStartModifier--];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			index = offset++;
		}

		for (position += 6; offset < position; samples[index] += audioDataByte * leftChannelVolume) {
			audioDataByte = audioData[loopStartModifier--];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			index = offset++;
		}

		rawAudioStream.loopStartModifier = loopStartModifier << 8;
		return offset >> 1;
	}

	static int getModifiedMonoEndOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int length, int endPosition, RawAudioStream rawAudioStream, int pitch, int sampleLoopEnd) {
		int sampleLength;
		if (pitch == 0 || (sampleLength = offset + (pitch + (endPosition - loopStartModifier) - 257) / pitch) > length) {
			sampleLength = length;
		}

		byte audioDataByte;
		int position;
		int index;
		while (offset < sampleLength) {
			index = loopStartModifier >> 8;
			audioDataByte = audioData[index];
			position = offset++;
			samples[position] += ((audioDataByte << 8) + (audioData[index + 1] - audioDataByte) * (loopStartModifier & 255)) * volume >> 6;
			loopStartModifier += pitch;
		}

		if (pitch == 0 || (sampleLength = offset + (pitch + (endPosition - loopStartModifier) - 1) / pitch) > length) {
			sampleLength = length;
		}

		for (index = sampleLoopEnd; offset < sampleLength; loopStartModifier += pitch) {
			audioDataByte = audioData[loopStartModifier >> 8];
			position = offset++;
			samples[position] += ((audioDataByte << 8) + (index - audioDataByte) * (loopStartModifier & 255)) * volume >> 6;
		}

		rawAudioStream.loopStartModifier = loopStartModifier;
		return offset;
	}

	static int getModifiedStereoEndOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int sampleLength, int endPosition, RawAudioStream rawAudioStream, int pitch, int sampleLoopEnd) {
		int length;
		if (pitch == 0 || (length = offset + (endPosition - loopStartModifier + pitch - 257) / pitch) > sampleLength) {
			length = sampleLength;
		}

		offset <<= 1;

		byte audioDataByte;
		int index;
		int audioLoopOffset;
		int position;
		for (length <<= 1; offset < length; loopStartModifier += pitch) {
			position = loopStartModifier >> 8;
			audioDataByte = audioData[position];
			audioLoopOffset = (audioDataByte << 8) + (loopStartModifier & 255) * (audioData[position + 1] - audioDataByte);
			index = offset++;
			samples[index] += audioLoopOffset * rightChannelVolume >> 6;
			index = offset++;
			samples[index] += audioLoopOffset * leftChannelVolume >> 6;
		}

		if (pitch == 0 || (length = (offset >> 1) + (endPosition - loopStartModifier + pitch - 1) / pitch) > sampleLength) {
			length = sampleLength;
		}

		length <<= 1;

		for (position = sampleLoopEnd; offset < length; loopStartModifier += pitch) {
			audioDataByte = audioData[loopStartModifier >> 8];
			audioLoopOffset = (audioDataByte << 8) + (position - audioDataByte) * (loopStartModifier & 255);
			index = offset++;
			samples[index] += audioLoopOffset * rightChannelVolume >> 6;
			index = offset++;
			samples[index] += audioLoopOffset * leftChannelVolume >> 6;
		}

		rawAudioStream.loopStartModifier = loopStartModifier;
		return offset >> 1;
	}

	static int getFirstModifiedMonoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int sampleLength, int startPosition, RawAudioStream rawAudioStream, int pitchOffset, int loopStart) {
		int position;
		if (pitchOffset == 0 || (position = offset + (pitchOffset + (startPosition + 256 - loopStartModifier)) / pitchOffset) > sampleLength) {
			position = sampleLength;
		}

		int index;
		int modifierIndex;
		while (offset < position) {
			modifierIndex = loopStartModifier >> 8;
			byte audioPositionByte = audioData[modifierIndex - 1];
			index = offset++;
			samples[index] += ((audioPositionByte << 8) + (audioData[modifierIndex] - audioPositionByte) * (loopStartModifier & 255)) * volume >> 6;
			loopStartModifier += pitchOffset;
		}

		if (pitchOffset == 0 || (position = offset + (pitchOffset + (startPosition - loopStartModifier)) / pitchOffset) > sampleLength) {
			position = sampleLength;
		}

		for (modifierIndex = pitchOffset; offset < position; loopStartModifier += modifierIndex) {
			index = offset++;
			samples[index] += ((loopStart << 8) + (audioData[loopStartModifier >> 8] - loopStart) * (loopStartModifier & 255)) * volume >> 6;
		}

		rawAudioStream.loopStartModifier = loopStartModifier;
		return offset;
	}

	static int getFirstModifiedStereoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int length, int startPosition, RawAudioStream rawAudioStream, int pitchOffset, int loopStartPosition) {
		int position;
		if (pitchOffset == 0 || (position = offset + (startPosition + 256 - loopStartModifier + pitchOffset) / pitchOffset) > length) {
			position = length;
		}

		offset <<= 1;

		int sampleOffset;
		int rightIndex;
		int index;
		for (position <<= 1; offset < position; loopStartModifier += pitchOffset) {
			index = loopStartModifier >> 8;
			byte audioPositionByte = audioData[index - 1];
			rightIndex = (audioData[index] - audioPositionByte) * (loopStartModifier & 255) + (audioPositionByte << 8);
			sampleOffset = offset++;
			samples[sampleOffset] += rightIndex * rightChannelVolume >> 6;
			sampleOffset = offset++;
			samples[sampleOffset] += rightIndex * leftChannelVolume >> 6;
		}

		if (pitchOffset == 0 || (position = (offset >> 1) + (startPosition - loopStartModifier + pitchOffset) / pitchOffset) > length) {
			position = length;
		}

		position <<= 1;

		for (index = loopStartPosition; offset < position; loopStartModifier += pitchOffset) {
			rightIndex = (index << 8) + (loopStartModifier & 255) * (audioData[loopStartModifier >> 8] - index);
			sampleOffset = offset++;
			samples[sampleOffset] += rightIndex * rightChannelVolume >> 6;
			sampleOffset = offset++;
			samples[sampleOffset] += rightIndex * leftChannelVolume >> 6;
		}

		rawAudioStream.loopStartModifier = loopStartModifier;
		return offset >> 1;
	}

	static int calculateUnmodifiedMonoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int overallVolume, int positionOffset, int endPosition, RawAudioStream rawAudioStream) {
		loopStartModifier >>= 8;
		endPosition >>= 8;
		volume <<= 2;
		overallVolume <<= 2;
		int position;
		if ((position = offset + endPosition - loopStartModifier) > positionOffset) {
			position = positionOffset;
		}

		rawAudioStream.rightChannelVolume += rawAudioStream.overallRightChannel * (position - offset);
		rawAudioStream.leftChannelVolume += rawAudioStream.overallLeftChannel * (position - offset);

		int index;
		for (position -= 3; offset < position; volume += overallVolume) {
			index = offset++;
			samples[index] += audioData[loopStartModifier++] * volume;
			volume += overallVolume;
			index = offset++;
			samples[index] += audioData[loopStartModifier++] * volume;
			volume += overallVolume;
			index = offset++;
			samples[index] += audioData[loopStartModifier++] * volume;
			volume += overallVolume;
			index = offset++;
			samples[index] += audioData[loopStartModifier++] * volume;
		}

		for (position += 3; offset < position; volume += overallVolume) {
			index = offset++;
			samples[index] += audioData[loopStartModifier++] * volume;
		}

		rawAudioStream.volume = volume >> 2;
		rawAudioStream.loopStartModifier = loopStartModifier << 8;
		return offset;
	}

	static int calculateUnmodifiedStereoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int overallRightChannelVolume, int overallLeftChannelVolume, int positionOffset, int endPosition, RawAudioStream rawAudioStream) {
		loopStartModifier >>= 8;
		endPosition >>= 8;
		rightChannelVolume <<= 2;
		leftChannelVolume <<= 2;
		overallRightChannelVolume <<= 2;
		overallLeftChannelVolume <<= 2;
		int position;
		if ((position = endPosition + offset - loopStartModifier) > positionOffset) {
			position = positionOffset;
		}

		rawAudioStream.volume += rawAudioStream.overallVolume * (position - offset);
		offset <<= 1;
		position <<= 1;

		byte audioDataByte;
		int index;
		for (position -= 6; offset < position; leftChannelVolume += overallLeftChannelVolume) {
			audioDataByte = audioData[loopStartModifier++];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			rightChannelVolume += overallRightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
			leftChannelVolume += overallLeftChannelVolume;
			audioDataByte = audioData[loopStartModifier++];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			rightChannelVolume += overallRightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
			leftChannelVolume += overallLeftChannelVolume;
			audioDataByte = audioData[loopStartModifier++];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			rightChannelVolume += overallRightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
			leftChannelVolume += overallLeftChannelVolume;
			audioDataByte = audioData[loopStartModifier++];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			rightChannelVolume += overallRightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
		}

		for (position += 6; offset < position; leftChannelVolume += overallLeftChannelVolume) {
			audioDataByte = audioData[loopStartModifier++];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			rightChannelVolume += overallRightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
		}

		rawAudioStream.rightChannelVolume = rightChannelVolume >> 2;
		rawAudioStream.leftChannelVolume = leftChannelVolume >> 2;
		rawAudioStream.loopStartModifier = loopStartModifier << 8;
		return offset >> 1;
	}

	static int getUnmodifiedLoopStartMonoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int overallVolume, int volumeOffset, int startPosition, RawAudioStream rawAudioStream) {
		loopStartModifier >>= 8;
		startPosition >>= 8;
		volume <<= 2;
		overallVolume <<= 2;
		int position;
		if ((position = offset + loopStartModifier - (startPosition - 1)) > volumeOffset) {
			position = volumeOffset;
		}

		rawAudioStream.rightChannelVolume += rawAudioStream.overallRightChannel * (position - offset);
		rawAudioStream.leftChannelVolume += rawAudioStream.overallLeftChannel * (position - offset);

		int index;
		for (position -= 3; offset < position; volume += overallVolume) {
			index = offset++;
			samples[index] += audioData[loopStartModifier--] * volume;
			volume += overallVolume;
			index = offset++;
			samples[index] += audioData[loopStartModifier--] * volume;
			volume += overallVolume;
			index = offset++;
			samples[index] += audioData[loopStartModifier--] * volume;
			volume += overallVolume;
			index = offset++;
			samples[index] += audioData[loopStartModifier--] * volume;
		}

		for (position += 3; offset < position; volume += overallVolume) {
			index = offset++;
			samples[index] += audioData[loopStartModifier--] * volume;
		}

		rawAudioStream.volume = volume >> 2;
		rawAudioStream.loopStartModifier = loopStartModifier << 8;
		return offset;
	}

	static int getUnmodifiedLoopStartStereoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int overallRightChannelVolume, int overallLeftChannelVolume, int volumeOffset, int startPosition, RawAudioStream rawAudioStream) {
		loopStartModifier >>= 8;
		startPosition >>= 8;
		rightChannelVolume <<= 2;
		leftChannelVolume <<= 2;
		overallRightChannelVolume <<= 2;
		overallLeftChannelVolume <<= 2;
		int position;
		if ((position = loopStartModifier + offset - (startPosition - 1)) > volumeOffset) {
			position = volumeOffset;
		}

		rawAudioStream.volume += rawAudioStream.overallVolume * (position - offset);
		offset <<= 1;
		position <<= 1;

		byte audioDataByte;
		int index;
		for (position -= 6; offset < position; leftChannelVolume += overallLeftChannelVolume) {
			audioDataByte = audioData[loopStartModifier--];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			rightChannelVolume += overallRightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
			leftChannelVolume += overallLeftChannelVolume;
			audioDataByte = audioData[loopStartModifier--];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			rightChannelVolume += overallRightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
			leftChannelVolume += overallLeftChannelVolume;
			audioDataByte = audioData[loopStartModifier--];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			rightChannelVolume += overallRightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
			leftChannelVolume += overallLeftChannelVolume;
			audioDataByte = audioData[loopStartModifier--];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			rightChannelVolume += overallRightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
		}

		for (position += 6; offset < position; leftChannelVolume += overallLeftChannelVolume) {
			audioDataByte = audioData[loopStartModifier--];
			index = offset++;
			samples[index] += audioDataByte * rightChannelVolume;
			rightChannelVolume += overallRightChannelVolume;
			index = offset++;
			samples[index] += audioDataByte * leftChannelVolume;
		}

		rawAudioStream.rightChannelVolume = rightChannelVolume >> 2;
		rawAudioStream.leftChannelVolume = leftChannelVolume >> 2;
		rawAudioStream.loopStartModifier = loopStartModifier << 8;
		return offset >> 1;
	}

	static int calculateModifiedMonoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int overallVolume, int positionOffset, int endPosition, RawAudioStream rawAudioStream, int pitch, int sampleLoopEnd) {
		rawAudioStream.rightChannelVolume -= rawAudioStream.overallRightChannel * offset;
		rawAudioStream.leftChannelVolume -= rawAudioStream.overallLeftChannel * offset;
		int position;
		if (pitch == 0 || (position = offset + (endPosition - loopStartModifier + pitch - 257) / pitch) > positionOffset) {
			position = positionOffset;
		}

		byte audioDataByte;
		int sampleIndex;
		int audioIndex;
		while (offset < position) {
			audioIndex = loopStartModifier >> 8;
			audioDataByte = audioData[audioIndex];
			sampleIndex = offset++;
			samples[sampleIndex] += ((audioDataByte << 8) + (audioData[audioIndex + 1] - audioDataByte) * (loopStartModifier & 255)) * volume >> 6;
			volume += overallVolume;
			loopStartModifier += pitch;
		}

		if (pitch == 0 || (position = offset + (endPosition - loopStartModifier + pitch - 1) / pitch) > positionOffset) {
			position = positionOffset;
		}

		for (audioIndex = sampleLoopEnd; offset < position; loopStartModifier += pitch) {
			audioDataByte = audioData[loopStartModifier >> 8];
			sampleIndex = offset++;
			samples[sampleIndex] += ((audioDataByte << 8) + (audioIndex - audioDataByte) * (loopStartModifier & 255)) * volume >> 6;
			volume += overallVolume;
		}

		rawAudioStream.rightChannelVolume += rawAudioStream.overallRightChannel * offset;
		rawAudioStream.leftChannelVolume += rawAudioStream.overallLeftChannel * offset;
		rawAudioStream.volume = volume;
		rawAudioStream.loopStartModifier = loopStartModifier;
		return offset;
	}

	static int calculateModifiedStereoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int overallRightChannelVolume, int overallLeftChannelVolume, int positionOffset, int endPosition, RawAudioStream rawAudioStream, int pitch, int sampleLoopEnd) {
		rawAudioStream.volume -= offset * rawAudioStream.overallVolume;
		int position;
		if (pitch == 0 || (position = offset + (endPosition - loopStartModifier + pitch - 257) / pitch) > positionOffset) {
			position = positionOffset;
		}

		offset <<= 1;

		byte audioDataByte;
		int sampleIndex;
		int audioOffset;
		int audioIndex;
		for (position <<= 1; offset < position; loopStartModifier += pitch) {
			audioIndex = loopStartModifier >> 8;
			audioDataByte = audioData[audioIndex];
			audioOffset = (audioDataByte << 8) + (loopStartModifier & 255) * (audioData[audioIndex + 1] - audioDataByte);
			sampleIndex = offset++;
			samples[sampleIndex] += audioOffset * rightChannelVolume >> 6;
			rightChannelVolume += overallRightChannelVolume;
			sampleIndex = offset++;
			samples[sampleIndex] += audioOffset * leftChannelVolume >> 6;
			leftChannelVolume += overallLeftChannelVolume;
		}

		if (pitch == 0 || (position = (offset >> 1) + (endPosition - loopStartModifier + pitch - 1) / pitch) > positionOffset) {
			position = positionOffset;
		}

		position <<= 1;

		for (audioIndex = sampleLoopEnd; offset < position; loopStartModifier += pitch) {
			audioDataByte = audioData[loopStartModifier >> 8];
			audioOffset = (audioDataByte << 8) + (audioIndex - audioDataByte) * (loopStartModifier & 255);
			sampleIndex = offset++;
			samples[sampleIndex] += audioOffset * rightChannelVolume >> 6;
			rightChannelVolume += overallRightChannelVolume;
			sampleIndex = offset++;
			samples[sampleIndex] += audioOffset * leftChannelVolume >> 6;
			leftChannelVolume += overallLeftChannelVolume;
		}

		offset >>= 1;
		rawAudioStream.volume += rawAudioStream.overallVolume * offset;
		rawAudioStream.rightChannelVolume = rightChannelVolume;
		rawAudioStream.leftChannelVolume = leftChannelVolume;
		rawAudioStream.loopStartModifier = loopStartModifier;
		return offset;
	}

	static int getModifiedLoopStartMonoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int overallVolume, int volumeOffset, int startPosition, RawAudioStream rawAudioStream, int pitch, int sampleLoopStart) {
		rawAudioStream.rightChannelVolume -= rawAudioStream.overallRightChannel * offset;
		rawAudioStream.leftChannelVolume -= rawAudioStream.overallLeftChannel * offset;
		int position;
		if (pitch == 0 || (position = offset + (startPosition + 256 - loopStartModifier + pitch) / pitch) > volumeOffset) {
			position = volumeOffset;
		}

		int index;
		int audioIndex;
		while (offset < position) {
			audioIndex = loopStartModifier >> 8;
			byte audioDataByte = audioData[audioIndex - 1];
			index = offset++;
			samples[index] += ((audioDataByte << 8) + (audioData[audioIndex] - audioDataByte) * (loopStartModifier & 255)) * volume >> 6;
			volume += overallVolume;
			loopStartModifier += pitch;
		}

		if (pitch == 0 || (position = offset + (startPosition - loopStartModifier + pitch) / pitch) > volumeOffset) {
			position = volumeOffset;
		}

		for (audioIndex = pitch; offset < position; loopStartModifier += audioIndex) {
			index = offset++;
			samples[index] += ((sampleLoopStart << 8) + (audioData[loopStartModifier >> 8] - sampleLoopStart) * (loopStartModifier & 255)) * volume >> 6;
			volume += overallVolume;
		}

		rawAudioStream.rightChannelVolume += rawAudioStream.overallRightChannel * offset;
		rawAudioStream.leftChannelVolume += rawAudioStream.overallLeftChannel * offset;
		rawAudioStream.volume = volume;
		rawAudioStream.loopStartModifier = loopStartModifier;
		return offset;
	}

	static int getModifiedLoopStartStereoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int overallRightChannelVolume, int overallLeftChannelVolume, int volumeOffset, int startPosition, RawAudioStream rawAudioStream, int pitch, int loopStartPosition) {
		rawAudioStream.volume -= offset * rawAudioStream.overallVolume;
		int volume;
		if (pitch == 0 || (volume = offset + (startPosition + 256 - loopStartModifier + pitch) / pitch) > volumeOffset) {
			volume = volumeOffset;
		}

		offset <<= 1;

		int sampleIndex;
		int audioPosition;
		int audioIndex;
		for (volume <<= 1; offset < volume; loopStartModifier += pitch) {
			audioIndex = loopStartModifier >> 8;
			byte audioDataByte = audioData[audioIndex - 1];
			audioPosition = (audioData[audioIndex] - audioDataByte) * (loopStartModifier & 255) + (audioDataByte << 8);
			sampleIndex = offset++;
			samples[sampleIndex] += audioPosition * rightChannelVolume >> 6;
			rightChannelVolume += overallRightChannelVolume;
			sampleIndex = offset++;
			samples[sampleIndex] += audioPosition * leftChannelVolume >> 6;
			leftChannelVolume += overallLeftChannelVolume;
		}

		if (pitch == 0 || (volume = (offset >> 1) + (startPosition - loopStartModifier + pitch) / pitch) > volumeOffset) {
			volume = volumeOffset;
		}

		volume <<= 1;

		for (audioIndex = loopStartPosition; offset < volume; loopStartModifier += pitch) {
			audioPosition = (audioIndex << 8) + (loopStartModifier & 255) * (audioData[loopStartModifier >> 8] - audioIndex);
			sampleIndex = offset++;
			samples[sampleIndex] += audioPosition * rightChannelVolume >> 6;
			rightChannelVolume += overallRightChannelVolume;
			sampleIndex = offset++;
			samples[sampleIndex] += audioPosition * leftChannelVolume >> 6;
			leftChannelVolume += overallLeftChannelVolume;
		}

		offset >>= 1;
		rawAudioStream.volume += rawAudioStream.overallVolume * offset;
		rawAudioStream.rightChannelVolume = rightChannelVolume;
		rawAudioStream.leftChannelVolume = leftChannelVolume;
		rawAudioStream.loopStartModifier = loopStartModifier;
		return offset;
	}
}
