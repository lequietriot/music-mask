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

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A class which decodes the RuneScape sequence data to a readable MIDI format.
 */
public class MidiTrackLoader {

    /**
     * The decoded MIDI Sequence data.
     */
    public Sequence midiSequence;

    /**
     * The class to decode RuneScape's encoded MIDI data.
     * @param inputStream initialize the decoder with the encoded data stream.
     */
    public MidiTrackLoader(InputStream inputStream) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(inputStream.available());
            for (int index = 0; index < byteBuffer.capacity(); index++) {
                byteBuffer.put((byte) inputStream.read());
            }
            convertToMidi(byteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * The method to decode RuneScape's custom encoded MIDI data to a readable MIDI format.
     * @param byteBuffer The buffer storing encoded MIDI data
     */
    public void convertToMidi(ByteBuffer byteBuffer) {
        byteBuffer.position(byteBuffer.limit() - 3);
        int tracks = byteBuffer.get() & 0xFF;
        int division = byteBuffer.getShort() & 0xFFFF;
        int length = 14 + tracks * 10;
        byteBuffer.position(0);
        int tempoCount = 0;
        int controlChangeCount = 0;
        int noteOnCount = 0;
        int noteOffCount = 0;
        int pitchBendCount = 0;
        int channelPressureCount = 0;
        int keyAftertouchCount = 0;
        int programChangeCount = 0;

        int track;
        int opcode;
        int eventCount;
        for (track = 0; track < tracks; ++track) {
            opcode = -1;

            while (true) {
                eventCount = byteBuffer.get() & 0xFF;
                if (eventCount != opcode) {
                    ++length;
                }

                opcode = eventCount & 15;
                if (eventCount == 7) {
                    break;
                }

                if (eventCount == 23) {
                    ++tempoCount;
                } else if (opcode == 0) {
                    ++noteOnCount;
                } else if (opcode == 1) {
                    ++noteOffCount;
                } else if (opcode == 2) {
                    ++controlChangeCount;
                } else if (opcode == 3) {
                    ++pitchBendCount;
                } else if (opcode == 4) {
                    ++channelPressureCount;
                } else if (opcode == 5) {
                    ++keyAftertouchCount;
                } else {
                    if (opcode != 6) {
                        throw new RuntimeException();
                    }

                    ++programChangeCount;
                }
            }
        }

        length += 5 * tempoCount;
        length += 2 * (noteOnCount + noteOffCount + controlChangeCount + pitchBendCount + keyAftertouchCount);
        length += channelPressureCount + programChangeCount;
        track = byteBuffer.position();
        opcode = tracks + tempoCount + controlChangeCount + noteOnCount + noteOffCount + pitchBendCount
                + channelPressureCount + keyAftertouchCount + programChangeCount;

        for (eventCount = 0; eventCount < opcode; ++eventCount) {
            readVarInt(byteBuffer);
        }

        length += byteBuffer.position() - track;
        eventCount = byteBuffer.position();
        int modulationMSBCount = 0;
        int modulationLSBCount = 0;
        int channelVolumeMSBCount = 0;
        int channelVolumeLSBCount = 0;
        int channelPanningMSBCount = 0;
        int channelPanningLSBCount = 0;
        int NRPNMSBCount = 0;
        int NRPNLSBCount = 0;
        int RPNMSBCount = 0;
        int RPNLSBCount = 0;
        int miscEventCount = 0;
        int toggleCount = 0;
        int controller = 0;

        int controllerCount;
        for (controllerCount = 0; controllerCount < controlChangeCount; ++controllerCount) {
            controller = controller + (byteBuffer.get() & 0xFF) & 127;
            if (controller != 0 && controller != 32) {
                if (controller == 1) {
                    ++modulationMSBCount;
                } else if (controller == 33) {
                    ++modulationLSBCount;
                } else if (controller == 7) {
                    ++channelVolumeMSBCount;
                } else if (controller == 39) {
                    ++channelVolumeLSBCount;
                } else if (controller == 10) {
                    ++channelPanningMSBCount;
                } else if (controller == 42) {
                    ++channelPanningLSBCount;
                } else if (controller == 99) {
                    ++NRPNMSBCount;
                } else if (controller == 98) {
                    ++NRPNLSBCount;
                } else if (controller == 101) {
                    ++RPNMSBCount;
                } else if (controller == 100) {
                    ++RPNLSBCount;
                } else if (controller != 64 && controller != 65 && controller != 120 && controller != 121 && controller != 123) {
                    ++toggleCount;
                } else {
                    ++miscEventCount;
                }
            } else {
                ++programChangeCount;
            }
        }

        controllerCount = 0;

        int miscEventOffset = byteBuffer.position();
        skip(byteBuffer, miscEventCount);

        int keyPressureOffset = byteBuffer.position();
        skip(byteBuffer, keyAftertouchCount);

        int channelPressureOffset = byteBuffer.position();
        skip(byteBuffer, channelPressureCount);

        int pitchBendOffset = byteBuffer.position();
        skip(byteBuffer, pitchBendCount);

        int modulationMSBOffset = byteBuffer.position();
        skip(byteBuffer, modulationMSBCount);

        int channelVolumeMSBOffset = byteBuffer.position();
        skip(byteBuffer, channelVolumeMSBCount);

        int channelPanningMSBOffset = byteBuffer.position();
        skip(byteBuffer, channelPanningMSBCount);

        int pitchOffset = byteBuffer.position();
        skip(byteBuffer, noteOnCount + noteOffCount + keyAftertouchCount);

        int noteOnOffset = byteBuffer.position();
        skip(byteBuffer, noteOnCount);

        int toggleOffset = byteBuffer.position();
        skip(byteBuffer, toggleCount);

        int noteOffOffset = byteBuffer.position();
        skip(byteBuffer, noteOffCount);

        int modulationLSBOffset = byteBuffer.position();
        skip(byteBuffer, modulationLSBCount);

        int channelVolumeLSBOffset = byteBuffer.position();
        skip(byteBuffer, channelVolumeLSBCount);

        int channelPanningLSBOffset = byteBuffer.position();
        skip(byteBuffer, channelPanningLSBCount);

        int programChangeOffset = byteBuffer.position();
        skip(byteBuffer, programChangeCount);

        int pitchBend2Offset = byteBuffer.position();
        skip(byteBuffer, pitchBendCount);

        int NRPNMSBOffset = byteBuffer.position();
        skip(byteBuffer, NRPNMSBCount);

        int NRPNLSBOffset = byteBuffer.position();
        skip(byteBuffer, NRPNLSBCount);

        int RPNMSBOffset = byteBuffer.position();
        skip(byteBuffer, RPNMSBCount);

        int RPNLSBOffset = byteBuffer.position();
        skip(byteBuffer, RPNLSBCount);

        int tempoOffset = byteBuffer.position();
        skip(byteBuffer, tempoCount * 3);

        ByteBuffer midiBuff = ByteBuffer.allocate(length + 1);

        midiBuff.putInt(1297377380);
        midiBuff.putInt(6);
        midiBuff.putShort((short) (tracks > 1 ? 1 : 0));
        midiBuff.putShort((short) tracks);
        midiBuff.putShort((short) division);

        byteBuffer.position(track);

        int messagePosition = 0;
        int pitchPosition = 0;
        int noteOnPosition = 0;
        int noteOffPosition = 0;
        int pitchBendPositions = 0;
        int channelPressurePosition = 0;
        int keyPressurePosition = 0;
        int[] controllerArray = new int[128];
        controller = 0;

        label: for (int trackIndex = 0; trackIndex < tracks; ++trackIndex) {
            midiBuff.putInt(1297379947);
            skip(midiBuff, 4);
            int currentPosition = midiBuff.position();
            int currentOffset = -1;

            while (true) {

                int varInt = readVarInt(byteBuffer);

                writeVarInt(midiBuff, varInt);

                int controllerValue = byteBuffer.array()[controllerCount++] & 255;
                boolean messageExists = controllerValue != currentOffset;
                currentOffset = controllerValue & 15;
                if (controllerValue == 7) {
                    {
                        midiBuff.put((byte) 255);
                    }

                    midiBuff.put((byte) 47);
                    midiBuff.put((byte) 0);
                    writeLengthFromMark(midiBuff, midiBuff.position() - currentPosition);
                    continue label;
                }

                if (controllerValue == 23) {
                    {
                        midiBuff.put((byte) 255);
                    }

                    midiBuff.put((byte) 81);
                    midiBuff.put((byte) 3);
                    midiBuff.put(byteBuffer.array()[tempoOffset++]);
                    midiBuff.put(byteBuffer.array()[tempoOffset++]);
                    midiBuff.put(byteBuffer.array()[tempoOffset++]);
                } else {
                    messagePosition ^= controllerValue >> 4;
                    if (currentOffset == 0) {
                        if (messageExists) {
                            midiBuff.put((byte) (144 + messagePosition));
                        }

                        pitchPosition += byteBuffer.array()[pitchOffset++];
                        noteOnPosition += byteBuffer.array()[noteOnOffset++];
                        midiBuff.put((byte) (pitchPosition & 127));
                        midiBuff.put((byte) (noteOnPosition & 127));
                    } else if (currentOffset == 1) {
                        if (messageExists) {
                            midiBuff.put((byte) (128 + messagePosition));
                        }

                        pitchPosition += byteBuffer.array()[pitchOffset++];
                        noteOffPosition += byteBuffer.array()[noteOffOffset++];
                        midiBuff.put((byte) (pitchPosition & 127));
                        midiBuff.put((byte) (noteOffPosition & 127));
                    } else if (currentOffset == 2) {
                        if (messageExists) {
                            midiBuff.put((byte) (176 + messagePosition));
                        }

                        controller = controller + byteBuffer.array()[eventCount++] & 127;
                        midiBuff.put((byte) controller);
                        byte controllerData;
                        if (controller != 0 && controller != 32) {
                            if (controller == 1) {
                                controllerData = byteBuffer.array()[modulationMSBOffset++];
                            } else if (controller == 33) {
                                controllerData = byteBuffer.array()[modulationLSBOffset++];
                            } else if (controller == 7) {
                                controllerData = byteBuffer.array()[channelVolumeMSBOffset++];
                            } else if (controller == 39) {
                                controllerData = byteBuffer.array()[channelVolumeLSBOffset++];
                            } else if (controller == 10) {
                                controllerData = byteBuffer.array()[channelPanningMSBOffset++];
                            } else if (controller == 42) {
                                controllerData = byteBuffer.array()[channelPanningLSBOffset++];
                            } else if (controller == 99) {
                                controllerData = byteBuffer.array()[NRPNMSBOffset++];
                            } else if (controller == 98) {
                                controllerData = byteBuffer.array()[NRPNLSBOffset++];
                            } else if (controller == 101) {
                                controllerData = byteBuffer.array()[RPNMSBOffset++];
                            } else if (controller == 100) {
                                controllerData = byteBuffer.array()[RPNLSBOffset++];
                            } else if (controller != 64 && controller != 65 && controller != 120 && controller != 121 && controller != 123) {
                                controllerData = byteBuffer.array()[toggleOffset++];
                            } else {
                                controllerData = byteBuffer.array()[miscEventOffset++];
                            }
                        } else {
                            controllerData = byteBuffer.array()[programChangeOffset++];
                        }

                        int controllerInfo = controllerData + controllerArray[controller];
                        controllerArray[controller] = controllerInfo;
                        midiBuff.put((byte) (controllerInfo & 127));
                    } else if (currentOffset == 3) {
                        if (messageExists) {
                            midiBuff.put((byte) (224 + messagePosition));
                        }

                        pitchBendPositions += byteBuffer.array()[pitchBend2Offset++];
                        pitchBendPositions += byteBuffer.array()[pitchBendOffset++] << 7;
                        midiBuff.put((byte) (pitchBendPositions & 127));
                        midiBuff.put((byte) (pitchBendPositions >> 7 & 127));
                    } else if (currentOffset == 4) {
                        if (messageExists) {
                            midiBuff.put((byte) (208 + messagePosition));
                        }

                        channelPressurePosition += byteBuffer.array()[channelPressureOffset++];
                        midiBuff.put((byte) (channelPressurePosition & 127));
                    } else if (currentOffset == 5) {
                        if (messageExists) {
                            midiBuff.put((byte) (160 + messagePosition));
                        }

                        pitchPosition += byteBuffer.array()[pitchOffset++];
                        keyPressurePosition += byteBuffer.array()[keyPressureOffset++];
                        midiBuff.put((byte) (pitchPosition & 127));
                        midiBuff.put((byte) (keyPressurePosition & 127));
                    } else {
                        if (currentOffset != 6) {
                            throw new RuntimeException();
                        }

                        if (messageExists) {
                            midiBuff.put((byte) (192 + messagePosition));
                        }

                        midiBuff.put(byteBuffer.array()[programChangeOffset++]);
                    }
                }
            }
        }

        midiBuff.flip();

        try {
            midiSequence = MidiSystem.getSequence(new ByteArrayInputStream(midiBuff.array()));
        } catch (InvalidMidiDataException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Modified method from RuneLite's OutputStream class.
     */
    private static void writeLengthFromMark(ByteBuffer byteBuffer, int var1) {
        byteBuffer.array()[byteBuffer.position() - var1 - 4] = (byte) (var1 >> 24);
        byteBuffer.array()[byteBuffer.position() - var1 - 3] = (byte) (var1 >> 16);
        byteBuffer.array()[byteBuffer.position() - var1 - 2] = (byte) (var1 >> 8);
        byteBuffer.array()[byteBuffer.position() - var1 - 1] = (byte) var1;
    }

    /**
     * Modified method from RuneLite's OutputStream class.
     */
    private static void writeVarInt(ByteBuffer byteBuffer, int var1) {

        if ((var1 & -128) != 0)
        {
            if ((var1 & -16384) != 0)
            {
                if ((var1 & -2097152) != 0)
                {
                    if ((var1 & -268435456) != 0)
                    {
                        byteBuffer.put((byte) (var1 >>> 28 | 128));
                    }

                    byteBuffer.put((byte) (var1 >>> 21 | 128));
                }

                byteBuffer.put((byte) (var1 >>> 14 | 128));
            }

            byteBuffer.put((byte) (var1 >>> 7 | 128));
        }

        byteBuffer.put((byte) (var1 & 127));
    }

    /**
     * Modified method from RuneLite's InputStream class.
     */
    private static int readVarInt(ByteBuffer byteBuffer) {

        byte var1 = byteBuffer.get();

        int var2;
        for (var2 = 0; var1 < 0; var1 = byteBuffer.get())
        {
            var2 = (var2 | var1 & 127) << 7;
        }

        return var2 | var1;
    }

    /**
     * Modified method from RuneLite's InputStream class.
     */
    private static void skip(ByteBuffer byteBuffer, int length) {
        byteBuffer.position(byteBuffer.position() + length);
    }

    public Sequence getMidiSequence() {
        return midiSequence;
    }
}