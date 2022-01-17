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

public class MusicPlayer
{
    public Sequencer sequencer;
    public CustomReceiver customReceiver;

    public void init(Soundbank soundbank) {
        try {
            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();

            CustomSynthesizer customSynthesizer0 = new CustomSynthesizer(MidiSystem.getSynthesizer());
            customSynthesizer0.open();
            customSynthesizer0.unloadAllInstruments(customSynthesizer0.getDefaultSoundbank());
            customSynthesizer0.loadAllInstruments(soundbank);

            CustomSynthesizer customSynthesizer1 = new CustomSynthesizer(MidiSystem.getSynthesizer());
            customSynthesizer1.open();
            customSynthesizer1.unloadAllInstruments(customSynthesizer1.getDefaultSoundbank());
            customSynthesizer1.loadAllInstruments(soundbank);

            customReceiver = new CustomReceiver(customSynthesizer0, customSynthesizer1);

            sequencer.getTransmitter().setReceiver(customReceiver);

        } catch (MidiUnavailableException e)
        {
            e.printStackTrace();
        }
    }

    public void play(Sequence sequence) throws InvalidMidiDataException, MidiUnavailableException
    {
        if (sequencer != null)
        {
            if (!sequencer.isOpen())
            {
                sequencer.open();
            }
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(-1);
            sequencer.start();
        }
    }

    public void stop()
    {
        if (sequencer.isOpen())
        {
            sequencer.stop();
            sequencer.close();
        }
        try {
            if (sequencer.getReceiver() != null)
            {
                sequencer.getReceiver().close();
            }
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }
}
