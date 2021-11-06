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
import javax.sound.sampled.*;
import java.util.HashMap;
import java.util.Map;

public class MusicPlayer
{
    public SourceDataLine sourceDataLine;
    public DataLine.Info dataLineInfo;
    public Sequencer sequencer;

    public void init(Soundbank soundbank, boolean highQuality, String resamplerType, boolean stereo, boolean reverb, boolean chorus, boolean autoGainControl) {
        try {
            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();

            AudioFormat audioFormat;

            if (highQuality) {
                audioFormat = new AudioFormat(44100, 16, (stereo ? 2 : 1), true, false);
            } else {
                audioFormat = new AudioFormat(22050, 16, (stereo ? 2 : 1), true, false);
            }

            Map<String, Object> synthProperties = new HashMap<>();
            synthProperties.put("Interpolation", resamplerType);
            synthProperties.put("Control Rate", -1);
            synthProperties.put("Format", audioFormat);
            synthProperties.put("Latency", -1);
            synthProperties.put("Max Polyphony", 1024);
            synthProperties.put("Reverb", reverb);
            synthProperties.put("Chorus", chorus);
            synthProperties.put("Auto Gain Control", autoGainControl);
            synthProperties.put("Large Mode", true);

            dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);

            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            sourceDataLine.open();
            sourceDataLine.start();

            MusicMaskPlugin.audioSynthesizer = (AudioSynthesizer) MidiSystem.getSynthesizer();
            MusicMaskPlugin.audioSynthesizer.open(sourceDataLine, synthProperties);
            MusicMaskPlugin.audioSynthesizer.unloadAllInstruments(MusicMaskPlugin.audioSynthesizer.getDefaultSoundbank());
            MusicMaskPlugin.audioSynthesizer.loadAllInstruments(soundbank);

            sequencer.getTransmitter().setReceiver(MusicMaskPlugin.audioSynthesizer.getReceiver());

        } catch (MidiUnavailableException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void fadeOut()
    {
        MusicMaskPlugin.fading = true;
        FloatControl gainControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
        if (sourceDataLine.isControlSupported(gainControl.getType()))
        {
            try
            {
                for (float gainValue = gainControl.getValue(); gainValue > gainControl.getMinimum(); gainValue--)
                {
                    gainControl.setValue(gainValue);
                    Thread.sleep(50);
                    if (gainValue <= -60.0)
                    {
                        stop();
                        MusicMaskPlugin.fading = false;
                    }
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void play(Sequence sequence, boolean newSong) throws InvalidMidiDataException, MidiUnavailableException
    {
        if (sequencer != null)
        {
            byte[] XGModeOn = new byte[]{(byte) 0xF0, (byte) 0x43, (byte) 0x10, (byte) 0x4C, (byte) 0x00, (byte) 0x00, (byte) 0x7E, (byte) 0x00, (byte) 0xF7};
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
            sequencer.getReceiver().send(new SysexMessage(XGModeOn, XGModeOn.length), -1);
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
        if (sourceDataLine.isOpen())
        {
            sourceDataLine.stop();
            sourceDataLine.flush();
            sourceDataLine.close();
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
