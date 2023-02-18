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
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ClientTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import rs.musicmask.midisynth.DevicePcmPlayer;
import rs.musicmask.midisynth.MidiAudioStream;
import rs.musicmask.midisynth.MidiReceiver;
import rs.musicmask.midisynth.MidiTrackLoader;

import javax.inject.Inject;
import javax.sound.midi.*;
import javax.sound.sampled.LineUnavailableException;
import java.io.ByteArrayInputStream;

@PluginDescriptor(
        enabledByDefault = false,
        name = "Music Mask",
        description = "Allows you to enhance how the game music sounds",
        tags = {"sound", "music", "custom"}
)
@Slf4j
public class MusicMaskPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private MusicMaskConfig musicMaskConfig;

    private int clientVolume;

    private int currentTrackId;

    private MidiAudioStream midiAudioStream;

    private Sequencer sequencer;

    @Override
    protected void startUp()
    {
        currentTrackId = client.getMusicCurrentTrackId();

        if (client.getGameState().equals(GameState.LOGIN_SCREEN)) {
            clientVolume = 255;
        }
        else {
            clientVolume = client.getMusicVolume();
        }
        initSoundSynth();
    }

    private void initSoundSynth() {
        currentTrackId = client.getMusicCurrentTrackId();
        clientThread.invoke(() -> {
            try {
                if (client.isPlayingJingle() && currentTrackId != -1) {
                    if (client.getIndex(11).getFileIds(currentTrackId) != null) {
                        MidiTrackLoader trackLoader = new MidiTrackLoader(new ByteArrayInputStream(client.getIndex(11).loadData(currentTrackId, 0)));
                        try {
                            //Jingles are not supported at the moment, can cause glitches.
                            //playSong(musicMaskConfig.getSoundBank().getSoundBankName(), trackLoader.getMidiSequence(), musicMaskConfig.getMusicVolume());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (!client.isPlayingJingle() && currentTrackId != -1) {
                    if (client.getIndex(6).getFileIds(currentTrackId) != null) {
                        MidiTrackLoader trackLoader = new MidiTrackLoader(new ByteArrayInputStream(client.getIndex(6).loadData(currentTrackId, 0)));
                        try {
                            playSong(musicMaskConfig.getSoundBank().getSoundBankName(), trackLoader.getMidiSequence(), musicMaskConfig.getMusicVolume());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (client.getGameState().equals(GameState.LOGIN_SCREEN)) {
                    if (client.getIndex(6).getFileIds(0) != null) {
                        MidiTrackLoader trackLoader = new MidiTrackLoader(new ByteArrayInputStream(client.getIndex(6).loadData(0, 0)));
                        try {
                            playSong(musicMaskConfig.getSoundBank().getSoundBankName(), trackLoader.getMidiSequence(), musicMaskConfig.getMusicVolume());
                        } catch (Exception e) {
                            e.printStackTrace();
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

    public void playSong(String soundBankName, Sequence midiSequence, int volume) {
        new Thread(() -> {
            if (sequencer != null && sequencer.isRunning()) {
                sequencer.stop();
                sequencer.close();
                sequencer = null;
                midiAudioStream = null;
            }

            midiAudioStream = new MidiAudioStream(soundBankName);
            midiAudioStream.setInitialPatch(9, 128);
            midiAudioStream.setPcmStreamVolume(volume);

            MidiReceiver midiReceiver = new MidiReceiver(midiAudioStream);
            try {
                sequencer = MidiSystem.getSequencer(false);
                sequencer.open();
                sequencer.getTransmitter().setReceiver(midiReceiver);
                sequencer.setSequence(midiSequence);
                sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);

                if (!sequencer.isRunning()) {
                    sequencer.start();
                }

                DevicePcmPlayer devicePcmPlayer = new DevicePcmPlayer();
                devicePcmPlayer.init();
                devicePcmPlayer.setStream(midiAudioStream);
                devicePcmPlayer.open();
                devicePcmPlayer.samples = new int[1024];
                do {
                    devicePcmPlayer.fill(devicePcmPlayer.samples, 256);
                    devicePcmPlayer.write();
                } while (sequencer != null && sequencer.isRunning());
            } catch (MidiUnavailableException | InvalidMidiDataException | LineUnavailableException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setSongVolume(int volume) {
        midiAudioStream.setPcmStreamVolume(volume);
    }

    @Subscribe
    public void onClientTick(ClientTick clientTick) {
        new Thread(() -> {
            client.setMusicVolume(1);
            client.setMusicVolume(0);
            int lastTrackId = currentTrackId;
            if (client.getMusicCurrentTrackId() != lastTrackId) {
                currentTrackId = client.getMusicCurrentTrackId();
                while (midiAudioStream != null && midiAudioStream.getVolume() > -1) {
                    try {
                        Thread.sleep(10);
                        if (midiAudioStream != null) {
                            midiAudioStream.setPcmStreamVolume(midiAudioStream.getVolume() - 1);
                            if (midiAudioStream.getVolume() == 0) {
                                if (sequencer != null && midiAudioStream != null) {
                                    sequencer.stop();
                                    sequencer.close();
                                    sequencer = null;
                                    midiAudioStream = null;
                                    initSoundSynth();
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Subscribe
    protected void onConfigChanged(ConfigChanged configChanged) {
        if (configChanged.getKey().equals("setVolume")) {
            setSongVolume((Integer.parseInt(configChanged.getNewValue())));
        }
        if (configChanged.getKey().equals("setSoundBank")) {
            if (sequencer != null && midiAudioStream != null) {
                sequencer.stop();
                sequencer.close();
                sequencer = null;
                midiAudioStream = null;
                initSoundSynth();
            }
        }
    }


    @Override
    protected void shutDown() {
        if (sequencer != null && midiAudioStream != null) {
            sequencer.stop();
            sequencer.close();
            sequencer = null;
            midiAudioStream = null;
            client.setMusicVolume(clientVolume);
        }
    }
}
