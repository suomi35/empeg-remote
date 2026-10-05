package com.chasinglemons.empeg.util

object Constants {

    const val LENS_BLUE = 0xFF00BFFF
    const val LENS_RED = 0xFFE52900
    const val LENS_YELLOW = 0xFFD0C700
    const val LENS_GREEN = 0xFF00DA37
    const val LENS_WHITE = 0xFFFFFFFF

    const val EXTENSION_MP3 = ".mp3"

    const val HTML_A = "a"
    const val HTML_HREF = "href"
    const val HTML_TD = "td"
    const val HTML_TR = "tr"

    const val ALL_MUSIC = "All Music"
    const val PLAYLIST_HEAD = "head"
    const val PLAYLIST_NONE = "none"
    const val PLAYLIST_ADD = "add"

    // Root of the browse tree on the empeg web lite server (FID 101 = "All
    // Music"). Current firmware ignores EXT=.htm and returns playlist XML.
    const val PLAYLIST_ROOT_PATH = "/?FID=101&EXT=.xml"

    const val SECONDS = "seconds"
    const val MINUTES = "minutes"

    // https://riocar.org/FAQ/11.html
    const val HIJACK_MENU = "HijackMenu"
    const val ONE = "One"
    const val ONE_LONG = "One.L"
    const val TWO = "Two"
    const val TWO_LONG = "Two.L"
    const val THREE = "Three"
    const val THREE_LONG = "Three.L"
    const val FOUR = "Four"
    const val FOUR_LONG = "Four.L"
    const val FIVE = "Five"
    const val FIVE_LONG = "Five.L"
    const val SIX = "Six"
    const val SIX_LONG = "Six.L"
    const val SEVEN = "Seven"
    const val SEVEN_LONG = "Seven.L"
    const val EIGHT = "Eight"
    const val EIGHT_LONG = "Eight.L"
    const val NINE = "Nine"
    const val NINE_LONG = "Nine.L"
    const val ZERO = "Zero"
    const val ZERO_LONG = "Zero.L"
    const val CANCEL = "Cancel"
    const val CANCEL_LONG = "Cancel.L"
    const val SOURCE = "Source"
    const val SOURCE_LONG = "Source.L"
    const val TUNER = "Tuner"
    const val TUNER_LONG = "Tuner.L"
    const val SELECT_MODE = "SelectMode"
    const val SELECT_MODE_LONG = "SelectMode.L"
    const val SOUND = "Sound"
    const val SOUND_LONG = "Sound.L"
    const val PREV_TRACK = "PrevTrack"
    const val PREV_TRACK_LONG = "PrevTrack.L"
    const val NEXT_TRACK = "NextTrack"
    const val NEXT_TRACK_LONG = "NextTrack.L"
    const val MENU = "Menu"
    const val MENU_LONG = "Menu.L"
    const val INFO = "Info"
    const val INFO_LONG = "Info.L"
    const val VOL_UP = "VolUp"
    const val VOL_UP_LONG = "VolUp.L"
    const val VOL_DOWN = "VolDown"
    const val VOL_DOWN_LONG = "VolDown.L"
    const val VISUAL = "Visual"
    const val VISUAL_LONG = "Visual.L"
    const val PLAY = "Play"
    const val PLAY_LONG = "Play.L"
    const val SEARCH = "Search"
    const val SEARCH_LONG = "Search.L"
    const val TOP = "Top"
    const val TOP_LONG = "Top.L"
    const val BOTTOM = "Bottom"
    const val BOTTOM_LONG = "Bottom.L"
    const val LEFT = "Left"
    const val LEFT_LONG = "Left.L"
    const val RIGHT = "Right"
    const val RIGHT_LONG = "Right.L"
    const val KNOB_LEFT = "KnobLeft"
    const val KNOB_RIGHT = "KnobRight"
    const val KNOB = "Knob"
    const val KNOB_LONG = "Knob.L"

}