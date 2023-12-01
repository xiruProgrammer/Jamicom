
/**
 * Globals.java
 * Singleton contendo variaveis e constantes de escopo global
 * 
 */

package com.jamicom;

import java.awt.event.KeyEvent;

public class Globals {
	
	public static final int BANK_1K = 1024;
	public static final int BANK_2K = 2048;
	public static final int BANK_4K = 4096;
	public static final int BANK_8K = 8192;
	public static final int BANK_16K = 16384;
	public static final int BANK_32K = 32768;

	public static final int INTERRUPT_NMI = 8;
	public static final int INTERRUPT_FRAME_IRQ = 1;
	public static final int INTERRUPT_DMC_IRQ = 2;
	public static final int INTERRUPT_MAPPER_IRQ = 4;
    public static final int BASE_NAMETABLE_ADDR   = 0x2000;

	public static int SCR_X = 256;
	public static int SCR_Y = 224;

	public static int SCR_SCALE = 1;
	public static boolean INTERPOLATE_GFX = true;
    public static final int PALETTE_SIZE = 64;

	public static int P1_UP = KeyEvent.VK_UP;
	public static int P1_DOWN = KeyEvent.VK_DOWN;
	public static int P1_LEFT = KeyEvent.VK_LEFT;
	public static int P1_RIGHT = KeyEvent.VK_RIGHT;
	public static int P1_A = KeyEvent.VK_Z;
	public static int P1_B = KeyEvent.VK_X;
	public static int P1_SEL = KeyEvent.VK_A;
	public static int P1_START = KeyEvent.VK_S;

	public static boolean IS_HALT = false;
	public static boolean EMULA = false;

    public static final int NMI_FLAG              = 0x80;
    public static final int SPR0HIT_FLAG 	   	  = 0x40;
    public static final int OVERFLOW_FLAG 		  = 0x20;
	public static final int BASE_SPR_TABLE		  = 256;
    public static final int OAM_SIZE              = 256;
    public static final int SMALL_SPRITE          = 8;
    public static final int BIG_SPRITE            = 16;
    public static final int DRAWBUFFER_WIDTH 	  = 256;
    public static final int DRAWBUFFER_HEIGHT 	  = 240;
    public static final int SPR0_HIT_BIT_ON		  = 0x20;
    public static final int SPR0_HIT_BIT_OFF      = 0;
    public static final int SPR_PRIORITY_BIT_MASK = 0x20;
    public static final int SPR_PRIORITY_BIT_ON   = 0x40;
    public static final int SPR_PRIORITY_BIT_OFF  = 0;
    public static final int SPR_FLIPY_BIT_MASK	  = 0x80;
    public static final int SPR_FLIPX_BIT_MASK	  = 0x40;
    public static final int SPR_WIDTH             = 8;
    public static final int SPR_MAX_COORDX        = 248;
    public static final int LAST_SPRITE_Y	      = 252;
    public static final int SPRITE_ENTRY_SIZE	  = 4;
    public static final int MAX_SPRITE_COUNT	  = 8;
    public static final int PIXEL_OPAQUE_MASK     = 3;
    public static final int SPR_COLLISION_BIT     = 0x10;

    public static final int VRAM_READ_CALLER_SPR  = 1;
    public static final int VRAM_READ_CALLER_BG  = 0;

	public static final int SAMPLE_RATE = 48000;
	public static final int FRAMEBUFFER_SIZE =  800;
	public static final int BITS_PER_SAMPLE = 16;
	public static final int AUDIO_CHANNELS = 1;
	public static final int CYCLES_PER_LINE = 114;
	public static final int CYCLES_PER_SAMPLE = 37;
	public static final int APU_FRAME_CYCLES = 7467;

	public static final int SQUARE1 = 0;
	public static final int SQUARE2 = 1;
	public static final int TRIANGLE = 2;
	public static final int NOISE = 3;
	public static final int DMC = 4;

	public static final int DMC_REGISTER_0 = 0;
	public static final int DMC_REGISTER_2 = 1;
	public static final int DMC_REGISTER_3 = 2;

	public static final int TRIANGLE_REGISTER_0 = 0;
	public static final int TRIANGLE_REGISTER_1 = 1;
	public static final int TRIANGLE_REGISTER_2 = 2;
	public static final int NOISE_REGISTER_0 = 0;
	public static final int NOISE_REGISTER_1 = 1;
	
}
