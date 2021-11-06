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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("musicMask")
public interface MusicMaskConfig extends Config
{
    @ConfigItem(
            position = 0,
            keyName = "soundBank",
            name = "SoundFont",
            description = "Set the SoundFont version"
    )
    default SoundSets getSoundSet()
    {
        return SoundSets.RUNESCAPE_2;
    }

    @ConfigItem(
            position = 1,
            keyName = "highQuality",
            name = "High Quality Mode",
            description = "Enable higher quality sound"
    )
    default boolean getHighQuality()
    {
        return true;
    }

    @ConfigItem(
            position = 2,
            keyName = "resamplerType",
            name = "Resampler Type",
            description = "Set the Resampler Type"
    )
    default ResamplerTypes getResamplerType()
    {
        return ResamplerTypes.SINC;
    }

    @ConfigItem(
            position = 3,
            keyName = "stereoMode",
            name = "Stereo Mode",
            description = "Enable Stereo Sound"
    )
    default boolean getStereoMode()
    {
        return true;
    }

    @ConfigItem(
            position = 4,
            keyName = "enableReverb",
            name = "Enable Reverb",
            description = "Enable Reverberated Sound"
    )
    default boolean getReverbMode()
    {
        return false;
    }

    @ConfigItem(
            position = 5,
            keyName = "enableChorus",
            name = "Enable Chorus",
            description = "Enable Chorused Sound"
    )
    default boolean getChorusMode()
    {
        return false;
    }

    @ConfigItem(
            position = 6,
            keyName = "autoGainControl",
            name = "Auto Gain Control",
            description = "Enable Auto Gain Control"
    )
    default boolean getAutoGainControl()
    {
        return false;
    }

    @ConfigItem(
            position = 7,
            keyName = "defaultLoginMusic",
            name = "Default Login Music",
            description = "Set the default login screen music"
    )
    default String getDefaultLoginMusic()
    {
        return "Scape Main";
    }

    @ConfigItem(
            position = 8,
            keyName = "defaultCustomSoundBank",
            name = "Default Custom SoundFont Path",
            description = "Set the default custom SoundBank from a local file path"
    )
    default String getCustomSoundBankPath()
    {
        return System.getProperty("user.home") + "/Downloads/Custom.sf2";
    }
}
