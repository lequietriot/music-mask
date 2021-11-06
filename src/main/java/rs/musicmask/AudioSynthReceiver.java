/*
 * Copyright (c) 2021, Rodolfo Ruiz-Velasco <https://github.com/lequietriot>
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

import com.sun.media.sound.AudioSynthesizer;

import javax.sound.midi.*;

public class AudioSynthReceiver implements Receiver {

    int bank;

    AudioSynthesizer audioSynthesizer;

    public AudioSynthReceiver(AudioSynthesizer synthesizer)
    {
        audioSynthesizer = synthesizer;
    }

    @Override
    public void send(MidiMessage message, long timeStamp)
    {
        if (message instanceof ShortMessage)
        {
            ShortMessage shortMessage = (ShortMessage) message;
            int command = shortMessage.getCommand();
            int channel = shortMessage.getChannel();
            int data1 = shortMessage.getData1();
            int data2 = shortMessage.getData2();

            MidiChannel midiChannel = audioSynthesizer.getChannels()[channel];

            if (command == ShortMessage.CONTROL_CHANGE)
            {
                if (data1 == 32)
                {
                    bank = data2;
                }
                else if (data1 != 0)
                {
                    midiChannel.controlChange(data1, data2);
                }
            }
            if (command == ShortMessage.PROGRAM_CHANGE)
            {
                midiChannel.programChange(bank * 128, data1);
            }
            else {
                try {
                    audioSynthesizer.getReceiver().send(message, timeStamp);
                } catch (MidiUnavailableException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void close()
    {
        try {
            audioSynthesizer.getReceiver().close();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }
}
