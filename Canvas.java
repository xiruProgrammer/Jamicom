
/**
 * Canvas.java
 * Define objeto do tipo Canvas (simula a tela do jogo)
 * cria imagem buferizada, converte valores da paleta de cores do videogame para valores RGB
 * o buffer é atualizado ANTES da imagem ser exibida, que permite animação suave
 */

package com.jamicom;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

@SuppressWarnings("serial")
class Canvas extends JComponent {
	
	// paleta de cores original do sistema convertida em c�digos RGB equivalentes
	private static final int normalPalette[] = { 
			0x757575, 0x271b8f, 0x0000ab, 0x47009f, 0x8f0077, 0xab0013, 0xa70000, 0x7f0b00,
    		0x432f00, 0x004700, 0x005100, 0x003f17, 0x1b3f5f, 0x000000, 0x000000, 0x000000,
			0xbcbcbc, 0x0073ef, 0x233bef, 0x8300f3, 0xbf00bf, 0xe7005b, 0xdb2b00, 0xcb4f0f,
			0x8b7300, 0x009700, 0x00ab00, 0x00933b, 0x00838b, 0x000000, 0x000000, 0x000000,
			0xffffff, 0x3fbfff, 0x5f97ff, 0xa78bfd, 0xf77bff, 0xff77b7, 0xff7763, 0xff9b3b,
			0xf3bf3f, 0x83d313, 0x4fdf4b, 0x58f898, 0x00ebdb, 0x787878, 0x000000, 0x000000,
			0xffffff, 0xabe7ff, 0xc7d7ff, 0xd7cbff, 0xffc7ff, 0xffc7db, 0xffbfb3, 0xffdbab,
			0xffe7a3, 0xe3ffa3, 0xabf3bf, 0xb3ffcf, 0x9ffff3, 0xc4c4c4, 0x000000, 0x000000 
	};

	// alguns jogos utilizam uma paleta especial com tons de cinza para exibir cenas em preto e branco
	private static final int grayPalette[] = {
			0x747474, 0x747474, 0x747474, 0x747474, 0x747474, 0x747474, 0x747474, 0x747474,
			0x747474, 0x747474, 0x747474, 0x747474, 0x747474, 0x747474, 0x747474, 0x747474,
			0xBCBCBC, 0xBCBCBC, 0xBCBCBC, 0xBCBCBC,	0xBCBCBC, 0xBCBCBC, 0xBCBCBC, 0xBCBCBC,
			0xBCBCBC, 0xBCBCBC, 0xBCBCBC, 0xBCBCBC, 0xBCBCBC, 0xBCBCBC, 0xBCBCBC, 0xBCBCBC,
			0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC,
			0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC,
			0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC,
			0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC, 0xFCFCFC
		};

	// outros jogo enfatizam a paleta original tornando as cores mais brilhantes
	private static final int RGBPalette[] = {
			0x343434, 0x00023C, 0x000051, 0x0d004a, 0x2e0031, 0x410003, 0x3e0000, 0x270000,
			0x0a0600, 0x001100, 0x001900, 0x001200, 0x180e1a, 0x000000, 0x000000, 0x000000,
			0x676767, 0x002e81, 0x0b1386, 0x3a0087, 0x5e0065, 0x770029, 0x700c00, 0x631c0c,
			0x3c3100, 0x004600, 0x005300, 0x004511, 0x003b41, 0x000000, 0x000000, 0x000000,
			0x969696, 0x1d6695, 0x304da9, 0x7145a8, 0x8c3b95, 0x933a65, 0x923b30, 0x905115,
			0x85680e, 0x427504, 0x247d1d, 0x278d4d, 0x00817a, 0x2c2c2c, 0x000000, 0x000000,
			0x969696, 0x5f8496, 0x71799e, 0x7b729d, 0x946f95, 0x946f7d, 0x946b63, 0x937c5c,
			0x928457, 0x809358, 0x5e8c68, 0x629374, 0x57928d, 0x6d6d6d, 0x000000, 0x000000
	};

    private static int paletteMirrors[] = { 
    		0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 
    		16, 17, 18, 4, 19, 20, 21, 8, 22, 23, 24, 12, 25, 26, 27
    };
	
	private final BufferedImage screen;
	private int[] screenBuffer;	
	private int[] actualPalette;
	private int[] ppuPalette;
	private int[] drawBuffer;
	private boolean interpolatePixels;
	private int drawCursor;


	Canvas() {
		screen = new BufferedImage(Globals.SCR_X, Globals.SCR_Y, BufferedImage.TYPE_INT_BGR);
		screenBuffer = new int[Globals.SCR_X * Globals.SCR_Y];
		
		actualPalette = new int[Globals.PALETTE_SIZE];		
		interpolatePixels = Globals.INTERPOLATE_GFX;
		actualPalette = normalPalette;
	}

	public void setup(Ppu ppu) {
		drawBuffer = ppu.getDrawBuffer();
		ppuPalette = ppu.getPalette();		
	}

	public void setPaletteMirror(int index, int elBalor) {
		paletteMirrors[index] = elBalor;
	}

	public int getPaletteMirror(int index) {
		return paletteMirrors[index];
	}
	
	public void setGrayPalette() {
		
		actualPalette = grayPalette;
		
	}

	
	public void setNormalPalette() {
		
		actualPalette = normalPalette;
		
	}

	public void setRGBPalette() {
		
		actualPalette = RGBPalette;
		
	}

	// copia os codigos de cores do sistema ao buffer da tela j� convertidos em equivalentes RGB
	public void drawScanline(int scanlineNum) {
		for (int m = 0; m < 256; m++)
			screenBuffer[drawCursor++] = 
				actualPalette[ppuPalette[paletteMirrors[drawBuffer[(scanlineNum * 256) + m] & 0x1F]]];
	}

	public void resetDrawBuffer() {
		drawCursor = 0;
	}
	
	// desenha o buffer apos conclusao do desenho (60 vezes por segundo)
	public void paint(Graphics g) {
		Graphics2D ig2d = (Graphics2D)g;
        
		if(interpolatePixels) {
			ig2d.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		}
		
        screen.setRGB(0, 0, Globals.SCR_X, Globals.SCR_Y, screenBuffer, 0, Globals.SCR_X);
		g.drawImage(screen, 0, 0, Globals.SCR_X * Globals.SCR_SCALE, Globals.SCR_Y * Globals.SCR_SCALE, this);

	}
	
}
