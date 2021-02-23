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
package com.musicmask;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("musicMask")
public interface MusicMaskConfig extends Config
{
    enum MusicVersion
    {
        RUNESCAPE_2("RS2"),
        RUNESCAPE_OLD_SCHOOL("OSRS"),
        RUNESCAPE_HIGH_DEFINITION("RSHD"),
        RUNESCAPE_3("RS3");
        //CUSTOM("Custom");

        public String version;

        MusicVersion(String name) {
            version = name;
        }
    }

    @ConfigItem(
            position = 1,
            keyName = "musicVersion",
            name = "",
            description = ""
    )
    default MusicVersion getMusicVersion() {
        return MusicVersion.RUNESCAPE_OLD_SCHOOL;
    }

    @ConfigItem(
            position = 2,
            keyName = "musicVolume",
            name = "Music Volume",
            description = "Set the Music Volume"
    )
    default int getMusicVolume()
    {
        return 255;
    }

    @ConfigItem(
            position = 3,
            keyName = "musicLoop",
            name = "Loop Mode",
            description = "Set the Music Loop"
    )
    default boolean getLoopingMode()
    {
        return false;
    }

    @ConfigItem(
            position = 4,
            keyName = "musicShuffle",
            name = "Shuffle Mode",
            description = "Set the Music to Shuffle"
    )
    default boolean getShuffleMode() {
        return false;
    }

    @ConfigItem(
            position = 5,
            keyName = "musicOverride",
            name = "Override Login Music",
            description = "Override the login song"
    )
    default String getOverridingMusic() {
        return "";
    }
}
