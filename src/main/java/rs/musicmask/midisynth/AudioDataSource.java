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

import jcraft.jorbis.OggVorbisDecoder;
import rs.musicmask.MusicMaskPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * A class which holds the data and variables for an audio source, which can be used in music and sound effects.
 */
public class AudioDataSource {

    /**
     * A byte array containing the raw audio in 8-bit signed little-endian format.
     */
    public byte[] audioData;

    /**
     * An integer value for the raw audio sample rate.
     */
    public int sampleRate;

    /**
     * An integer value for the sample loop start position in the raw audio.
     */
    public int loopStart;

    /**
     * An integer value for the sample loop end position in the raw audio.
     */
    public int loopEnd;

    /**
     * A boolean value to determine whether the sample should loop or not.
     */
    public boolean isLooping;

    /**
     * A method to load .ogg file resources by name, decoding them to raw 8-bit audio for use.
     * @param audioName The name of the audio resource to load.
     */
    public AudioDataSource(String audioName, String soundBankVersion) {
        if (MusicMaskPlugin.class.getResourceAsStream(soundBankVersion + "/samples/" + audioName + ".ogg") != null) {
            InputStream inputStream;
            try {
                inputStream = Objects.requireNonNull(MusicMaskPlugin.class.getResourceAsStream(soundBankVersion + "/samples/" + audioName + ".ogg"));
                loadAudioSource(inputStream);
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A method that decodes .ogg files to raw 8-bit audio, mapping the values for this instance.
     * @param oggInputStream The input stream of the loaded .ogg file resource.
     */
    public void loadAudioSource(InputStream oggInputStream) {
        OggVorbisDecoder oggVorbisDecoder = new OggVorbisDecoder(oggInputStream);
        if (oggVorbisDecoder.pcmSampleData != null) {
            byte[] sampleData = new byte[oggVorbisDecoder.pcmSampleData.length / 2];
            for (int index = 0; index < sampleData.length; index++) {
                sampleData[index] = oggVorbisDecoder.pcmSampleData[index * 2 + 1];
            }
            sampleRate = oggVorbisDecoder.sampleRate;
            loopStart = oggVorbisDecoder.loopStart;
            loopEnd = oggVorbisDecoder.loopEnd;
            isLooping = loopStart != 0;

            if (sampleData.length != loopEnd) {
                byte[] soundTrimData = new byte[loopEnd];
                for (int index = 0; index < soundTrimData.length; index++) {
                    soundTrimData[index] = sampleData[index];
                }
                audioData = soundTrimData;
            }
            else {
                audioData = sampleData;
            }
        }
    }

}
