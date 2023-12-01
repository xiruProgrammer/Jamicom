/**
 * Cartridge.java
 * Define objeto do tipo Cartridge (Cartucho) no formato de operação pelo sistema
 * Arquivos de ROM representam a imagem do cartucho com a seguinte estrutura:
 * 1) Cabeçalho de 16 bytes contendo numero do mapper, numero de bancos, etc;
 * 2) Um ou mais bancos de 16 kilobytes de memoria de programa (PRG-ROM)
 * 3) Um ou mais bancos de 8 kilobytes de memoria de caracteres graficos (CHR-ROM)
 */

package com.jamicom;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class Cartridge {
	private static final int PRG_BYTE  = 4;
	private static final int CHR_BYTE  = 5;
	private static final int MAPPER_BYTE = 6;
	private static final int HEADER_SIZE = 16;
	
	private int[] romHeader;
	private int[] prg;
	private int[] chr;
	private String nomeRom;
	
	public Cartridge(File fileName) throws IOException {
	
		InputStream input;
		ZipFile zipFile;
		
		// abre o arquivo no path repassado pela classe principal
		String fullPath = fileName.getCanonicalPath().toLowerCase();
		if(fullPath.endsWith(".zip")) {
			zipFile = new ZipFile(fileName);
			ZipEntry entry = zipFile.entries().nextElement();
			input = zipFile.getInputStream(entry);	
		}
		else
			input = new FileInputStream(fileName);
		
		// extrai o cabe�alho
		romHeader = new int[HEADER_SIZE];
		for(int n=0; n < HEADER_SIZE; n++)
			romHeader[n] = input.read();
		
		// obtem o numero de bancos de programa de 16KB do cabe�alho e carrega em array
		int prgSize = getNumPrg() * Globals.BANK_16K;
		prg = new int[prgSize];
		
		for(int n=0; n < prgSize; n++)
			prg[n] = input.read();

		// obtem o numero de bancos de caracteres de 8KB do cabe�alho e carrega em array
		if(getNumChr() == 0)
			chr = new int[Globals.BANK_8K * 2];
		else
			chr = new int[Globals.BANK_8K * getNumChr()];
		
		for(int n=0; n < Globals.BANK_8K * getNumChr(); n++)
			chr[n] = input.read();
		
		input.close();
	}

	public int getNumPrg() {
	    return romHeader[PRG_BYTE];
	}

	
	public int getNumChr() {
	    return romHeader[CHR_BYTE];
	}

	// obtem numero do mapper (descricao na classe Jamicom)
	public int getMapperNum() {
	    return (romHeader[MAPPER_BYTE] >> 4) | (romHeader[MAPPER_BYTE + 1] & 0xF0);
	}

	// obtem tipo do espelhamento da memoria de video para jogos sem VRAM extra no cartucho
	public int getMirror() {
	    return romHeader[MAPPER_BYTE] & 1;
	}

	// checar se o cartucho dispoe de VRAM extra neste caso o espelhamento nao se aplica
	public boolean is4ScreenNt() {
	    return (romHeader[MAPPER_BYTE] & 8) == 8 ? true : false;
	}

	// metodos de R/W dos bancos de memoria
	public int prgRead(int addr) {
		return prg[addr];
	}
	
	public void chrWrite(int addr, int value) {
		chr[addr] = value;
	}

	public int chrRead(int addr) {	
		return chr[addr];
	}
	
	public String getNombre() {
		return nomeRom;
	}
}
