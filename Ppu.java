
/**
 * Ppu.java
 * Define objeto do tipo PPU (Unidade de Processamento de Imagem);
 * Renderiza os dois componentes graficos, o cen�rio (= background) e os personagens (= sprites);
 * ambos elementos sao compostos de Tiles (blocos) contendo caracteres de 8 x 8 pixels;
 * tanto a memoria de cenarios quanto a de personagens apenas indica o indice do bloco na memoria e seus atributos;
 * a renderizacao inclui calculos de rotacao de tela e interacao dos personagens com o cenario;
 * os personagens sao desenhados primeiro e em seguida o cenario
 * 
 * 
 */

package com.jamicom;

import java.util.Arrays;

public class Ppu {
	

    private int flagsRegister;
    int baseBgPatternIndex;                
    int baseSprPatternIndex;               
    int sprHeight;                    
    public int[] sprOam;                 
    private int loopyT;

    private int fineY;
    private int fineX;
    boolean bgRenderEnabled;               
    boolean sprRenderEnabled;
    private boolean bgLeftColumnEnabled;           
    private boolean sprLeftColumnEnabled;
    private int bgColunaDrawCursor;
    private int renderBufferCursor;           
    private int[] drawBuffer;      
    private int bgPatternByte1;
    private int bgPatternByte2;
    private int attrByte;
    private int attrShift;
    private int loopyV;
    private int bgTile;
    private int bgTileAddr;
    private int bgTileColOffset;
    
    private int sprPatternByte1;
    private int sprPatternByte2;
    private int sprPatternFinal;
    private int sprFirstLine;
    private int sprTileNum;
    private int sprTileAddr;
    private int sprDirection;
    private int sprTileColOffset;
    private int sprTileLinOffset;
    private int spriteCount;
    private int sprPixelsToWrite;
    private int sprZeroBit;                   
    private int sprPriorityBit;             
    private boolean sprHitFrameFlag;             
    private int[] palettes;              
    public int lineDot;
    public int ciclosSobrando;
    int spr0FlagDelay;
    int vramReadCaller;
    boolean fue255;
    boolean samePatternTable;
    private Mapper mapper;
    private Cpu cpu;

    public Ppu() {
    	
        baseSprPatternIndex = Globals.BASE_SPR_TABLE;
        sprHeight = Globals.SMALL_SPRITE;
        sprOam = new int[Globals.OAM_SIZE];
        drawBuffer = new int[Globals.DRAWBUFFER_WIDTH * Globals.DRAWBUFFER_HEIGHT];
		palettes = new int[28];
    }

    public void setup(Jamicom jamicom) {
    	this.mapper = jamicom.getMapper();
    	this.cpu = jamicom.getCpu();
    }

    // a PPU tem um registrador de status proprio
    void turnOnFlags(int flags) {
        flagsRegister |= flags;
    }

    void turnOffFlags(int flags) {
        flagsRegister = (flagsRegister & (~(flags))) & 0xff;   
    }

    void resetFlags() {
        turnOffFlags(Globals.NMI_FLAG | Globals.SPR0HIT_FLAG | Globals.OVERFLOW_FLAG);
    }

    int getFlagsRegister() {
    	return flagsRegister;
    }
    
    // leitura e escrita da memoria contendo personagens
    int sprOamRead(int addr) {	
    	return sprOam[addr];
    }
    
    void sprOamWrite(int addr, int val) {	
    	sprOam[addr] = val;
    }
    
    // paletas do sistema, convertidos em equivalentes RGB antes de exibicao na tela
    int palettesRead(int addr) {	
    	return palettes[addr];
    }
    
    void palettesWrite(int addr, int val) {	
    	palettes[addr] = val;
    }
    
	int[] getPalette() {		
		return palettes;	
	}

