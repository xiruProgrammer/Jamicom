/**
 * Console.java
 * Define objeto do tipo Console
 * Integra os subsistemas e executa o loop principal da aplicacao
 * o loop é executado 60 vezes por segundo produzindo os 60 quadros do videogame original
 * cada quadro é composto de 262 linhas, sendo que as linhas de 0 a 239 sao exibidas na tela
 * a cada linha sao atualizados: graficos (tela); sons; mapper e sao executados 114 ciclos da CPU
 * ao final das 262 linhas a tela é renderizada no canvas e as amostras de som enviadas para a saida
 */

package com.jamicom;

import java.io.IOException;

public class Console {
	
	private Ppu ppu;
	private Cpu cpu;
	private Apu apu;
	private Jamicom jamicom;
	private Canvas canvas;

	private GamePad gamepad;
	private Mapper mapper;
	private int[] systemRam;	    	
	public int scanline;
	private static final int[] scanlineCycles = { 114, 114, 113 };
	private int scanlineCycleIndex =0;

	public Console() throws IOException {
		systemRam = new int[0x800];
		
	}
	
	void setup(Jamicom jamicom) {
		this.jamicom = jamicom;
		ppu = jamicom.getPpu();
		cpu = jamicom.getCpu();
		apu = jamicom.getApu();
		canvas = jamicom.getCanvas();
		gamepad = jamicom.getGamePad();
		mapper = jamicom.getMapper();		
	}
	
	 void startEmulation() throws InterruptedException {	 

		 int cyclesToUpdate = 0;
		 while(Globals.EMULA) { 		 
    	    scanline = 0;
    	    
    	    // executa um quadro (60 quadros por segundo)
    	    while (scanline < 262) {
    	    	if ( (scanline >= 0) && (scanline < 240)) { // visible
    	    		
    	    		// calcula uma linha de elementos do jogo (sprites)
    	            if (ppu.sprRenderEnabled)
    	                ppu.renderSprites(scanline);
    	            
    	            // calcula uma linha de cenario (background)
    	            if(ppu.bgRenderEnabled) 
    	            	ppu.updateBgLine(scanline);
    	                  	
    	    	}
    	    	ppu.renderScanline(ppu.ciclosSobrando , scanline);

    	    	while (cpu.getCurrentCycles() < scanlineCycles[scanlineCycleIndex]) {
    	        	cyclesToUpdate = cpu.executeInstruction();
    	        	cpu.totalCiclos+=cyclesToUpdate;
        	        ppu.renderScanline(cyclesToUpdate*3 , scanline);
        	        
        	        // atualiza mapper (quando necessario) checar interrupcoes
    	        	mapper.update(cyclesToUpdate, scanline);          

    	    	}
    	    	
    	    	// produz 3 amostras de audio por linha
    	        apu.audioUpdate(scanlineCycles[scanlineCycleIndex]);
    	        cpu.updateCurrentCycles(-scanlineCycles[scanlineCycleIndex]);
    			if((scanline > 7) && (scanline < 232))
    				canvas.drawScanline(scanline);  	            
    			scanline++;
    	          if(++scanlineCycleIndex > 2)
                  	scanlineCycleIndex = 0;
    	    }
    	    
    	    // ao final envia as amostras de audio para a linha de saida
    	    apu.audioPlayFrame();
    	    
    	    // renderiza as linhas calculadas na area de desenho (canvas)
       	    jamicom.drawFrame();
    		ppu.resetRenderBuffer();
    	 }
    }
    

	public void updateCpuCycles(int ciclos) {
		cpu.updateCurrentCycles(ciclos);
	}
	
	public GamePad getgamepad() {
		return this.gamepad;
	}
		
	public int systemRamRead(int addr) {
		return systemRam[addr];
	}
	
	public void systemRamWrite(int addr, int val) {
		systemRam[addr] = val;
	}
	
	public int getCurrentScanline() {
		return scanline;
	}
	
	// VBlank = intervalo de reposicionamento do feixe de eletrons para inicio de novo quadro, usado para interrupcoes
	 boolean isVBlankPeriod() {
    	if(scanline > 240 && scanline < 261)
    		return true;
    	else
    		return false;
	    	
    }
}