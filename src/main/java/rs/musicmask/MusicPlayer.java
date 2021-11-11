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

import javax.sound.midi.*;

public class MusicPlayer
{
    public Sequencer sequencer;
    public CustomReceiver customReceiver;

    public void init(Soundbank soundbank) {
        try {
            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();

            CustomSynthesizer customSynthesizer = new CustomSynthesizer(MidiSystem.getSynthesizer());
            customSynthesizer.open();
            customSynthesizer.unloadAllInstruments(customSynthesizer.getDefaultSoundbank());
            customSynthesizer.loadAllInstruments(soundbank);

            customReceiver = new CustomReceiver(customSynthesizer);

            sequencer.getTransmitter().setReceiver(customReceiver);

        } catch (MidiUnavailableException e)
        {
            e.printStackTrace();
        }
    }

    public void play(Sequence sequence, boolean newSong) throws InvalidMidiDataException, MidiUnavailableException
    {
        if (sequencer != null)
        {
            if (!sequencer.isOpen())
            {
                sequencer.open();
            }
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(-1);

            if (MusicMaskPlugin.pausedPosition > 0 && !newSong)
            {
                sequencer.setMicrosecondPosition(MusicMaskPlugin.pausedPosition);
            }
            sequencer.start();
        }
    }

    public void stop()
    {
        MusicMaskPlugin.pausedPosition = sequencer.getMicrosecondPosition();
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