	// o sprite na posicao zero de memoria tem prioridade logica sobre os outros
    int getSprZeroBit(int spriteNumber) {	
        if(spriteNumber == 0)
            return Globals.SPR0_HIT_BIT_ON;
        else
            return Globals.SPR0_HIT_BIT_OFF;
    }

    // flag de prioridade do sprite para definir a ordem nas camadas do desenho
    int getSprPriorityBit(int spriteNumber) {    	
        if((sprOam[spriteNumber + 2] & Globals.SPR_PRIORITY_BIT_MASK) !=0)
            return Globals.SPR_PRIORITY_BIT_ON;
        else
            return Globals.SPR_PRIORITY_BIT_OFF; 
    }

    // flags de rotacao vertical e horizontal do sprite
    boolean getSprFlipYBit(int spriteNumber) {	
        if((sprOam[spriteNumber + 2] & Globals.SPR_FLIPY_BIT_MASK) !=0)
            return true;
        else
            return false;   
    }

    boolean getSprFlipXBit(int spriteNumber) {
        if((sprOam[spriteNumber + 2] & Globals.SPR_FLIPX_BIT_MASK) !=0)
            return true;
        else
            return false;  
    }

    // verifica se o sprite esta incluido na linha sendo atualmente desenhada
    int getSprPixelsToWrite(int spriteNumber) {
        if(sprOam[spriteNumber + 3] > Globals.SPR_MAX_COORDX)
            return 0x100 - sprOam[spriteNumber + 3];
        else
            return Globals.SPR_WIDTH;   
    }

    int getSprHeight() {	
        return sprHeight;    
    }
    
    // metodos para calculo da posicao do cenario (scroll)
    void setFineX(int balor) {	
        fineX = balor;
    }

    void setLoopyV(int balor) {
        loopyV = balor;
    }
    
    void setLoopyT(int balor) {
        loopyT = balor;    
    }

    int getFineX() {
        return fineX;
    }

    int getLoopyV() {
        return loopyV;   
    }

    int getLoopyT() {
        return loopyT;  
    }

    int getBgPatternTable() {	
        return baseBgPatternIndex;  
    }
   
    int getSprPatternTable() {	
        return baseSprPatternIndex;
    }
        
