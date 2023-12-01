/**
 * Mapper.java
 * Define objeto do tipo Mapper (nome dado pela funcao de coordenar o mapeamento de memoria do jogo)
 * Gerencia leitura, escrita da memoria de video e memoria de programa bem como os registradores do sistema
 * diferentes subtipos de mapper foram implementados usando polimorfismo e herança
 */

package com.jamicom;

class Mapper {
	private Ppu ppu;
	private Apu apu;
	private  Console console;
	protected  Cartridge cartridge;
	private  GamePad gamepad;
	private Canvas canvas;
	private Cpu cpu;
	private int interruptSignal;
	private boolean nmiDelayFlag;
	private boolean irqDelayFlag;	
	private boolean nmiEnable;	        	
	private int reg2000;
	private int reg2003;                       
	private int reg4014;                       
    private int bufferReg2007;                  
    protected int[] nameTableData;          
    protected int nt0Offset;
    protected int nt1Offset;
    protected int nt2Offset;
    protected int nt3Offset;
    protected int prgBank0Ptr;           
    protected int prgBank1Ptr;           
    protected int rstAddr;
    private boolean isSecondWrite;  
    private int reg2006;               
    private int addrIncrementer;     
	int patternTable0Offset;   
    int patternTable1Offset;
    private boolean updateDmaCycles;
    private boolean updateDmcCycles;
    
	Mapper() {
        nt0Offset = 0;
        patternTable0Offset = 0;
		patternTable1Offset = 4096;
	    prgBank0Ptr = 0;
	}
	
	public void setup(Jamicom joro) {
    	this.ppu = joro.getPpu();
    	this.cpu = joro.getCpu();
    	this.canvas = joro.getCanvas();
    	this.console = joro.getConsole();
    	this.apu = joro.getApu();
    	this.gamepad = joro.getGamePad();
    	this.cartridge = joro.getCartridge();
    	setCartridge(this.cartridge);
	}

	// configura espelhamento da memoria do programa conforme definicoes do mapper
    public void setCartridge(Cartridge cartridge) {
    	this.cartridge = cartridge;
        if(cartridge.is4ScreenNt()) {
            nameTableData = new int[4096];
        	nt1Offset = Globals.BANK_1K;
        	nt2Offset = nt1Offset + Globals.BANK_1K;
        	nt3Offset = nt2Offset + Globals.BANK_1K;
        }
        else {
        	if(cartridge.getMapperNum() == 5)
        		nameTableData = new int[4096];
        	else
                nameTableData = new int[2048];
        	if (cartridge.getMirror() == 0) {
                nt1Offset = 0;
                nt2Offset = Globals.BANK_1K;
            }
            else {
                nt1Offset = Globals.BANK_1K;
                nt2Offset = 0;
            }
            nt3Offset = Globals.BANK_1K;        	
        }
	    if(cartridge.getNumPrg() == 1)
	    	prgBank1Ptr = 0;
	    else
	    	prgBank1Ptr = Globals.BANK_16K;
    }    
    
    // retornam endereços dos vetores de interrupcao da CPU
	int getRstAddr() {
		return ioRead(0xFFFC) | (ioRead(0xFFFD) << 8);
	}

	int getIrqAddr() {
		return ioRead(0xFFFE) | (ioRead(0xFFFF) << 8); 
	}

	int getNmiAddr() {
		return ioRead(0xFFFA) | (ioRead(0xFFFB) << 8);
	}

	int getInterruptSignal() {
		return interruptSignal;
	}

	// ajuste dos ciclos do sistema de acordo com interrupcoes
	void setUpdateDmcCycles(boolean balor) {
		updateDmcCycles = balor;
	}

	boolean getUpdateDmcCycles() {
		return updateDmcCycles;
	}

	void setUpdateDmaCycles(boolean balor) {
		updateDmaCycles = balor;
	}

	boolean getUpdateDmaCycles() {
		return updateDmaCycles;
	}

	// gerencia manejo de interrupcoes
    void setInterruptSignal(int losflagsRegister) {	
    	interruptSignal |= losflagsRegister;
    }

    void clearInterruptSignal(int losflagsRegister) {   	
    	interruptSignal = (interruptSignal & (~(losflagsRegister))) & 0xff;       
    }

	boolean irqIsRequested() {
		if((interruptSignal & 7) !=0)
			return true;
		else
			return false;
	}

    
	void setNmiDelayFlag(boolean value) {
		nmiDelayFlag = value;
	}

	boolean getNmiDelayFlag() {
		return nmiDelayFlag;
	}

