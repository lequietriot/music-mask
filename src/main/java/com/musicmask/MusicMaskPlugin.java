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

import com.google.inject.Provides;
import com.musicmask.soundengine.MidiPcmStream;
import com.musicmask.soundengine.MidiTrack;
import com.musicmask.soundengine.PcmPlayer;
import com.musicmask.soundengine.SoundPlayer;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import javax.sound.midi.InvalidMidiDataException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@PluginDescriptor(
        name = "Music Mask",
        description = "Plays music over the game",
        tags = {"sound", "music"}
)
public class MusicMaskPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private MusicMaskConfig musicMaskConfig;

    private String currentSong;

    private String maskPath;

    private Thread songThread;

    private Thread fadeThread;

    private SoundPlayer soundPlayer;

    @Override
    protected void startUp() throws Exception
    {
        currentSong = "Scape Main";
        initSongThread();
        songThread.start();
    }

    private void initSongThread()
    {
        songThread = new Thread(() ->
        {
            try
            {
                if (!musicMaskConfig.getShuffleMode())
                {
                    File resource = new File("resources/MusicMask/RLMusicMask/");
                    maskPath = resource.getAbsolutePath();
                    File[] midiFiles = new File(maskPath + "/MIDI/").listFiles();
                    if (midiFiles != null)
                    {
                        for (File midi : midiFiles)
                        {
                            if (midi.getName().contains(" - "))
                            {
                                int index = midi.getName().lastIndexOf(" - ");
                                String name = midi.getName().substring(index).replace(".mid", "").replace(" - ", "").trim();

                                if (name.equalsIgnoreCase(currentSong))
                                {
                                    initMidiStream(midi, musicMaskConfig.getShuffleMode(), musicMaskConfig.getLoopingMode());
                                    System.out.println(name + " is playing!");
                                }
                            }

                            if (midi.getName().replace(".mid", "").trim().equalsIgnoreCase(currentSong))
                            {
                                initMidiStream(midi, musicMaskConfig.getShuffleMode(), musicMaskConfig.getLoopingMode());
                                System.out.println(midi.getName() + " is playing!");
                            }
                        }
                    }
                }
                else
                {
                    File resource = new File("resources/MusicMask/RLMusicMask/");
                    maskPath = resource.getAbsolutePath();
                    File[] midiFiles = new File(maskPath + "/MIDI/").listFiles();
                    if (midiFiles != null)
                    {
                        for (File midi : midiFiles)
                        {
                            if (midi.getName().contains(" - "))
                            {
                                int index = midi.getName().lastIndexOf(" - ");
                                String name = midi.getName().substring(index).replace(".mid", "").replace(" - ", "").trim();

                                if (name.equalsIgnoreCase(currentSong))
                                {
                                    initMidiStream(midi, musicMaskConfig.getShuffleMode(), false);
                                    System.out.println(name + " is playing!");
                                }
                            }

                            if (midi.getName().replace(".mid", "").trim().equalsIgnoreCase(currentSong))
                            {
                                initMidiStream(midi, musicMaskConfig.getShuffleMode(), false);
                                System.out.println(midi.getName() + " is playing!");
                            }
                        }
                    }
                }
            } catch (IOException | InvalidMidiDataException exception)
            {
                exception.printStackTrace();
            }
        });
    }

    private void playSound(SoundPlayer soundPlayer)
    {
        soundPlayer.fill(soundPlayer.samples, 256);
        soundPlayer.write();
    }

    private void initFadeThread()
    {
        fadeThread = new Thread(() ->
        {
            if (soundPlayer != null)
            {
                for (int volume = ((MidiPcmStream) soundPlayer.stream).getPcmStreamVolume(); volume > 0; volume--)
                {
                    ((MidiPcmStream) soundPlayer.stream).setPcmStreamVolume(volume);
                    try
                    {
                        Thread.sleep(20);
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
                soundPlayer.close();
            }
            initSongThread();
            songThread.start();
        });
    }

    private void initMidiStream(File midi, boolean shuffle, boolean looping) throws IOException, InvalidMidiDataException
    {
        MidiPcmStream midiPcmStream = new MidiPcmStream();
        Path path = Paths.get(midi.toURI());
        PcmPlayer.pcmPlayer_sampleRate = 44100;
        PcmPlayer.pcmPlayer_stereo = true;

        MidiTrack midiTrack = new MidiTrack();
        midiTrack.midi = Files.readAllBytes(path);
        midiTrack.loadMidiTrackInfo();

        midiPcmStream.init(9, 128);
        midiPcmStream.setMusicTrack(midiTrack, looping);
        midiPcmStream.setPcmStreamVolume(musicMaskConfig.getMusicVolume());
        midiPcmStream.loadTestSoundBankCompletely(midiTrack, maskPath, musicMaskConfig.getMusicVersion().version);

        soundPlayer = new SoundPlayer();
        soundPlayer.setStream(midiPcmStream);
        soundPlayer.samples = new int[8196];
        soundPlayer.capacity = 16384;
        soundPlayer.init();
        soundPlayer.open(soundPlayer.capacity);

        while (midiPcmStream.active)
        {
            if (!shuffle)
            {
                playSound(soundPlayer);
            }

            else
            {
                playSound(soundPlayer);

                if (midiPcmStream.midiFile.isDone())
                {
                    soundPlayer.close();
                    if (maskPath != null)
                    {
                        File[] midiFiles = new File(maskPath + "/MIDI/").listFiles();

                        if (midiFiles != null)
                        {
                            File midiFile = midiFiles[(int) (midiFiles.length * Math.random())];
                            if (midiFile.getName().contains(".mid"))
                            {
                                if (midiFile.getName().contains(" - "))
                                {
                                    int index = midiFile.getName().lastIndexOf(" - ");
                                    String name = midiFile.getName().substring(index).replace(".mid", "").replace(" - ", "").trim();

                                    if (name.equalsIgnoreCase(currentSong))
                                    {
                                        initMidiStream(midiFile, musicMaskConfig.getShuffleMode(), false);
                                        System.out.println(name + " is playing!");
                                    }
                                }

                                if (midiFile.getName().replace(".mid", "").trim().equalsIgnoreCase(currentSong))
                                {
                                    initMidiStream(midiFile, musicMaskConfig.getShuffleMode(), false);
                                    System.out.println(midiFile.getName() + " is playing!");
                                }
                            }

                            else
                            {
                                midiFile = midiFiles[(int) (midiFiles.length * Math.random())];
                                if (midiFile.getName().contains(".mid"))
                                {
                                    if (midiFile.getName().contains(" - "))
                                    {
                                        int index = midiFile.getName().lastIndexOf(" - ");
                                        String name = midiFile.getName().substring(index).replace(".mid", "").replace(" - ", "").trim();

                                        if (name.equalsIgnoreCase(currentSong))
                                        {
                                            initMidiStream(midiFile, musicMaskConfig.getShuffleMode(), false);
                                            System.out.println(name + " is playing!");
                                        }
                                    }

                                    if (midiFile.getName().replace(".mid", "").trim().equalsIgnoreCase(currentSong))
                                    {
                                        initMidiStream(midiFile, musicMaskConfig.getShuffleMode(), false);
                                        System.out.println(midiFile.getName() + " is playing!");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void shutDown()
    {
        if (soundPlayer != null && songThread != null)
        {
            soundPlayer.close();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) throws IOException, InvalidMidiDataException
    {
        if (configChanged.getKey().equals("musicVolume"))
        {
            if (soundPlayer != null)
            {
                ((MidiPcmStream) soundPlayer.stream).setPcmStreamVolume(musicMaskConfig.getMusicVolume());
            }
        }

        if (configChanged.getKey().equals("musicVersion"))
        {
            if (songThread != null)
            {
                initFadeThread();
                fadeThread.start();
            }
        }

        if (configChanged.getKey().equals("musicOverride"))
        {
            currentSong = musicMaskConfig.getOverridingMusic();
            if (songThread != null)
            {
                initFadeThread();
                fadeThread.start();
            }
        }

        if (configChanged.getKey().equals("musicShuffle"))
        {
            if (musicMaskConfig.getShuffleMode())
            {
                if (maskPath != null)
                {
                    File[] midiFiles = new File(maskPath + "/MIDI/").listFiles();

                    if (midiFiles != null)
                    {
                        File midiFile = midiFiles[(int) (midiFiles.length * Math.random())];
                        if (midiFile.getName().contains(" - "))
                        {
                            int index = midiFile.getName().lastIndexOf(" - ");
                            currentSong = midiFile.getName().substring(index).replace(".mid", "").replace(" - ", "").trim();

                            if (songThread != null)
                            {
                                initFadeThread();
                                fadeThread.start();
                            }
                        }

                        if (!midiFile.getName().contains(" - "))
                        {
                            currentSong = midiFile.getName().replace(".mid", "").trim();
                            if (songThread != null)
                            {
                                initFadeThread();
                                fadeThread.start();
                            }
                        }
                    }
                }
            }
        }
    }

    @Provides
    MusicMaskConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MusicMaskConfig.class);
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) throws IOException, InvalidMidiDataException
    {
        if (!musicMaskConfig.getShuffleMode())
        {
            if (!musicMaskConfig.getOverridingMusic().equals(""))
            {
                if (gameTick != null)
                {
                    Widget musicPlayingWidget = client.getWidget(WidgetID.MUSIC_GROUP_ID, 6);
                    if (musicPlayingWidget != null)
                    {
                        if (!musicPlayingWidget.getText().equals(currentSong))
                        {
                            currentSong = musicPlayingWidget.getText();
                            if (songThread != null)
                            {
                                initFadeThread();
                                fadeThread.start();
                            }
                        }
                    }
                }
            } else
            {
                Widget musicPlayingWidget = client.getWidget(WidgetID.MUSIC_GROUP_ID, 6);
                if (musicPlayingWidget != null)
                {
                    if (!musicPlayingWidget.getText().equals(currentSong))
                    {
                        currentSong = musicPlayingWidget.getText();
                        if (songThread != null)
                        {
                            initFadeThread();
                            fadeThread.start();
                        }
                    }
                }
            }
        }
        else
        {
            if (maskPath != null)
            {
                File[] midiFiles = new File(maskPath + "/MIDI/").listFiles();

                if (midiFiles != null)
                {
                    File midiFile = midiFiles[(int) (midiFiles.length * Math.random())];
                    if (midiFile.getName().contains(".mid"))
                    {
                        if (midiFile.getName().contains(" - "))
                        {
                            int index = midiFile.getName().lastIndexOf(" - ");
                            String name = midiFile.getName().substring(index).replace(".mid", "").replace(" - ", "").trim();

                            if (name.equalsIgnoreCase(currentSong))
                            {
                                initMidiStream(midiFile, musicMaskConfig.getShuffleMode(), false);
                                System.out.println(name + " is playing!");
                            }
                        }

                        if (midiFile.getName().replace(".mid", "").trim().equalsIgnoreCase(currentSong))
                        {
                            initMidiStream(midiFile, musicMaskConfig.getShuffleMode(), false);
                            System.out.println(midiFile.getName() + " is playing!");
                        }
                    }

                    else
                    {
                        midiFile = midiFiles[(int) (midiFiles.length * Math.random())];
                        if (midiFile.getName().contains(".mid"))
                        {
                            if (midiFile.getName().contains(" - "))
                            {
                                int index = midiFile.getName().lastIndexOf(" - ");
                                String name = midiFile.getName().substring(index).replace(".mid", "").replace(" - ", "").trim();

                                if (name.equalsIgnoreCase(currentSong))
                                {
                                    initMidiStream(midiFile, musicMaskConfig.getShuffleMode(), false);
                                    System.out.println(name + " is playing!");
                                }
                            }

                            if (midiFile.getName().replace(".mid", "").trim().equalsIgnoreCase(currentSong))
                            {
                                initMidiStream(midiFile, musicMaskConfig.getShuffleMode(), false);
                                System.out.println(midiFile.getName() + " is playing!");
                            }
                        }
                    }
                }
            }
        }
    }
}
