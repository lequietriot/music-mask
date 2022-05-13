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
package com.ibm.realtime.synth.test;

import com.ibm.realtime.synth.engine.*;
import com.ibm.realtime.synth.modules.*;
import com.ibm.realtime.synth.soundfont2.SoundFontSoundbank;
import com.ibm.realtime.synth.utils.AudioUtils;
import com.ibm.realtime.synth.utils.MidiUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;
import java.io.File;
import java.util.List;

import static com.ibm.realtime.synth.utils.Debug.*;

/**
 * A console synth implementation that uses a SoundFont 2 soundbank.
 * 
 * @author florian
 */
public class SoundFont2Synth {

	public static final double DEFAULT_LATENCY = 50.0;
	public static final double DEFAULT_SLICE_TIME = 1.0;

	// fields needed in the timeOutThread
	private static boolean running = false;
	private static Object runLock = new Object();
	private static int timeOutSeconds = 0; // no timeOut
	private static Synthesizer synth = null;
	private static AudioSink sink = null;
	private static double latencyMillis = DEFAULT_LATENCY;
	private static double volumeDB = 0;

	// needed by play method
	private static SMFMidiIn smfPlayer = null;
	private static MaintenanceThread maintenance = null;

	private static MemoryAllocator memAllocator = null;

	public static void start(String customSoundBankPath, byte[] midiData, int musicVolume) throws Exception {

		String soundbankFile = customSoundBankPath;
		int[] midiDevs = new int[30];
		int midiDevCount = 0;
		String[] dmidiDevs = new String[30];
		int dmidiDevCount = 0;
		int audioDev = -2; // use default device
		String directAudioDev = "";
		String eventronAudioDev = "";
		double sliceTimeMillis = DEFAULT_SLICE_TIME;
		String outputFile = "";
		double sampleRate = 44100.0;
		int channels = 2;
		int bitsPerSample = 16;
		int threadCount = -1;
		boolean useJavaSoundMidiPlayback = true;
		boolean benchmarkMode = false;
		boolean lowlatencyMode = false;
		boolean interactive = true;
		boolean errorOccured = false;
		int noteDispatcherMode= Synthesizer.NOTE_DISPATCHER_REQUEST_ASYNCHRONOUS;
		double playWaitTime = 0.5;
		boolean preload = true;

		// verify parameters

		File sbFile = new File(soundbankFile);
		if (sbFile.isDirectory() || !sbFile.exists()) {
			out("Invalid soundfont file: " + sbFile);
			out("Please specify an existing soundbank file with the -sb command line parameter.");
			out("");
			printUsageAndExit();
		}
		File wavFile = null;
		byte[] mFile = null;
		if (midiData != null) {
			mFile = midiData;
			if (mFile == null) {
				out("Invalid MIDI input file: " + mFile);
				out("Please specify an existing MIDI file with the -if command line parameter.");
				out("");
				printUsageAndExit();
			}
		}
		if (((audioDev >= -1 ? 1 : 0) + (directAudioDev != "" ? 1 : 0) + (eventronAudioDev != "" ? 1
				: 0)) > 1) {
			out("Error: can only select one audio device!");
			out("");
			printUsageAndExit();
		}
		// use Java Sound's default device if neither -a nor -da is specified
		if (audioDev == -2 && directAudioDev == "" && eventronAudioDev == "") {
			audioDev = -1;
		}
		// non-interactive only possible with duration
		if (!interactive && timeOutSeconds == 0) {
			error("Refuse to run in non-interactive mode without timeout.");
			interactive = true;
		}
		if (lowlatencyMode && latencyMillis > 2.0) {
			debug("WARNING: using low latency mode with larger buffer size will increase jitter!");
		}

		AudioFormat format = new AudioFormat((float) sampleRate, bitsPerSample,
				channels, true, false);
		DiskWriterSink waveSink = null;
		AudioPullThread pullThread = null;
		DirectMidiIn[] dmidis = null;
		JavaSoundMidiIn[] midis = null;

		// STARTUP ENGINE

		try {
			debugNoNewLine("Loading soundbank " + sbFile + "...");
			SoundFontSoundbank soundbank = new SoundFontSoundbank(sbFile);
			debug(soundbank.getName());

			// set up mixer
			//debug("creating Mixer...");
			AudioMixer mixer = new AudioMixer();

			// slice time should always be <= latency
			if (sliceTimeMillis > latencyMillis) {
				sliceTimeMillis = latencyMillis;
			}
			// create the pull thread
			// debug("creating AudioPullThread...");
			pullThread = new AudioPullThread();
			pullThread.setSliceTimeMillis(sliceTimeMillis);
			pullThread.setInput(mixer);

			// set up soundcard (sink)
			int bufferSizeSamples = pullThread.getPreferredSinkBufferSizeSamples(
					latencyMillis, format.getSampleRate());
			if (audioDev >= -1) {
				debugNoNewLine("creating JavaSoundSink...");
				JavaSoundSink jsSink = new JavaSoundSink();
				// open the sink
				jsSink.open(audioDev, format, bufferSizeSamples);
				debug(jsSink.getName());
				sink = jsSink;
				format = jsSink.getFormat();
			} else if (directAudioDev != "") {
				debug("creating DirectAudioSink: " + directAudioDev);
				DirectAudioSink daSink = new DirectAudioSink();
				daSink.open(directAudioDev, format, bufferSizeSamples);
				sink = daSink;
				format = daSink.getFormat();
				debug("- slice: "
						+ pullThread.getSliceTimeSamples(format.getSampleRate())
						+ " samples = "
						+ format3(pullThread.getSliceTime() * 1000.0)
						+ "ms, period: "
						+ daSink.getPeriodSizeSamples() + " samples = "
						+ format3(daSink.getPeriodTimeNanos() / 1000000.0)
						+ "ms, ALSA buffer: "
						+ daSink.getALSABufferSizeSamples() + " samples = "
						+ format3(daSink.getALSABufferTimeNanos() / 1000000.0)
						+ "ms");

			}
			debug("- audio format: " + format.getChannels() + " channels, "
					+ format.getSampleSizeInBits() + " bits, "
					+ format.getFrameSize() + " bytes per frame, "
					+ format1(format.getSampleRate()) + " Hz, "
					+ (format.isBigEndian() ? "big endian" : "little endian"));

			// use effective latency
			latencyMillis = AudioUtils.samples2nanos(sink.getBufferSize(),
					sampleRate) / 1000000.0;
			pullThread.setSink(sink);
			// set up Synthesizer
			debugNoNewLine("creating Synthesizer: ");
			synth = new Synthesizer(soundbank, mixer);
			synth.setFixedDelayNanos((long) (((2.0 * latencyMillis)) * 1000000.0));
			synth.setMasterClock(sink);
			synth.getParams().setMasterTuning(443);
			synth.setNoteDispatcherMode(noteDispatcherMode);
			debugNoNewLine(format1(synth.getParams().getMasterTuning())
					+ "Hz tuning, ");
			if (benchmarkMode) {
				synth.setBenchmarkMode(true);
				if (volumeDB == 0.0) {
					volumeDB = AudioUtils.linear2decibel(4.0);
				}
			}
			if (lowlatencyMode) {
				synth.setSchedulingOfRealtimeEvents(false);
			}
			synth.getParams().setMasterVolume(
					AudioUtils.decibel2linear(volumeDB));
			if (volumeDB != 0.0) {
				debugNoNewLine("volume: "
						+ format1(AudioUtils.linear2decibel(synth.getParams().getMasterVolume()))
						+ "dB (linear: "
						+ format3(synth.getParams().getMasterVolume()) + "), ");
			}
			pullThread.addListener(synth);
			if (threadCount >= 0) {
				synth.setRenderThreadCount(threadCount);
			}
			if (preload) {
				debugNoNewLine("preloading, ");
				synth.preLoad();
			}
			setVolume(musicVolume / 100.0);
			synth.start();
			if (synth.getRenderThreadCount() > 1) {
				debug("" + synth.getRenderThreadCount()
						+ " render threads");
			} else {
				debug("rendering from mixer thread");
			}

			// set up wave output
			if (wavFile != null) {
				debug("setting up wave file output to file "+wavFile);
				waveSink = new DiskWriterSink();
				waveSink.open(wavFile, format);
				pullThread.setSlaveSink(waveSink);
			}
			
			debugNoNewLine("MIDI ports:");

			// open Java Sound MIDI ports?
			midis = new JavaSoundMidiIn[midiDevCount];
			midiDevCount = 0;
			for (int i = 0; i < midis.length; i++) {
				if (midiDevs[i] < JavaSoundMidiIn.getMidiDeviceCount()) {
					debugNoNewLine(" Java Sound ");
					JavaSoundMidiIn midi = new JavaSoundMidiIn(i);
					midis[midiDevCount] = midi;
					midi.addListener(synth);
					try {
						midi.open(midiDevs[i]);
						out(midi.getName());
						if (lowlatencyMode) {
							midi.setTimestamping(false);
						}
						midiDevCount++;
					} catch (Exception e) {
						out("ERROR:");
						out(" no MIDI: " + e.toString());
					}
				}
			}
			// open Direct MIDI ports?
			dmidis = new DirectMidiIn[dmidiDevCount];
			dmidiDevCount = 0;
			for (int i = 0; i < dmidis.length; i++) {
				debugNoNewLine(" Direct " + dmidiDevs[i]);
				DirectMidiIn dmidi = new DirectMidiIn(i);
				dmidis[dmidiDevCount] = dmidi;
				dmidi.addListener(synth);
				try {
					dmidi.open(dmidiDevs[i]);
					if (lowlatencyMode) {
						dmidi.setTimestamping(false);
					}
					dmidiDevCount++;
				} catch (Exception e) {
					out("ERROR:");
					out(" no MIDI: " + e.toString());
				}
			}

			if (midiDevCount + dmidiDevCount == 0) {
				debug("none");
			} else {
				// new line
				debug("");
			}

			// create the maintenance thread
			maintenance = new MaintenanceThread();

			maintenance.addServiceable(mixer);
			for (int i = 0; i < midiDevCount; i++) {
				maintenance.addAdjustableClock(midis[i]);
			}
			for (int i = 0; i < dmidiDevCount; i++) {
				maintenance.addAdjustableClock(dmidis[i]);
			}
			maintenance.setMasterClock(sink);

			// push the MIDI input file to the synthesizer (at once)
			if (mFile != null) {
				if (useJavaSoundMidiPlayback) {
					playMidiFileWithJavaSound(mFile);
				} else {
					outNoNewLine("Playing MIDI File");
					SMFPusher pusher = new SMFPusher();
					pusher.open(mFile);
					int events = pusher.pushToSynth(synth);
					out(", duration:" + format3(pusher.getDurationInSeconds())
							+ "s, " + events + " notes.");
				}
			}

			// start memory allocator
			if (memAllocator != null) {
				debug("Starting memory allocator");
				memAllocator.setEnabled(true);
			}
			// start the pull thread -- from now on, the mixer is polled
			// for new data
			pullThread.start();
			// start the maintenance thread
			maintenance.start();

			if (ThreadFactory.hasRealtimeThread()) {
				if (ThreadFactory.couldSetRealtimeThreadPriority()) {
					debug("Using realtime threads with high priority");
				} else {
					debug("Using realtime threads with default priority");
				}
			} else {
				debug("Realtime threads are not enabled.");
			}

			if (smfPlayer != null) {
				// grace period for playback start to warm up engine
				try {
					Thread.sleep((int) (playWaitTime * 1000.0));
				} catch (InterruptedException ie) {
				}
				smfPlayer.start();
			}
			maintenance.synchronizeClocks(true);

			running = true;

			// if we're using a time out, start a thread to get stdin
			if (interactive && timeOutSeconds > 0) {
				Thread t = new Thread(new Runnable() {
					public void run() {
						interactiveKeyHandling();
					}
				});
				t.setDaemon(true);
				t.start();
			}

			if (timeOutSeconds > 0) {
				out("This program will exit after " + timeOutSeconds
						+ " seconds.");
			}
			if (interactive) {
				out("Press ENTER or SPACE+ENTER to play a random note");
				out("p+ENTER:Panic                    q+ENTER to quit");
			}

			if (timeOutSeconds == 0) {
				interactiveKeyHandling();
			} else {
				long startTime = System.nanoTime();
				while (running) {
					try {
						synchronized (runLock) {
							runLock.wait(1000);
						}
						if (timeOutSeconds > 0) {
							long elapsedSeconds = (System.nanoTime() - startTime) / 1000000000L;
							if (elapsedSeconds >= timeOutSeconds) {
								debug("Timeout reached. Stopping...");
								running = false;
								break;
							}
						}
					} catch (Exception e) {
						error(e);
					}
				}
			}
		} catch (Throwable t) {
			error(t);
			errorOccured = true;
		}

		// clean-up
		if (memAllocator != null) {
			memAllocator.setEnabled(false);
		}

		if (smfPlayer != null) {
			try {
				smfPlayer.close();
			} catch (Exception e) {
				error(e);
			}
		}
		if (waveSink != null) {
			waveSink.close();
		}
		if (maintenance != null) {
			maintenance.stop();
		}
		if (pullThread != null) {
			pullThread.stop();
			out("Resynchronizations of audio device: " + pullThread.getResynchCounter());
		}
		for (int i = 0; i < midiDevCount; i++) {
			if (midis[i] != null) {
				midis[i].close();
			}
		}
		for (int i = 0; i < dmidiDevCount; i++) {
			if (dmidis[i] != null) {
				dmidis[i].close();
			}
		}
		if (synth != null) {
			synth.close();
		}
		if (sink != null) {
			sink.close();
			if (sink instanceof DirectAudioSink) {
				out("Underruns of the audio device     : " + ((DirectAudioSink) sink).getUnderrunCount());
			}
		}

		// done
		out("SoundFont2Synth exit.");
		System.exit(errorOccured ? 1 : 0);
	}

