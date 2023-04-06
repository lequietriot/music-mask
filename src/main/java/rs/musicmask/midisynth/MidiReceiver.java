/*
 * Copyright (c) 2023, Rodolfo Ruiz-Velasco <ruizvelascorodolfo@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met
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

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

/**
 * An implementation of the Receiver class which functions more similar to RuneScape.
 */
public class MidiReceiver implements Receiver {

    /**
     * A MIDI Synthesizer to send midi messages to.
     */
    MidiAudioStream midiSynth;

    /**
     * A method to set the MIDI synthesizer variable.
     * @param midiAudioStream The MIDI synthesizer stream to use.
     */
    public MidiReceiver(MidiAudioStream midiAudioStream) {
        midiSynth = midiAudioStream;
    }

    /**
     * An overridden method from the Receiver class which processes MIDI messages with the sound bank to create audio.
     * @param message The MIDI Message to send.
     * @param timeStamp The precise point of time in which the MIDI Message is sent.
     */
    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (message != null) {
            if (message instanceof ShortMessage) {
                ShortMessage shortMessage = (ShortMessage) message;
                int command = shortMessage.getCommand();
                int channel = shortMessage.getChannel();
                int data1 = shortMessage.getData1();
                int data2 = shortMessage.getData2();
                if (command == 128) {
                    midiSynth.noteOff(channel, data1);
                } else if (command == 144) {
                    if (data2 > 0) {
                        midiSynth.noteOn(channel, data1, data2);
                    } else {
                        midiSynth.noteOff(channel, data1);
                    }
                } else if (command == 176) {
                    if (data1 == 0) {
                        midiSynth.bankControls[channel] = (data2 << 14) + (midiSynth.bankControls[channel] & -2080769);
                    }

                    if (data1 == 32) {
                        midiSynth.bankControls[channel] = (data2 << 7) + (midiSynth.bankControls[channel] & -16257);
                    }

                    if (data1 == 1) {
                        midiSynth.modulationControls[channel] = (data2 << 7) + (midiSynth.modulationControls[channel] & -16257);
                    }

                    if (data1 == 33) {
                        midiSynth.modulationControls[channel] = data2 + (midiSynth.modulationControls[channel] & -128);
                    }

                    if (data1 == 5) {
                        midiSynth.portamentoTimeControls[channel] = (data2 << 7) + (midiSynth.portamentoTimeControls[channel] & -16257);
                    }

                    if (data1 == 37) {
                        midiSynth.portamentoTimeControls[channel] = data2 + (midiSynth.portamentoTimeControls[channel] & -128);
                    }

                    if (data1 == 7) {
                        midiSynth.volumeControls[channel] = (data2 << 7) + (midiSynth.volumeControls[channel] & -16257);
                    }

                    if (data1 == 39) {
                        midiSynth.volumeControls[channel] = data2 + (midiSynth.volumeControls[channel] & -128);
                    }

                    if (data1 == 10) {
                        midiSynth.panControls[channel] = (data2 << 7) + (midiSynth.panControls[channel] & -16257);
                    }

                    if (data1 == 42) {
                        midiSynth.panControls[channel] = data2 + (midiSynth.panControls[channel] & -128);
                    }

                    if (data1 == 11) {
                        midiSynth.expressionControls[channel] = (data2 << 7) + (midiSynth.expressionControls[channel] & -16257);
                    }

                    if (data1 == 43) {
                        midiSynth.expressionControls[channel] = data2 + (midiSynth.expressionControls[channel] & -128);
                    }

                    int[] controlValues;
                    if (data1 == 64) {
                        controlValues = midiSynth.switchControls;
                        if (data2 >= 64) {
                            controlValues[channel] |= 1;
                        } else {
                            controlValues[channel] &= -2;
                        }
                    }

                    if (data1 == 65) {
                        if (data2 >= 64) {
                            controlValues = midiSynth.switchControls;
                            controlValues[channel] |= 2;
                        } else {
                            midiSynth.setPortamentoSwitch(channel);
                            controlValues = midiSynth.switchControls;
                            controlValues[channel] &= -3;
                        }
                    }

                    if (data1 == 99) {
                        midiSynth.dataEntriesMSB[channel] = (data2 << 7) + (midiSynth.dataEntriesMSB[channel] & 127);
                    }

                    if (data1 == 98) {
                        midiSynth.dataEntriesMSB[channel] = (midiSynth.dataEntriesMSB[channel] & 16256) + data2;
                    }

                    if (data1 == 101) {
                        midiSynth.dataEntriesMSB[channel] = (data2 << 7) + (midiSynth.dataEntriesMSB[channel] & 127) + 16384;
                    }

                    if (data1 == 100) {
                        midiSynth.dataEntriesMSB[channel] = (midiSynth.dataEntriesMSB[channel] & 16256) + data2 + 16384;
                    }

                    if (data1 == 120) {
                        midiSynth.allSoundOff(channel);
                    }

                    /*
                    if (data1 == 121) {
                        midiSynth.resetAllControllers(channel);
                    }
                     */

                    int value;
                    if (data1 == 6) {
                        value = midiSynth.dataEntriesMSB[channel];
                        if (value == 16384) {
                            midiSynth.dataEntriesLSB[channel] = (data2 << 7) + (midiSynth.dataEntriesLSB[channel] & -16257);
                        }
                    }

                    if (data1 == 38) {
                        value = midiSynth.dataEntriesMSB[channel];
                        if (value == 16384) {
                            midiSynth.dataEntriesLSB[channel] = data2 + (midiSynth.dataEntriesLSB[channel] & -128);
                        }
                    }

                    if (data1 == 16) {
                        midiSynth.sampleLoopControls[channel] = (data2 << 7) + (midiSynth.sampleLoopControls[channel] & -16257);
                    }

                    if (data1 == 48) {
                        midiSynth.sampleLoopControls[channel] = data2 + (midiSynth.sampleLoopControls[channel] & -128);
                    }

                    if (data1 == 81) {
                        if (data2 >= 64) {
                            controlValues = midiSynth.switchControls;
                            controlValues[channel] |= 4;
                        } else {
                            midiSynth.setReTriggerSwitch(channel);
                            controlValues = midiSynth.switchControls;
                            controlValues[channel] &= -5;
                        }
                    }

                    if (data1 == 17) {
                        midiSynth.reTrigger(channel, (data2 << 7) + (midiSynth.reTriggerControls[channel] & -16257));
                    }

                    if (data1 == 49) {
                        midiSynth.reTrigger(channel, data2 + (midiSynth.reTriggerControls[channel] & -128));
                    }

                } else if (command == 192) {
                    midiSynth.programChange(channel, data1 + midiSynth.bankControls[channel]);
                } else if (command == 224) {
                    midiSynth.pitchBend(channel, (data1 + data2 * 128) & 16256);
                } else {
                    if (command == 255) {
                        midiSynth.systemReset();
                    }
                }
            }
        }

    }

    /**
     * An overridden method from the Receiver class to stop and close the MIDI synthesizer.
     */
    @Override
    public void close() {
        if (midiSynth != null) {
            midiSynth = null;
        }
    }

}