	void setIrqDelayFlag(boolean value) {
		irqDelayFlag = value;
	}

	boolean getIrqDelayFlag() {
		return irqDelayFlag;
	}

	void setNmiEnableFlag(boolean value) {
		nmiEnable = value;
	}

	boolean getNmiEnableFlag() {
		return nmiEnable;
	}

	void setReg2003(int balor) {
		reg2003 = balor;
	}
	
	Ppu getPpu() {
		return this.ppu;
	}
	
	/* gerencia leitura de memoria RAM e aos registradores do sistema
	 * a comunicação da CPU com a PPU, APU e portas é feita por registradores mapeados na memoria principal do sistema
	 */
	int ioRead(int addr) {	
		if(addr < 0x2000)
	    	return 	console.systemRamRead(addr & 0x7ff);
		else if ( (addr >= 0x2000) && (addr < 0x4000) ) { // PPU Registers

			switch (addr % 8) {

	            case 0:
	            	return (reg2000 & 0xfc) | ((ppu.getLoopyV() >> 10) & 3);
	            case 1:
	                return 0xff;
	            case 2: {
	                int temp = ppu.getFlagsRegister();
	                ppu.turnOffFlags(Globals.NMI_FLAG);
	                isSecondWrite = false;
	                int laSoma =  ppu.lineDot + (cpu.currentOpCycles*3);
	                if( console.scanline == 240 && (laSoma > 341)) 
	                	cpu.setSuprimeNmi(true);
	                return temp;
	            }
	            case 3:
	                return reg2003;
	            case 4:
	                return ppu.sprOamRead(reg2003);
	            case 5:
	                return 0;
	            case 6:
	            	return reg2006;
	            case 7: {
	            	
	                int tempor;
	                int lv = ppu.getLoopyV();   
	                if (lv >= 0x3F00)
	                	tempor = canvas.getPaletteMirror(lv & 0x1F);              
	                else {
	                    tempor = bufferReg2007;
	                    ppu.vramReadCaller = Globals.VRAM_READ_CALLER_SPR;
	                    bufferReg2007 = vramRead(lv);
	                } 
	                int currScanline = console.getCurrentScanline();  
	                if( ppu.isRenderActive() && ( ( (currScanline < 240)) || (currScanline == 261)) ) {   	
	                	ppu.updateCoarseX();
	                	ppu.updateCoarseY();   	
	                }
	                else
	                	ppu.setLoopyV((lv + addrIncrementer) & 0x7FFF);
	                	
	                return tempor;
	            }
	        	default:
	        		return 0xff;
	        }    
	    }	    
		else if(addr == 0x4015)
			return apu.reg4015Read();    
	    else if (addr == 0x4016)// PORTAS DE LEITURAS DOS CONTROLES
	    	return gamepad.reg4016Read();
	    else if(addr == 0x4017)
	    	return 0x480;
	    else if ((addr >= 0x8000) && (addr < 0xC000))
	        return cartridge.prgRead(addr - 0x8000 + prgBank0Ptr);
	    else if ((addr >= 0xC000) && (addr < 0x10000))
	    	return cartridge.prgRead(addr - 0xC000 + prgBank1Ptr);
	    else
	    	return 0;	    	
	}
	