	private static void interactiveKeyHandling() {
		int note = -1;
		int ignoreNext = 0;
		try {
			while (true) {
				int c = (int) ((char) System.in.read());

				// out(""+c);
				if (ignoreNext > 0 && (c == 10 || c == 13)) {
					ignoreNext--;
					continue;
				}

				if (c != 10 && c != 13) {
					ignoreNext = 2;
				}

				// finish a previously played note
				if (note >= 0) {
					synth.midiInReceived(new MidiEvent(null,
							sink.getAudioTime(), 0, 0x90, note, 0));
					note = -1;
				}
				if (c == 'q') {
					out("stopping...");
					break;
				}
				if (c == 'p') {
					out("Reset.");
					synth.reset();
				}
				if (c == 32 || c == 10 || c == 13) {
					// generate a random Note On event on a white key
					do {
						note = ((int) (Math.random() * 40)) + 40;
					} while (!MidiUtils.isWhiteKey(note));
					int vel = ((int) (Math.random() * 40)) + 87;
					out("Playing: NoteOn " + note + "," + vel);
					synth.midiInReceived(new MidiEvent(null,
							sink.getAudioTime(), 0, 0x90, note, vel));
					if (c != 32) ignoreNext++;
				}
			}
		} catch (Exception e) {
			error(e);
		}
		running = false;
		synchronized (runLock) {
			runLock.notifyAll();
		}
	}

