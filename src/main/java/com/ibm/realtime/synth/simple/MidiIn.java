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

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.List;

public class MidiIn implements Receiver {

	private boolean open;
	private Listener listener;

	private Transmitter midiInTransmitter;
	private MidiDevice midiIn;

	/**
	 * List of usabe MIDI devices, i.e. they provide a MIDI IN port.
	 */
	private static List<MidiDevice.Info> devList;
	
	public MidiIn() {
		setupMidiDevices();
	}
	
	public void open(int devIndex) throws Exception {
		if (devList.size() == 0) {
			throw new Exception("no MIDI IN devices available!");
		}
		if (devIndex<0 || devIndex>=devList.size()) {
			throw new Exception("No MIDI IN device is selected!");
		}
		open = true;
    	if (midiIn != null) {
    		midiIn.close();
    		midiIn = null;
    	}
   		midiIn = MidiSystem.getMidiDevice(devList.get(devIndex));
		if (!midiIn.isOpen()) {
			midiIn.open();
		}
		midiInTransmitter = midiIn.getTransmitter();
		// connect the device with this instance as Receiver
		midiInTransmitter.setReceiver(this);
		Debug.debug("Opened MIDI IN device '"+devList.get(devIndex)+"'");
	}

	public void close() {
		if (midiInTransmitter != null) {
			Debug.debug("Closing MIDI IN.");
			midiInTransmitter.setReceiver(null);
		}
		if (midiIn != null) {
			midiIn.close();
		}
		midiIn = null;
		devList = null;
		midiInTransmitter = null;
		open = false;
	}

	public void setListener(Listener l) {
		this.listener = l;
	}

	public boolean isOpen() {
		return open;
	}
	

	public static List<MidiDevice.Info> getDeviceList() {
		setupMidiDevices();
		return devList;
	}

	private static void setupMidiDevices() {
		System.out.print("Gathering MIDI devices...");
		if (devList == null) {
			devList = new ArrayList<MidiDevice.Info>();
			MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
			// go through all MIDI devices and see if they are MIDI IN
			for (MidiDevice.Info info : infos) {
				try {
					MidiDevice dev = MidiSystem.getMidiDevice(info);
					if (!(dev instanceof Sequencer)
							&& !(dev instanceof Synthesizer)
							&& (dev.getMaxTransmitters()!=0)) {
						devList.add(info);
					}
				} catch (MidiUnavailableException mue) {
					Debug.debug(mue);
				}
			}
		}
		System.out.println("done ("+devList.size()+" devices available).");
	}

	public static int getMidiDeviceCount() {
		return devList.size();
	}

	// interface Receiver
	public void send(MidiMessage message, long timeStamp) {
		if (listener != null) {
			if (message.getLength() <= 3) {
				if (message instanceof ShortMessage) {
					ShortMessage sm = (ShortMessage) message;
					listener.midiInPlayed(sm.getStatus(), sm.getData1(), sm.getData2());
				} else {
					int data1 = 0;
					int data2 = 0;
					if (message.getLength() > 1) {
						byte[] msg = message.getMessage();
						data1 = msg[1] & 0xFF;
						if (message.getLength() > 2) {
							data2 = msg[2] & 0xFF;
						}
					}
					listener.midiInPlayed(message.getStatus(), data1, data2);
				}
			} else {
				listener.midiInPlayed(message.getMessage());
			}
		}
	}

	public interface Listener {
		public void midiInPlayed(int status, int data1, int data2);
		public void midiInPlayed(byte[] message);
	}

}