    // renderiza uma linha de personagens - sprites (executado 240 vezes por quadro)
    void renderSprites(int currScanline) {
    	
    	// percorre toda memoria de sprites a procura dos que se encontram na linha atual
        for (int spriteIndex = Globals.LAST_SPRITE_Y; spriteIndex > -1; spriteIndex -= Globals.SPRITE_ENTRY_SIZE) {
            sprFirstLine = sprOam[spriteIndex]+1;
            
            // seleciona o sprite caso a linha atual passe dentro de suas coordenadas
            if (currScanline >= sprFirstLine && currScanline < (sprFirstLine + sprHeight)) {
                spriteCount++;
                
                // caso mais de 8 sprites por linha, ativa o flag sinalizador no registrador de status
                if(spriteCount >= Globals.MAX_SPRITE_COUNT)
                	turnOnFlags(Globals.OVERFLOW_FLAG);

                // obtem os atributos do sprite (subpaleta, posicao, prioridade, tamanho, rotacao)
                sprZeroBit = getSprZeroBit(spriteIndex);
                sprPriorityBit = getSprPriorityBit(spriteIndex);

                if (sprHeight == Globals.BIG_SPRITE)
                    sprTileNum = ((sprOam[spriteIndex + 1] & 1) * Globals.BASE_SPR_TABLE) | (sprOam[spriteIndex + 1] & 0xfe);
                else
                    sprTileNum = baseSprPatternIndex + sprOam[spriteIndex + 1];

                if(getSprFlipYBit(spriteIndex) == true)
                    sprTileLinOffset = (sprHeight - 1) - (currScanline - sprFirstLine);
                else
                    sprTileLinOffset = (currScanline - sprFirstLine);
                
                if (sprTileLinOffset > 7)
                    sprTileLinOffset += 8;

                sprTileAddr = (16 * sprTileNum) + sprTileLinOffset;
                
                vramReadCaller = Globals.VRAM_READ_CALLER_SPR;
                
                // obtem o caracter (pattern) do sprite na memoria de caracteres
                sprPatternByte1 = mapper.vramRead(sprTileAddr);
                sprPatternByte2 = mapper.vramRead(sprTileAddr + 8);

                sprPixelsToWrite = getSprPixelsToWrite(spriteIndex);

                // posiciona o cursor na coordenada inicial para desenho
                renderBufferCursor = (currScanline * Globals.DRAWBUFFER_WIDTH) + sprOam[spriteIndex + 3];

                if (getSprFlipXBit(spriteIndex)) {	// Si Flip X
                    sprDirection = 1;
                    sprTileColOffset = 0;
                }
                else {
                    sprDirection = -1;
                    sprTileColOffset = 7;
                }
                
                // finalmente, desenha o sprite selecionado
                for (int pixelIndex = 0; pixelIndex < sprPixelsToWrite; pixelIndex++) {
                	// obtem o caracter (pattern)
                    sprPatternFinal = ((sprPatternByte1 >> sprTileColOffset) & 1) | (((sprPatternByte2 >> sprTileColOffset) & 1) << 1);

                    // caso caracter utilize o indice 0 da paleta � considerado transparente e NAO � desenhado
                    if ((sprPatternFinal & Globals.PIXEL_OPAQUE_MASK) == 0)
                        renderBufferCursor++;
                    
                    // se na margem esquerda da tela e a coluna esquerda est� desativada tambem nao desenha
                    else if ((sprOam[spriteIndex + 3] + pixelIndex) < 8 && sprLeftColumnEnabled == false) 
                        renderBufferCursor++;

                    // de outra forma desenha e insere flags indicando que a posicao ja esta ocupada por um sprite
                    else {
                    	drawBuffer[renderBufferCursor++] = Globals.SPR_COLLISION_BIT | sprPatternFinal |
                            ((sprOam[spriteIndex + 2] & Globals.PIXEL_OPAQUE_MASK) << 2) | sprZeroBit | sprPriorityBit;
                    }
                    sprTileColOffset += sprDirection;
                    
                }
            }
        }
    }

