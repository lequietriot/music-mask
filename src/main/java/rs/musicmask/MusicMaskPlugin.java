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
import com.ibm.realtime.synth.test.SoundFont2Synth;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

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
    private ClientThread clientThread;

    @Inject
    private MusicMaskConfig musicMaskConfig;

    private int clientVolume;

    private boolean isJingle;

    private int currentTrackId;


    @Override
    protected void startUp()
    {
        currentTrackId = client.getMusicCurrentTrackId();
        isJingle = client.isPlayingJingle();
        clientVolume = client.getMusicVolume();
        initSoundSynth();
    }

    private void initSoundSynth() {
        clientThread.invoke(() -> {
            try {
                if (isJingle && currentTrackId != -1) {
                    TrackLoader trackLoader = new TrackLoader();
                    TrackDefinition trackDefinition = trackLoader.load(client.getIndex(11).loadData(currentTrackId, 0));
                    new Thread(() -> {
                        try {
                            SoundFont2Synth.start(musicMaskConfig.getCustomSoundBankPath(), trackDefinition.getMidi(), musicMaskConfig.getMusicVolume());
                            System.out.println(currentTrackId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    if (client.getGameState().equals(GameState.LOGIN_SCREEN)) {
                        TrackLoader trackLoader = new TrackLoader();
                        TrackDefinition trackDefinition = trackLoader.load(client.getIndex(6).loadData(0, 0));
                        new Thread(() -> {
                            try {
                                SoundFont2Synth.start(musicMaskConfig.getCustomSoundBankPath(), trackDefinition.getMidi(), musicMaskConfig.getMusicVolume());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                    else {
                        if (currentTrackId != -1) {
                            TrackLoader trackLoader = new TrackLoader();
                            TrackDefinition trackDefinition = trackLoader.load(client.getIndex(6).loadData(currentTrackId, 0));
                            new Thread(() -> {
                                try {
                                    SoundFont2Synth.start(musicMaskConfig.getCustomSoundBankPath(), trackDefinition.getMidi(), musicMaskConfig.getMusicVolume());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        }
                        else {
                            TrackLoader trackLoader = new TrackLoader();
                            TrackDefinition trackDefinition = trackLoader.load(client.getIndex(6).loadData(147, 0));
                            new Thread(() -> {
                                try {
                                    SoundFont2Synth.start(musicMaskConfig.getCustomSoundBankPath(), trackDefinition.getMidi(), musicMaskConfig.getMusicVolume());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        }
                    }
                }
                client.setMusicVolume(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Provides
    MusicMaskConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MusicMaskConfig.class);
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        if (gameTick != null) {
            client.setMusicVolume(1);
            client.setMusicVolume(0);
            int lastTrackId = currentTrackId;
            if (client.getMusicCurrentTrackId() != lastTrackId) {
                currentTrackId = client.getMusicCurrentTrackId();
                SoundFont2Synth.stop();
                SoundFont2Synth.close();
                initSoundSynth();
            }
        }
    }

    @Subscribe
    protected void onConfigChanged(ConfigChanged configChanged) {
        if (configChanged.getKey().equals("setVolume")) {
            SoundFont2Synth.setVolume((Integer.parseInt(configChanged.getNewValue())) / 100.0);
        }
        if (configChanged.getKey().equals("setSoundFont")) {
            SoundFont2Synth.stop();
            SoundFont2Synth.close();
            initSoundSynth();
        }
    }

    @Override
    protected void shutDown() {
        SoundFont2Synth.stop();
        SoundFont2Synth.close();
        client.setMusicVolume(clientVolume);
    }
}
