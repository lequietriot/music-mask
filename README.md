# MusicMask Plugin
A plugin for RuneLite that allows you to play higher quality music over the Old School RuneScape game.

## Downloadable SoundFonts
Some SoundFonts, compatible with the Old School RuneScape SoundBank format, can be downloaded from the RuneScape MIDI Music Archive on Google Drive, found here: https://drive.google.com/drive/folders/1Dl3Q1kNx7ouYcDLJhqAUCZxd4ZliShM2?usp=sharing

## How to use (with the RuneLite Plugin Hub):

- Install the plugin via RuneLite's Plugin Hub.
- Be sure to mute the game's music.
- Download a SoundFont 2 file that is compatible with RuneScape's Sound Bank format (Specification further below).
- In the plugin's configuration settings, set the Default Custom SoundFont Path to the same path where your SoundFont is stored.
- It should be good to go, enjoy your Music Mask!

## How to use (with IntelliJ):
- Make sure you have IntelliJ IDEA, the latest Community Version installed.
- Once you have this, open and create a new project from Version Control.
- Clone the url: https://github.com/lequietriot/music-mask
- In the project, navigate to the src folder, open the test folder, and finally... 
  Run the MusicMaskPluginTest class.
- It should be all good to go, enjoy your Music Mask!

## Old School RuneScape SoundBank Specification
By default, Old School RuneScape uses MIDI CC #32 (Bank Select LSB) to choose different banks, with bank 1 being the 
Drum Kits. Each bank can store up to 128 instruments, but Old School RuneScape does not fill every instrument slot.
Below is a list of every instrument currently in Old School RuneScape as of the Theatre of Blood update:

### Bank 0
- 0 = Acoustic Grand Piano
- 1 = Bright Acoustic Piano
- 2 = Electric Grand Piano
- 3 = Honky-tonk Piano
- 4 = Electric Piano 1
- 5 = Electric Piano 2
- 6 = Harpsichord
- 7 = Clavinet
- 8 = Celesta
- 9 = Glockenspiel
- 10 = Music Box
- 11 = Vibraphone
- 12 = Marimba
- 13 = Xylophone
- 14 = Tubular Bells
- 15 = Dulcimer
- 16 = Drawbar Organ
- 17 = Percussive Organ
- 18 = Rock Organ
- 19 = Church Organ
- 20 = Reed Organ
- 21 = Accordion
- 22 = Harmonica
- 23 = Tango Accordion
- 24 = Acoustic Guitar (Nylon)
- 25 = Acoustic Guitar (Steel)
- 26 = Electric Guitar (Jazz)
- 27 = Electric Guitar (Clean)
- 28 = Electric Guitar (Muted)
- 29 = Overdriven Guitar
- 30 = Distortion Guitar
- 31 = Guitar Harmonics
- 32 = Acoustic Bass
- 33 = Electric Bass (Finger)
- 34 = Electric Bass (Pick)
- 35 = Fretless Bass
- 36 = Slap Bass 1
- 37 = Slap Bass 2
- 38 = Synth Bass 1
- 39 = Synth Bass 2
- 40 = Violin
- 41 = Viola
- 42 = Cello
- 43 = Contrabass
- 44 = Tremolo Strings
- 45 = Pizzicato Strings
- 46 = Orchestral Harp
- 47 = Timpani
- 48 = String Ensemble 1
- 49 = String Ensemble 2 (Slow Strings)
- 50 = Synth Strings 1
- 51 = Synth Strings 2
- 52 = Choir Aahs
- 53 = Voice Oohs
- 54 = Synth Voice
- 55 = Orchestra Hit
- 56 = Trumpet
- 57 = Trombone
- 58 = Tuba
- 59 = Muted Trumpet
- 60 = French Horn
- 61 = Brass Section
- 62 = Synth Brass 1
- 63 = Synth Brass 2
- 64 = Soprano Sax
- 65 = Alto Sax
- 66 = Tenor Sax
- 67 = Baritone Sax
- 68 = Oboe
- 69 = English Horn
- 70 = Bassoon
- 71 = Clarinet
- 72 = Piccolo
- 73 = Flute
- 74 = Recorder
- 75 = Pan Flute
- 76 = Blown Bottle
- 77 = Shakuhachi
- 78 = Whistle
- 79 = Ocarina
- 80 = Lead 1 (square)
- 81 = Lead 2 (sawtooth)
- 82 = Lead 3 (calliope)
- 83 = Lead 4 (chiff)
- 84 = Lead 5 (charang)
- 85 = Lead 6 (voice)
- 86 = Lead 7 (fifths)
- 87 = Lead 8 (bass + lead)
- 88 = Pad 1 (new age)
- 89 = Pad 2 (warm)
- 90 = Pad 3 (polysynth)
- 91 = Pad 4 (choir)
- 92 = Pad 5 (bowed)
- 93 = Pad 6 (metallic)
- 94 = Pad 7 (halo)
- 95 = Pad 8 (sweep)
- 96 = FX 1 (rain)
- 97 = FX 2 (soundtrack)
- 98 = FX 3 (crystal)
- 99 = FX 4 (atmosphere)
- 100 = FX 5 (brightness)
- 101 = FX 6 (goblins)
- 102 = FX 7 (echoes)
- 103 = FX 8 (sci-fi)
- 104 = Sitar
- 105 = Banjo
- 106 = Shamisen
- 107 = Koto
- 108 = Kalimba
- 109 = Bag pipe
- 110 = Fiddle
- 111 = Shanai
- 112 = Tinkle Bell
- 113 = Agogo
- 114 = Steel Drums
- 115 = Woodblock
- 116 = Taiko Drum
- 117 = Melodic Tom
- 118 = Synth Drum
- 119 = Reverse Cymbal
- 120 = Guitar Fret Noise
- 121 = Breath Noise
- 122 = Seashore
- 123 = Bird Tweet
- 124 = Telephone Ring
- 125 = Helicopter
- 126 = Applause
- 127 = Gunshot

### Bank 1
- 0 = Standard Drum Kit
- 8 = Room Drum Kit
- 16 = Power Drum Kit
- 24 = Electronic Drum Kit
- 25 = Analog Drum Kit
- 40 = Brush Drum Kit
- 48 = Orchestral Drum Kit
- 56 = Special FX Drum Kit
- 127 = Standard Drum Kit

### Bank 2
- 0 = Choir Ohhs (Transposed down 1 octave, or 12 notes)
- 1 = Opera Voice Oh, Staccato (Transposed down 2 octaves, or 24 notes)
- 2 = Opera Voice Oh, Sustained (Transposed down 3 octaves, or 36 notes)