    // renderiza X pixels (definidos por ciclosToRun) no cen�rio na linha atual
    void renderScanline(int ciclosToRun, int scanlineNum) {
        int bgPattern;        
        for (int pixelIndex = 1; pixelIndex <= ciclosToRun; pixelIndex++) {
        	
        	// ativa flag de deteccao de colisao entre um sprite e o cenario
        	if(--spr0FlagDelay == 0) {
        		turnOnFlags(Globals.SPR0HIT_FLAG);
        	}
        	
        	// calcula situacoes especiais de acordo com a posicao horizontal do cursor para calculo de logica
        	if(lineDot == 1 && scanlineNum == 241) {
        		turnOnFlags(Globals.NMI_FLAG);
        		if (mapper.getNmiEnableFlag()) {
        			if(cpu.getSuprimeNmi() == false)
        				mapper.setInterruptSignal(Globals.INTERRUPT_NMI);
        		}
				cpu.setSuprimeNmi(false);
        	}
        	if(lineDot == 1 && scanlineNum == 261) {
            	resetFlags();
    	   		sprHitFrameFlag = false;
         	}
        	if(lineDot == 256) {
        		if(fue255)
        			fue255 = false;
        		else {
                    if (isRenderActive() && (scanlineNum < 240))
                		updateCoarseY();
        		}
        	}        	
        	if(lineDot == 257) {
        		if(fue255)
        			fue255 = false;
        		else {
            		if(isRenderActive() && (scanlineNum < 240)) {
               		 loopyV = (loopyV & 0x7BE0) | (loopyT & 0x41F);
            			mapper.setReg2003(0);
            		}        			
        		}
        	}
        	if(lineDot == 304) {
                if (isRenderActive() && (scanlineNum == 261)) {
                	loopyV = (loopyV & 0x841f) | (loopyT & 0x7BE0);
                }
        	}
        	
        	// se o cursor se encontra dentro das coordenadas visiveis da tela, desenha o cenario
            if (bgColunaDrawCursor < Globals.DRAWBUFFER_WIDTH) {
            	if(bgRenderEnabled) {
            		
            		// obter o caracter respectivo do elemento do cenario da coluna atual
	            	bgPattern = ((bgPatternByte1 >> bgTileColOffset) & 1) | (((bgPatternByte2 >> bgTileColOffset) & 1) << 1);
	            	
	            	// se coluna esquerda desativada nao desenha cenario
	                if (bgPattern == 0 || (bgColunaDrawCursor < 8 && bgLeftColumnEnabled == false))
	                    renderBufferCursor++;
	                else {
	                	
	                	// se a posicao atual do cursor ja contem um sprite
	                    if ((drawBuffer[renderBufferCursor] & Globals.PIXEL_OPAQUE_MASK) != 0) {
	                    	
	                    	// se este sprite for o de numero zero detecta-se colisao com o cenario e um flag sinaliza colisao
	                        if ((drawBuffer[renderBufferCursor] & Globals.SPR_PRIORITY_BIT_MASK) !=0) {
	                            if (sprHitFrameFlag == false) {
	                            	spr0FlagDelay = 3;
	                            	sprHitFrameFlag = true;
	                            } 
	                        }
	                        
	                        // se o sprite tem prioridade menor que o cenario, este � desenhado por cima do sprite                        
	                        if ((drawBuffer[renderBufferCursor] & Globals.SPR_PRIORITY_BIT_ON) !=0)
	                        	drawBuffer[renderBufferCursor] = bgPattern |
	                                (((attrByte >> attrShift) & Globals.PIXEL_OPAQUE_MASK) << 2);
	                    }
	                    
	                    // se nao ha sprite na posicao atual o cenario tambem � desenhado
	                    else
	                    	drawBuffer[renderBufferCursor] = bgPattern |
	                            (((attrByte >> attrShift) & Globals.PIXEL_OPAQUE_MASK) << 2);
	
	                    renderBufferCursor++;
	                }
	                bgTileColOffset--;
	                
	                // se o bloco (tile) do cenario ja foi todo desenhado, obtem na memoria o indice do proximo bloco
	                if (bgTileColOffset < 0) {
	                	
	                    updateCoarseX();
	                    vramReadCaller = Globals.VRAM_READ_CALLER_BG;
	                    bgTile = mapper.vramRead(Globals.BASE_NAMETABLE_ADDR | (loopyV & 0xFFF));
	                    bgTileAddr = baseBgPatternIndex + 16 * bgTile + fineY;
	                    bgPatternByte1 = mapper.vramRead(bgTileAddr);
	                    bgPatternByte2 = mapper.vramRead(bgTileAddr + 8);
	                    bgTileColOffset = 7;
	                    attrByte = mapper.vramRead(0x23C0 | (loopyV & 0x0C00) |
	                        ((loopyV >> 4) & 0x38) | ((loopyV >> 2) & 0x07));
	                    attrShift = (((loopyV & 2) >> 1) | ((loopyV & 0x40) >> 5)) * 2;
	                }
            	}
            }
            bgColunaDrawCursor++;
        	lineDot++;
        	if(lineDot > 340) {
        		ciclosSobrando = ciclosToRun - pixelIndex;
        		lineDot-=341;
        		return;
        	}
            ciclosSobrando = ciclosToRun - pixelIndex;
        }
    }

