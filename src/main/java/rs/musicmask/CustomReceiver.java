/*
 * Copyright (c) 2022, Rodolfo Ruiz-Velasco <https://github.com/lequietriot>
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
package rs.musicmask;

import javax.sound.midi.*;

public class CustomReceiver implements Receiver {

    public CustomSynthesizer[] customSynthesizers;

    CustomReceiver(CustomSynthesizer[] synthesizers)
    {
        customSynthesizers = synthesizers;
    }

    @Override
    public void send(MidiMessage message, long timeStamp)
    {
        if (message instanceof ShortMessage)
        {
            ShortMessage shortMessage = (ShortMessage) message;
            MidiChannel currentChannel;
            if (shortMessage.getChannel() == 9)
            {
                currentChannel = customSynthesizers[shortMessage.getChannel()].getChannels()[0];
                try {
                    shortMessage = new ShortMessage(shortMessage.getCommand(), 0, shortMessage.getData1(), shortMessage.getData2());
                    if (shortMessage.getCommand() == ShortMessage.PROGRAM_CHANGE)
                    {
                        currentChannel.programChange(shortMessage.getData1());
                    }
                    if (shortMessage.getCommand() == ShortMessage.NOTE_OFF)
                    {
                        currentChannel.noteOff(shortMessage.getData1(), shortMessage.getData2());
                    }
                    if (shortMessage.getCommand() == ShortMessage.NOTE_ON)
                    {
                        currentChannel.noteOn(shortMessage.getData1(), shortMessage.getData2());
                    }
                    if (shortMessage.getCommand() == ShortMessage.CONTROL_CHANGE)
                    {
                        if (shortMessage.getData1() != 0 && shortMessage.getData1() != 32)
                        {
                            currentChannel.programChange(128, currentChannel.getProgram());
                            currentChannel.controlChange(shortMessage.getData1(), shortMessage.getData2());
                        }
                        if (shortMessage.getData1() == 32)
                        {
                            currentChannel.programChange(shortMessage.getData2() * 128, currentChannel.getProgram());
                        }
                    }
                    if (shortMessage.getCommand() == ShortMessage.POLY_PRESSURE)
                    {
                        currentChannel.setPolyPressure(shortMessage.getData1(), shortMessage.getData2());
                    }
                    if (shortMessage.getCommand() == ShortMessage.CHANNEL_PRESSURE)
                    {
                        currentChannel.setChannelPressure(shortMessage.getData1());
                    }
                    if (shortMessage.getCommand() == ShortMessage.PITCH_BEND)
                    {
                        currentChannel.setPitchBend(shortMessage.getData1() + shortMessage.getData2() * 128);
                    }
                } catch (InvalidMidiDataException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                currentChannel = customSynthesizers[shortMessage.getChannel()].getChannels()[shortMessage.getChannel()];
                if (shortMessage.getCommand() == ShortMessage.PROGRAM_CHANGE)
                {
                    currentChannel.programChange(shortMessage.getData1());
                }
                if (shortMessage.getCommand() == ShortMessage.NOTE_OFF)
                {
                    currentChannel.noteOff(shortMessage.getData1(), shortMessage.getData2());
                }
                if (shortMessage.getCommand() == ShortMessage.NOTE_ON)
                {
                    currentChannel.noteOn(shortMessage.getData1(), shortMessage.getData2());
                }
                if (shortMessage.getCommand() == ShortMessage.CONTROL_CHANGE)
                {
                    if (shortMessage.getData1() != 0 && shortMessage.getData1() != 32)
                    {
                        currentChannel.controlChange(shortMessage.getData1(), shortMessage.getData2());
                    }
                    if (shortMessage.getData1() == 32)
                    {
                        currentChannel.programChange(shortMessage.getData2() * 128, currentChannel.getProgram());
                    }
                }
                if (shortMessage.getCommand() == ShortMessage.POLY_PRESSURE)
                {
                    currentChannel.setPolyPressure(shortMessage.getData1(), shortMessage.getData2());
                }
                if (shortMessage.getCommand() == ShortMessage.CHANNEL_PRESSURE)
                {
                    currentChannel.setChannelPressure(shortMessage.getData1());
                }
                if (shortMessage.getCommand() == ShortMessage.PITCH_BEND)
                {
                    currentChannel.setPitchBend(shortMessage.getData1() + shortMessage.getData2() * 128);
                }
            }
        }
    }

    @Override
    public void close() {
        for (CustomSynthesizer customSynthesizer : customSynthesizers)
        {
            customSynthesizer.close();
        }
    }
}