	// gerencia escrita de memoria RAM e aos registradores do sistema
	 void ioWrite(int addr, int value) {
	    if (addr < 0x2000)
	        console.systemRamWrite(addr & 0x7ff, value);
	    else if ((addr >= 0x2000) && (addr < 0x4000)) {	// PPU Registers
	    	switch (addr % 8) {
	    		case 0: {// REGISTER $2000
	    			if(console.isVBlankPeriod() && value > 0x7f && ppu.flagIsSet(Globals.NMI_FLAG) && nmiEnable == false) {
	    				setNmiDelayFlag(true);
	    				setInterruptSignal(Globals.INTERRUPT_NMI);
	    			}
	    			reg2000 = value;
	            	nmiEnable = (value & 0x80) == 0x80 ? true : false;
	                addrIncrementer = (value & 4) == 4 ? 32 : 1;
	                ppu.reg2000Write(value);
	                break;
	    		}
	            case 1: {// REGISTER 2001

	            	switch((value & 0xe0)>>5) {	// color intensity bits
	            		case 0:
	            			canvas.setNormalPalette();
	            			break;
	            		case 7:
	            			canvas.setRGBPalette();
	            			break;
	            	}
	                if ((value & 1) == 1)
	                	canvas.setGrayPalette();
	                ppu.reg2001Write(value);
	                break;
	            }
	            case 3: {// REGISTER 2003
	                reg2003 = value;

	                break;
	            }
	            case 4: {// REGISTER 2004
	                ppu.sprOamWrite(reg2003, value);
	                reg2003 = (reg2003 + 1) & 0xff;
	                break;
	            }
	            case 5: {// REGISTER 2005
	                int lt = ppu.getLoopyT();
	            	if (isSecondWrite == false) {    
	                    ppu.setLoopyT((lt & 0x7fe0) | ((value & 0xf8) >> 3));
	                    ppu.setFineX(value & 7);
	                    isSecondWrite = true;
	                }
	                else {   
	                	ppu.setLoopyT((lt & 0xc1f) | ((value & 7) << 12) | ((value & 0xc0) << 2) | ((value & 0x38) << 2));
	                    isSecondWrite = false;
	                }
	                break;
	            }
	            case 6: {// REGISTER 2006
	            	reg2006 = value;
	                int lt2 = ppu.getLoopyT();
	            	if (isSecondWrite == false) {
	                    ppu.setLoopyT((lt2 & 0xff) | ((value & 0x3f) << 8));
	                    isSecondWrite = true;
	                }
	                else {
	                	if(ppu.lineDot == 255 || ppu.lineDot == 256)
	                		ppu.fue255 = true;
	                   ppu.setLoopyT((lt2 & 0x7f00) | value);
	                    ppu.setLoopyV(ppu.getLoopyT());
	                    isSecondWrite = false;

	                }
	                break;
	            }
	            case 7: {// REGISTER 2007
	            	int lv = ppu.getLoopyV();
	                vramWrite(lv, value);
	                int currScanline = console.getCurrentScanline();
	                if( ppu.isRenderActive() && ( ( (currScanline < 240)) || (currScanline == 261)) ) {   	
	                		ppu.updateCoarseX();
	                		ppu.updateCoarseY();	                	
	                }
	                else
	                	ppu.setLoopyV((lv + addrIncrementer) & 0x7FFF);	
	                break;
	            }
	        }
	    }
	    else if (addr == 0x4000) // REGISTRADORES DA APU
	    	apu.sqrRegister0Write(Globals.SQUARE1, value);
	    else if (addr == 0x4001)
	    	apu.sqrRegister1Write(Globals.SQUARE1, value);	    
	    else if (addr == 0x4002)
	    	apu.sqrRegister2Write(Globals.SQUARE1, value);	    
	    else if (addr == 0x4003)
	    	apu.sqrRegister3Write(Globals.SQUARE1, value);
	    else if (addr == 0x4004)
	    	apu.sqrRegister0Write(Globals.SQUARE2, value);
	    else if (addr == 0x4005)
	    	apu.sqrRegister1Write(Globals.SQUARE2, value);	    
	    else if (addr == 0x4006)
	    	apu.sqrRegister2Write(Globals.SQUARE2, value);	    
	    else if (addr == 0x4007)	    	
	    	apu.sqrRegister3Write(Globals.SQUARE2, value);
	    else if (addr == 0x4008)
	    	apu.trgRegister0Write(value);
	    else if (addr == 0x400A) 
	    	apu.trgRegister1Write(value);	    
	    else if (addr == 0x400B)
	    	apu.trgRegister2Write(value);	    
	    else if (addr == 0x400C)
	    	apu.noiseRegister0Write(value);	    
	    else if (addr == 0x400E)
	    	apu.noiseRegister1Write(value);	    
	    else if (addr == 0x400F)
	    	apu.noiseRegister2Write(value);	    
	    else if (addr == 0x4010) {
	    	apu.dmcRegister0Write(value);	 
	    }	    
	    else if (addr == 0x4011)
	    	apu.dmcRegister1Write(value);
	    else if (addr == 0x4012) 
	    	apu.dmcRegister2Write( value);
    	else if (addr == 0x4013)
	    	apu.dmcRegister3Write(value);
	    else if (addr == 0x4014) {    // SPRITE DMA	    
	        reg4014 = value * 0x100;
	        for (int n = 0; n < 0x100; n++) {
	           ppu.sprOam[(reg2003 + n) & 0xFF] = ioRead(reg4014 + n);	
	        }
	        updateDmaCycles = true;	
	    }	    
	    else if (addr == 0x4015) {
	    	apu.toggleChannel(value);
	    	apu.dmcIrqFlag = 0;
	  		clearInterruptSignal(Globals.INTERRUPT_DMC_IRQ);
	    }	    
	    else if (addr == 0x4016)  // LATCH CONTROL 1
	    	gamepad.reg4016Write(value);
	    else if (addr == 0x4017) {   // LATCH CONTROL 2
	    	gamepad.reg4017Write(value);
	    	apu.reg4017Write(value);
	    }
	}