	private static void playMidiFileWithJavaSound(byte[] file) {
		try {
			smfPlayer = new SMFMidiIn();
			smfPlayer.open(file);
			// smfPlayer.setStopListener(this);

			// smfPlayer.setDeviceIndex(MIDI_DEV_COUNT); // one after the real
			// MIDI devices
			smfPlayer.addListener(synth);
			maintenance.addAdjustableClock(smfPlayer);

			// make sure the clocks are synchronized
			maintenance.synchronizeClocks(true);
			debug("MIDI file playing with Java Sound: " + file);

		} catch (Exception e) {
			error("Cannot load MIDI file: " + file);
			error(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static void printUsageAndExit() {
		DirectAudioSink.isAvailable();
		List infos = JavaSoundMidiIn.getDeviceList();
		List<DirectMidiInDeviceEntry> dminfos = DirectMidiIn.getDeviceEntryList();
		List<Mixer.Info> ainfos = JavaSoundSink.getDeviceList();
		List<DirectAudioSinkDeviceEntry> directSinks = DirectAudioSink.getDeviceEntryList();
		out("Usage:");
		out("java SoundFont2Synth [options]");
		out("Options:");
		out("-h              : display this help message");
		out("-sb [.sf2 file] : soundbank in .sf2 format to be used");
		out("-l  [millis]    : latency: buffer size in millis (default: "
				+ (((int) DEFAULT_LATENCY * 10) / 10.0) + ")");

		out("-if [.mid file] : MIDI file to be played");
		out("-of [.wav file] : write output to .wav file");
		if (DirectMidiIn.isAvailable()) {
			out("-dm [dev name]  : specify Direct MIDI port input by name, e.g.:");
			if (dminfos.size() > 0) {
				for (int i = 0; i < dminfos.size(); i++) {
					out("           " + dminfos.get(i).getFullInfoString());
				}
			} else {
				out("            [no Direct MIDI IN devices available]");
			}
		}
		out("-m  [MIDI dev#] : specify Java Sound MIDI port input by number:");
		if (infos.size() > 0) {
			for (int i = 0; i < infos.size(); i++) {
				out("           " + i + ": " + infos.get(i));
			}
		} else {
			out("            [no Java Sound MIDI IN devices available]");
		}
		out("-a  [audio dev#]: specify Java Sound audio output device by number:");
		if (ainfos.size() > 0) {
			for (int i = 0; i < ainfos.size(); i++) {
				String add = "";
				if (JavaSoundSink.isJavaSoundEngine(ainfos.get(i))) {
					add = " [NOT RECOMMENDED]";
				}
				out("           " + i + ": " + ainfos.get(i) + add);
			}
		} else {
			out("           [no Java Sound audio output devices available]");
		}
		if (DirectAudioSink.isAvailable()) {
			out("-da [dev name]  : specify direct audio output device by name, e.g.:");
			for (int i = 0; i < directSinks.size(); i++) {
				out("           " + directSinks.get(i).getFullInfoString());
			}
			if (directSinks.size() == 0) {
				out("            [no direct audio output devices available]");
			}
		}
		if (DirectAudioSink.isAvailable()) {
			out("NOTE: -da, -a, -ea cannot be used simultaneously.");
		}
		out("Advanced options:");
		out("-s  [millis]    : slice time: quantum time in millis (default: "
				+ (((int) DEFAULT_SLICE_TIME * 10) / 10.0) + ")");
		out("-c  [channels]  : number of output channels, default 2 (stereo)");
		out("-sr [Hz]        : sample rate in Hz (default 44100.0 Hz)");
		out("-b  [bits]      : sample resolution in bits (default: 16 bits)");
		out("-vol <dB>       : set volume to <dB> deciBels -100...0...inf (default: 0dB)");
		out("-t  [count]     : use [count] number of threads (default: "
				+ AsynchronousRenderer.getDefaultThreadCount() + " theads)");
		out("-trace <dest>   : metronome tracing, <dest> is either filename or port");
		out("-w <sec>        : if playing a MIDI file, wait <sec> seconds before starting");
		out("-duration <sec> : quit this program after <sec> seconds");
		out("-ni             : non-interactive, only valid with duration");
		out("-noPreload      : do not preload the synth rendering classes");
		out("-nd:{async|sync|auto}: mode for note dispatcher (default: auto)");

		out("-allocator <Rate> <Size> <Retention> : run an allocator thread with");
		out("                  the goal of causing GC activity.");
		out("                  Rate: effective allocation rate in bytes/second");
		out("                  Size: size of individual objects, in bytes");
		out("                  Retention: number of objects to retain in memory");
		
		out("-Xbenchmark     : benchmark mode");
		out("-Xlowlatency    : do not schedule incoming MIDI events. Will increase jitter, ");
		out("                  so only recommended with very small buffer sizes");
		out("-XXnoReuseObjects : wasteful use of buffer objects");
		out("-XnoJavaSoundSequencer : use Harmonicon's scheduler for MIDI file playback");
		out("                         instead of Java Sound");
		if (!DirectMidiIn.isAvailable()) {
			out("[direct MIDI input library not available]");
		}
		if (!DirectAudioSink.isAvailable()) {
			out("[direct audio sink library not available]");
		}
		System.exit(1);
	}

	public static double getVolume() {
		if (synth != null) {
			return synth.getParams().getMasterVolume();
		}
		return volumeDB;
	}

	public static void setVolume(double volume) {
		if (synth != null) {
			synth.getParams().setMasterVolume(volume);
			System.out.println("set volume to " + volume);
		}
	}

	public static void fadeOut() {
		new Thread(() -> {
			try {
				if (synth != null) {
					double volume = synth.getParams().getMasterVolume();
					while (volume != 0.0) {
						volume = -0.05;
						Thread.sleep(10);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	public static void stop() {
		smfPlayer.stop();
	}

	public static void close() {
		smfPlayer.close();
	}
}
