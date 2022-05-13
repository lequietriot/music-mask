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
import java.io.File;
import java.util.List;

public class Main {

	public static final String[] sounds = { "audio/kick_opn_14.wav",
			"audio/13hat_top_31.wav", "audio/snr_rim_l10.wav",
			"audio/snr_prs_l10.wav" };

	public static void main(String[] args) throws Exception {
		int midiDev = -1;
		int audioDev = -1;
		int latencyInMillis = 70;
		
		// parse arguments
		int argi = 0;
		while (argi < args.length) {
			String arg = args[argi]; 
			if (arg.equals("-h")) {
				printUsageAndExit();
			} 
			else if (arg.equals("-m")) {
				argi++;
				if (argi>=args.length) {
					printUsageAndExit();
				}
				midiDev = Integer.parseInt(args[argi]);
			}
			else if (arg.equals("-a")) {
				argi++;
				if (argi>=args.length) {
					printUsageAndExit();
				}
				audioDev = Integer.parseInt(args[argi]);
			}
			else if (arg.equals("-l")) {
				argi++;
				if (argi>=args.length) {
					printUsageAndExit();
				}
				latencyInMillis = Integer.parseInt(args[argi]);
			} else {
				printUsageAndExit();
			}
			argi++;
		}
		

		// load samples
		final AudioFileSource[] src = new AudioFileSource[sounds.length];
		for (int i = 0; i < sounds.length; i++) {
			src[i] = new AudioFileSource(new File(sounds[i]));
		}
		// define the first source's audioformat as the master format
		final AudioFormat format = src[0].getFormat();

		// set up mixer
		final AudioMixer mixer = new AudioMixer(format.getChannels(), format
				.getSampleRate());

		// set up soundcard (sink)
		SoundcardSink sink = new SoundcardSink();
		// open the sink and connect it with the mixer
		sink.open(audioDev, latencyInMillis, format, mixer);
		try {

			// do we want to open a MIDI port?
			MidiIn midi = null;
			if (midiDev >= 0) {
				// start MIDI IN
				midi = new MidiIn();
				midi.setListener(new MidiIn.Listener() {
					public void midiInPlayed(int status, int data1, int data2) {
						// only react to NOTE ON messages with velocity > 0
						if (((status & 0xF0) == 0x90) && (data2 > 0)) {
							AudioFileSource newSrc = src[data1 % src.length]
									.makeClone();
							mixer.addAudioStream(newSrc);
							serviceMixer(mixer);
						}
					}

					public void midiInPlayed(byte[] message) {
						// nothing to do for long MIDI messages
					}

				});
				midi.open(midiDev);
			} else {
				Debug.debug("No MIDI.");
			}
			try {

				// start the sink -- from now on, the mixer is polled for new
				// data
				sink.start();

				System.out.println("Press ENTER for a sound, 'q'+ENTER to quit.");
				int currSrc = 0;
				while (true) {
					char c = (char) System.in.read();
					if (c == 'q') {
						break;
					}
					AudioFileSource newSrc = src[(currSrc++) % src.length]
							.makeClone();
					mixer.addAudioStream(newSrc);
					serviceMixer(mixer);
				}
			} finally {
				// clean-up
				if (midi != null) {
					midi.close();
				}
			}
		} finally {
			sink.close();
		}

		Debug.debug("done");
	}
	
	
	/**
	 * Checks the input lines of the mixer and
	 * removes the ones that aren't needed anymore
	 */
	private static void serviceMixer(AudioMixer mixer) {
		List<AudioInput> ais = mixer.getAudioStreams();
		for (int i = ais.size()-1; i>=0 ; i--) {
			AudioInput ai = ais.get(i);
			if (ai.done()) {
				mixer.removeAudioStream(ai);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void printUsageAndExit() {
		List infos = MidiIn.getDeviceList();
		List ainfos = SoundcardSink.getDeviceList();
		System.out.println("Usage:");
		System.out.println("java Main [-m <MIDI dev>] [-a <audio dev>] [-l <latency>] [-h]");
		System.out.println("-m: allow MIDI input. <MIDI dev> is a number for the MIDI "
				+ "device from the following list:");
		if (infos.size() > 0) {
			for (int i = 0; i < infos.size(); i++) {
				System.out.println("   " + i + ": " + infos.get(i));
			}
		} else {
			System.out.println("    (no MIDI IN devices available)");
		}

		System.out.println("-a: specify the audio output device (otherwise the default device will be used)");
		System.out.println("    <audio dev> is a number for the audio "
				+ "device from the following list:");
		if (ainfos.size() > 0) {
			for (int i = 0; i < ainfos.size(); i++) {
				System.out.println("   " + i + ": " + ainfos.get(i));
			}
		} else {
			System.out.println("    (no audio output devices available)");
		}
		System.out.println("-l: specify the buffer size in milliseconds");
		
		System.exit(1);
	}
}
