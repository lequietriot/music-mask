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
import java.util.List;

public class CustomSynthesizer implements Synthesizer
{
    Synthesizer customSynthesizer;

    CustomSynthesizer(Synthesizer synthesizer) {
        customSynthesizer = synthesizer;
    }

    @Override
    public int getMaxPolyphony() {
        return 256;
    }

    @Override
    public long getLatency() {
        return 0;
    }

    @Override
    public MidiChannel[] getChannels() {
        return customSynthesizer.getChannels();
    }

    @Override
    public VoiceStatus[] getVoiceStatus() {
        return customSynthesizer.getVoiceStatus();
    }

    @Override
    public boolean isSoundbankSupported(Soundbank soundbank) {
        return customSynthesizer.isSoundbankSupported(soundbank);
    }

    @Override
    public boolean loadInstrument(Instrument instrument) {
        return customSynthesizer.loadInstrument(instrument);
    }

    @Override
    public void unloadInstrument(Instrument instrument) {
        customSynthesizer.unloadInstrument(instrument);
    }

    @Override
    public boolean remapInstrument(Instrument from, Instrument to) {
        return customSynthesizer.remapInstrument(from, to);
    }

    @Override
    public Soundbank getDefaultSoundbank() {
        return customSynthesizer.getDefaultSoundbank();
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        return customSynthesizer.getAvailableInstruments();
    }

    @Override
    public Instrument[] getLoadedInstruments() {
        return customSynthesizer.getLoadedInstruments();
    }

    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
        return customSynthesizer.loadAllInstruments(soundbank);
    }

    @Override
    public void unloadAllInstruments(Soundbank soundbank) {
        customSynthesizer.unloadAllInstruments(soundbank);
    }

    @Override
    public boolean loadInstruments(Soundbank soundbank, Patch[] patchList) {
        return customSynthesizer.loadInstruments(soundbank, patchList);
    }

    @Override
    public void unloadInstruments(Soundbank soundbank, Patch[] patchList) {
        customSynthesizer.unloadInstruments(soundbank, patchList);
    }

    @Override
    public Info getDeviceInfo() {
        return customSynthesizer.getDeviceInfo();
    }

    @Override
    public void open() throws MidiUnavailableException {
        customSynthesizer.open();
    }

    @Override
    public void close() {
        customSynthesizer.close();
    }

    @Override
    public boolean isOpen() {
        return customSynthesizer.isOpen();
    }

    @Override
    public long getMicrosecondPosition() {
        return customSynthesizer.getMicrosecondPosition();
    }

    @Override
    public int getMaxReceivers() {
        return customSynthesizer.getMaxReceivers();
    }

    @Override
    public int getMaxTransmitters() {
        return customSynthesizer.getMaxTransmitters();
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return customSynthesizer.getReceiver();
    }

    @Override
    public List<Receiver> getReceivers() {
        return customSynthesizer.getReceivers();
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        return customSynthesizer.getTransmitter();
    }

    @Override
    public List<Transmitter> getTransmitters() {
        return customSynthesizer.getTransmitters();
    }
}
