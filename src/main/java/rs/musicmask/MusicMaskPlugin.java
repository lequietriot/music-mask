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

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import javax.sound.midi.*;
import javax.sound.sampled.LineUnavailableException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@PluginDescriptor(
        enabledByDefault = false,
        name = "Music Mask",
        description = "Allows you to customize how the game music sounds",
        tags = {"sound", "music", "custom"}
)
public class MusicMaskPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private MusicMaskConfig musicMaskConfig;

    private Soundbank currentSoundBank;

    private String currentSong;

    public static MusicPlayer musicPlayer;

    @Override
    protected void startUp() throws Exception
    {
        initMusicPlayer();

        if (client.getWidget(WidgetInfo.MUSIC_WINDOW) != null)
        {
            Widget musicPlayingWidget = client.getWidget(WidgetID.MUSIC_GROUP_ID, 6);

            if (musicPlayingWidget != null)
            {
                currentSong = musicPlayingWidget.getText();
                musicPlayer.play(getMidiSequence(musicMaskConfig.getDefaultLoginMusic()));
            }
        }
        else
        {
            currentSong = musicMaskConfig.getDefaultLoginMusic();
            musicPlayer.play(getMidiSequence(musicMaskConfig.getDefaultLoginMusic()));
        }
    }

    private void initMusicPlayer() throws InvalidMidiDataException, IOException, MidiUnavailableException, LineUnavailableException {
        initConfiguration();
        musicPlayer = new MusicPlayer();
        musicPlayer.init(currentSoundBank);
    }

    @Provides
    MusicMaskConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MusicMaskConfig.class);
    }

    private void initConfiguration() throws InvalidMidiDataException, IOException
    {
        File soundBankResource = new File(musicMaskConfig.getCustomSoundBankPath());
        currentSoundBank = MidiSystem.getSoundbank(soundBankResource);
    }

    private Sequence getMidiSequence(String songName) throws InvalidMidiDataException, IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] midiData = getSongID(songName);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(midiData);
        MidiSystem.write(MidiSystem.getSequence(byteArrayInputStream), 1, byteArrayOutputStream);

        return MidiSystem.getSequence(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
    }

    public byte[] getSongID(String songName)
    {
        MusicTrackMapping musicTrackID = new MusicTrackMapping();
        if (musicTrackID.musicTracks.get(songName) != null) {
            int archiveID = musicTrackID.musicTracks.get(songName);
            {
                byte[] contents = client.getIndex(6).loadData(archiveID, 0);

                if (contents != null) {
                    TrackLoader loader = new TrackLoader();
                    TrackDefinition def = loader.load(contents);
                    return def.midi;
                }
            }
        }
        return null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) throws InvalidMidiDataException, IOException, MidiUnavailableException {
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            musicPlayer.stop();
        }

        if (client.getGameState() == GameState.LOGIN_SCREEN)
        {
            currentSong = musicMaskConfig.getDefaultLoginMusic();
            musicPlayer.play(getMidiSequence(musicMaskConfig.getDefaultLoginMusic()));
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) throws InvalidMidiDataException, MidiUnavailableException, LineUnavailableException, IOException {
        if (configChanged.getGroup().equals("musicMask")) {
            if (configChanged.getKey().equals("soundBank")) {
                musicPlayer.stop();
                initMusicPlayer();
                if (getMidiSequence(currentSong) != null) {
                    musicPlayer.play(getMidiSequence(currentSong));
                }
            }
            if (configChanged.getKey().equals("defaultLoginMusic")) {
                if (client.getGameState() == GameState.LOGIN_SCREEN)
                {
                    currentSong = configChanged.getNewValue();
                    fadeToSong(currentSong);
                }
            }
        }
    }

    private void fadeToSong(String currentSong) {
        new Thread(() -> {
            musicPlayer.stop();
            try {
                initMusicPlayer();
                if (getMidiSequence(currentSong) != null) {
                    musicPlayer.play(getMidiSequence(currentSong));
                }
            } catch (InvalidMidiDataException | MidiUnavailableException | LineUnavailableException | IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
        Widget musicPlayingWidget = client.getWidget(WidgetID.MUSIC_GROUP_ID, 6);

        if (musicPlayingWidget != null)
        {
            if (!musicPlayingWidget.getText().equals(currentSong))
            {
                currentSong = musicPlayingWidget.getText();
                fadeToSong(currentSong);
            }
        }
    }

    @Override
    protected void shutDown() {
        if (musicPlayer != null)
        {
            musicPlayer.stop();
        }
    }
}