	 // gerencia escrita a memoria de video da PPU
	 void vramWrite(int addr, int value) {
    	if (addr < 0x1000) {
    		if(cartridge.getNumChr() == 0)
                cartridge.chrWrite((addr%4096) + patternTable0Offset, value);
    	}
        else if ((addr >= 0x1000) && (addr < 0x2000)) {
    		if(cartridge.getNumChr() == 0)
                cartridge.chrWrite((addr%4096) + patternTable1Offset, value);
        }
        else if ((addr >= 0x2000) && (addr < 0x2400))
            nameTableData[addr - 0x2000 + nt0Offset] = value;
        else if ((addr >= 0x2400) && (addr < 0x2800))
        	nameTableData[addr - 0x2400 + nt1Offset] = value;
        else if ((addr >= 0x2800) && (addr < 0x2C00))
        	nameTableData[addr - 0x2800 + nt2Offset] = value;
        else if ((addr >= 0x2C00) && (addr < 0x3000))
        	nameTableData[addr - 0x2C00 + nt3Offset] = value;
        else if ((addr >= 0x3000) && (addr < 0x3400))
        	nameTableData[addr - 0x3000 + nt0Offset] = value;
        else if ((addr >= 0x3400) && (addr < 0x3800))
        	nameTableData[addr - 0x3400 + nt1Offset] = value;
        else if ((addr >= 0x3800) && (addr < 0x3C00))
        	nameTableData[addr - 0x3800 + nt2Offset] = value;
        else if ((addr >= 0x3C00) && (addr < 0x3F00))
        	nameTableData[addr - 0x3C00 + nt3Offset] = value;
        else if ((addr >= 0x3F00) && (addr < 0x4000))
        	ppu.palettesWrite(canvas.getPaletteMirror(addr & 0x1F), value & 0x3f);
    }
    
	 // gerencia leitura a memoria de video da PPU
	 int vramRead(int addr) {
		if(addr < 0x1000)
			return cartridge.chrRead((addr%4096) + patternTable0Offset);
		else if ((addr >= 0x1000) && (addr < 0x2000))
			return cartridge.chrRead((addr%4096) + patternTable1Offset);
		else if ((addr >= 0x2000) && (addr < 0x2400))
			return nameTableData[addr - 0x2000 + nt0Offset];
		else if ((addr >= 0x2400) && (addr < 0x2800))
			return nameTableData[addr - 0x2400 + nt1Offset];
		else if ((addr >= 0x2800) && (addr < 0x2C00))
			return nameTableData[addr - 0x2800 + nt2Offset];
		else if ((addr >= 0x2C00) && (addr < 0x3000))
			return nameTableData[addr - 0x2C00 + nt3Offset];
		else if ((addr >= 0x3000) && (addr < 0x3400))
			return nameTableData[addr - 0x3000 + nt0Offset];
		else if ((addr >= 0x3400) && (addr < 0x3800))
			return nameTableData[addr - 0x3400 + nt1Offset];
		else if ((addr >= 0x3800) && (addr < 0x3C00))
			return nameTableData[addr - 0x3800 + nt2Offset];
		else if ((addr >= 0x3C00) && (addr < 0x3F00))
			return nameTableData[addr - 0x3C00 + nt3Offset];
		else if ((addr >= 0x3F00) && (addr < 0x4000))
			return ppu.palettesRead(canvas.getPaletteMirror(addr & 0x1F));
		else
			return 0xff;
	}
	
	void update(int losCiclos, int scanlineNum) {	// para MMC, VRC y octros
	
	}	
}

/* Mapper Numero 2
 * O segundo mapper desenvolvido para o videogame, em ordem cronologica
 * em relação ao mapper original permite expansao da memoria de programa dos jogos até 256 KB
 */
class Mapper002 extends Mapper {
	int prgMask;
	
	@Override
    public void setCartridge(Cartridge cartridge) {
    	super.setCartridge(cartridge);
		prgMask = cartridge.getNumPrg() -1;
	    prgBank1Ptr = (cartridge.getNumPrg() -1) * Globals.BANK_16K;	    	    	
    }
    
	@Override
	 void ioWrite(int addr, int balor) {
	    if ((addr >= 0x8000) && (addr < 0x10000))
	    	prgBank0Ptr = Globals.BANK_16K * (balor & prgMask);
	    else
	    	super.ioWrite(addr, balor);
	}
}