    // metodo para atualizar o caractere do proximo bloco (tile) a ser desenhado, incluindo calculo da porcao do cenario exibida na tela
    void updateBgLine(int currScanline) {
        renderBufferCursor = (currScanline * Globals.DRAWBUFFER_WIDTH);
        vramReadCaller = Globals.VRAM_READ_CALLER_BG;
        bgTile = mapper.vramRead(Globals.BASE_NAMETABLE_ADDR | (loopyV & 0xFFF));
        fineY = (loopyV & 0x7000) >> 12;
        bgTileAddr = baseBgPatternIndex + 16 * bgTile + fineY;
        bgPatternByte1 = mapper.vramRead(bgTileAddr);
        bgPatternByte2 = mapper.vramRead(bgTileAddr + 8);
        bgTileColOffset = fineX == 0 ? 7 : 7 - fineX;
        attrByte = mapper.vramRead(0x23C0 | (loopyV & 0x0C00) |
            ((loopyV >> 4) & 0x38) | ((loopyV >> 2) & 0x07));
        attrShift = (((loopyV & 2) >> 1) | ((loopyV & 0x40) >> 5)) * 2;
        bgColunaDrawCursor= 0;    
    }

    // metodos para calcular a por��o do cen�rio que � exibida na tela
    void updateCoarseX() {
        if ((loopyV & 0x1F) == 0x1f) {
            loopyV &= ~0x001F;
            loopyV ^= 0x400;
        }
        else
            loopyV++;
    }

    void updateCoarseY() { 
    	if ((loopyV & 0x7000) != 0x7000) { 
    		loopyV += 0x1000;             
    	}
    		else {                                   
    			loopyV &= ~0x7000;             
            int coarseY = (loopyV & 0x3E0) >> 5;
            
            if (coarseY == 29) {                  
                coarseY = 0;
                loopyV ^= 0x800;          
            }
            else if (coarseY == 31)              
                coarseY = 0;
            else                               
                coarseY++;
            loopyV = (loopyV & ~0x03e0) | (coarseY << 5);
       }
    }

    // Escrita ao registrador $2000 (configura variaveis da PPU)
    void reg2000Write(int value) {
        loopyT = (loopyT & 0x73ff) | ((value & 3) << 10);
	    if((value & 0x20) == 0x20)
	        sprHeight = Globals.BIG_SPRITE;
	    else
	    	sprHeight = Globals.SMALL_SPRITE;
		if((value & 0x10) == 0x10)
	        baseBgPatternIndex = 0x1000;
		else
	        baseBgPatternIndex = 0;
	
		if((value & 8) == 8)
	        baseSprPatternIndex =  256;
		else
	        baseSprPatternIndex =  0;
		if(baseBgPatternIndex>> 4 == baseSprPatternIndex)
			samePatternTable = true;
		else
			samePatternTable = false;
    }

    boolean isSamePatternTable() {
    		return samePatternTable;
    }
    
    // Escrita ao registrador $2001 (configura variaveis da PPU complementares ao registrador $2000)
    void reg2001Write(int value) {
    	int laSoma = lineDot + (cpu.getLastOpCycles()*3);
    	if(bgRenderEnabled == false && (value & 8) == 8) {
    		if(lineDot < 257 && laSoma > 250)
    			fue255 = true;
    	}
    	sprRenderEnabled = (value & 0x10) == 0x10;
        bgRenderEnabled =  (value & 8) == 8;
        sprLeftColumnEnabled = (value & 4) == 4;
        bgLeftColumnEnabled = (value & 2) == 2;
    }
    
    void resetRenderBuffer() {
		Arrays.fill(drawBuffer, 0);	// aca color 0 al todos bufa
		renderBufferCursor = 0;
    }
    
    public int[] getDrawBuffer() {
		return drawBuffer;
	}
    
    public boolean isRenderActive() {
        if (sprRenderEnabled || bgRenderEnabled)
        	return true;
        else
        	return false;
    }
    
   boolean flagIsSet(int elFlago) {
    	if((flagsRegister & elFlago) != 0)
    		return true;
    	else
    		return false;	
    }
 
}