/**
 * Cpu.java
 * Define objeto do tipo CPU (processador central do videogame);
 * implementa os registradores e as operacoes;
 * executa os codigos operacionais (opcodes) do processador 6502;
 * composto de: 3 registradores gerais de 8 bits (A, X, Y), um registrador de status, o contador de programa e o ponteiro da pilha;
 * capaz de executar até 256 operacoes diferentes porem somente as operações documentadas foram implementadas.
 */

package com.jamicom;

class Cpu {
    private static final int CARRY_FLAG = 1;
    private static final int ZERO_FLAG = 2;
    private static final int INTERRUPT_FLAG = 4;
    private static final int DECIMAL_FLAG = 8;
    private static final int BREAK_FLAG = 0x10;
    private static final int BIT_5 = 0x20;
    
    private static final int OVERFLOW_FLAG = 0x40;
    private static final int NEGATIVE_FLAG = 0x80;

    private int regX;
    private int regY;
    private int regA;
    public int regSP;
    private int flagsRegister;
    private int currentCycles;
    int PC;
    private int currentOpcode;
    private int[] opcodeCycles;
    private int deltaCycles;
    int[] opcodos;

    int totalInstruc;
    int totalCiclos;
    private boolean suprimeNmi;
    int currentOpCycles;
    boolean updateInterruptCycles;

    private Mapper mapper;
    
    public Cpu() {
    	opcodeCycles = new int[256];
        regSP = 0xff;
        flagsRegister = BREAK_FLAG;

        // tabela de ciclos de CPU para calcular o numero de operacoes por quadro
        int ciclios[] = { 
        		7, 6, 2, 7, 3, 3, 5, 5, 3, 2, 2, 2, 4, 4, 6, 6,
                2, 5, 2, 7, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 6, 7,
			    6, 6, 2, 7, 3, 3, 5, 5, 4, 2, 2, 2, 4, 4, 6, 6,
			    2, 5, 2, 7, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
			    6, 6, 2, 7, 3, 3, 5, 5, 3, 2, 2, 2, 3, 4, 6, 6,
			    2, 5, 2, 7, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
			    6, 6, 2, 7, 3, 3, 5, 5, 4, 2, 2, 2, 5, 4, 6, 6,
			    2, 5, 2, 7, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
			    2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
			    2, 6, 2, 6, 4, 4, 4, 4, 2, 5, 2, 5, 5, 5, 5, 5,
                2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
                2, 5, 2, 5, 4, 4, 4, 4, 2, 4, 2, 4, 4, 4, 4, 4,
                2, 6, 2, 7, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
                2, 5, 2, 7, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
                2, 6, 3, 7, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
                2, 5, 2, 7, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7 
        };
        opcodos = new int[256];
        for (int n = 0; n < 256; n++)
        	opcodeCycles[n] = ciclios[n];
   
    }

    public void reset(int elPC) {
    	PC = elPC;
        currentOpcode = mapper.ioRead(PC);
    }
    
    public void setup(Mapper mapper) {
    	this.mapper = mapper;
    }

    void setSuprimeNmi(boolean valor) {
    	suprimeNmi = valor;
    }

    boolean getSuprimeNmi() {
    	return suprimeNmi;
    }

    // modos de endereçamento
    int getValImmed() {
    	
        return mapper.ioRead(PC + 1);
        
    }

    int getAddr_Idx_Ind() {
    	
        int addrLsb = (getValImmed() + regX) & 0xFF;
        return mapper.ioRead(addrLsb) +
            (mapper.ioRead(((addrLsb + 1) & 0xFF)) << 8);
    }

    int getAddr_Ind() {
        return mapper.ioRead(getValImmed()) + (mapper.ioRead((getValImmed() + 1) & 0xFF) << 8);
    }

    
    int getAddr_Ind_Idx() {
        return (getAddr_Ind() + regY) & 0xFFFF;        
    }

    
    int getAddr_Abs() {
        return mapper.ioRead(PC + 1) | (mapper.ioRead(PC + 2) << 8);
    }
    
    // metodos para ligar/desligar os flags do registrador de status
    void turnOnFlags(int losflagsRegister) {
        flagsRegister |= losflagsRegister; 
    }

    
    void turnOffFlags(int losflagsRegister) {
    	
        flagsRegister = (flagsRegister & (~(losflagsRegister))) & 0xff;
        
    }
    
    boolean flagIsSet(int elFlago) {
    	
    	if((flagsRegister & elFlago) != 0)
    		return true;
    	else
    		return false;
    	
    }
    
    // operacoes com a pilha
    void pushByte(int elvalor) {
        mapper.ioWrite(0x100 + regSP, elvalor);
        regSP = (regSP - 1) & 0xff; 
    }

    
    int pullByte(int opcodes) {
        PC+= opcodes;
        regSP = (regSP + 1) & 0xff;
        return mapper.ioRead(0x100 + regSP); 
    }
    
    // operacoes com os registradores gerais
    void setRegA(int elvalor) {
    	
        regA = elvalor;
        
        if ((regA & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        else
            turnOffFlags(NEGATIVE_FLAG);
        
        if (regA == 0)
            turnOnFlags(ZERO_FLAG);
        else
            turnOffFlags(ZERO_FLAG);
        
    }
    

    void setRegY(int elvalor) {
    	
        regY = elvalor;
        
        if ((regY & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        else
            turnOffFlags(NEGATIVE_FLAG);
        
        if (regY == 0)
            turnOnFlags(ZERO_FLAG);
        else
            turnOffFlags(ZERO_FLAG);
        
    }

    
    void setRegX(int elvalor) {
    	
        regX = elvalor;
        
        if ((regX & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        else
            turnOffFlags(NEGATIVE_FLAG);
        
        if (regX == 0)
            turnOnFlags(ZERO_FLAG);
        else
            turnOffFlags(ZERO_FLAG);
        
    }

    // correcao da contagem de ciclos (corrige um bug original do processador)
    void checkBugIndIdx() {
    	
        int temp = getAddr_Ind();
        
        if (((temp ^ (temp + regY)) & 0x100) == 0x100)
            currentCycles++;
        
    }

    
    void checkBugAbsInd(int rego) {
    	
        int temp = getAddr_Abs();
        
        if (((temp ^ (temp + rego)) & 0x100) == 0x100)
            currentCycles++;
        
    }
    
    // Operacoes - opcodes
    void opOr(int elOperando) {
    	
        regA |= elOperando;
        
        if ((regA & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        else
            turnOffFlags(NEGATIVE_FLAG);
        
        if (regA == 0)
            turnOnFlags(ZERO_FLAG);
        else
            turnOffFlags(ZERO_FLAG);
        
    }

    
    void opAnd(int elOperando) {
    	
        regA &= elOperando;
        
        if ((regA & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        else
            turnOffFlags(NEGATIVE_FLAG);
        
        if (regA == 0)
            turnOnFlags(ZERO_FLAG);
        else
            turnOffFlags(ZERO_FLAG);
        
    }

    
    void opAndXY(int elReg, int elOffset) {		// El Illegal Opcode 9C op And Y con ((opcode+2)+1) y store en (Abs,X)
        
    	int temp = (mapper.ioRead(PC + 2)+1) & elReg;
    	
        if ((temp & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        else
            turnOffFlags(NEGATIVE_FLAG);
        
        if (temp == 0)
            turnOnFlags(ZERO_FLAG);
        else
            turnOffFlags(ZERO_FLAG);
        
        mapper.ioWrite((getAddr_Abs() + elOffset) & 0xffff, temp);
        
    }

    
    void opEor(int elOperando) {
    	
        regA ^= elOperando;
        
        if ((regA & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        else
            turnOffFlags(NEGATIVE_FLAG);
        
        if (regA == 0)
            turnOnFlags(ZERO_FLAG);
        else
            turnOffFlags(ZERO_FLAG);
        
    }

    
    void opBit(int elOperando) {
    	
        turnOffFlags(NEGATIVE_FLAG | OVERFLOW_FLAG | ZERO_FLAG);
        flagsRegister |= elOperando & 0xC0;
        
        if ((regA & elOperando) == 0)
            turnOnFlags(ZERO_FLAG);
        
    }

    
    int opAsl(int elOperando) {
    	
        if ((elOperando & 0x80) == 0x80)
            turnOnFlags(CARRY_FLAG);
        else
            turnOffFlags(CARRY_FLAG);
        
        elOperando = (elOperando << 1) & 0xFF;
        
        if ((elOperando & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        else
            turnOffFlags(NEGATIVE_FLAG);
        
        if (elOperando == 0)
            turnOnFlags(ZERO_FLAG);
        else
            turnOffFlags(ZERO_FLAG);
        
        return elOperando;
        
    }

    
    int opRol(int elOperando) {
    	
        int tempor = (elOperando & 0x80) == 0x80 ? 1 : 0;
        elOperando = ((elOperando << 1) & 0xFF) | (flagsRegister & CARRY_FLAG);
        turnOffFlags(NEGATIVE_FLAG | ZERO_FLAG | CARRY_FLAG);
        
        if ((elOperando & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        
        if (elOperando == 0)
            turnOnFlags(ZERO_FLAG);
        
        flagsRegister |= tempor;
        return elOperando;
        
    }

    
    int opRor(int elOperando) {
    	
        int tempor = (elOperando & 1) == 1 ? 1 : 0;
        elOperando = (elOperando >> 1) | ((flagsRegister & CARRY_FLAG) << 7);
        turnOffFlags(NEGATIVE_FLAG | ZERO_FLAG | CARRY_FLAG);
        
        if ((elOperando & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        
        if (elOperando == 0)
            turnOnFlags(ZERO_FLAG);
        
        flagsRegister |= tempor;
        return elOperando;
        
    }

    
    void opCmp(int elvalor) {
    	
        int laDiferencia = regA - elvalor;
        
        if (regA >= elvalor)
            turnOnFlags(CARRY_FLAG);
        else
            turnOffFlags(CARRY_FLAG);
        
        if ((laDiferencia & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        else
            turnOffFlags(NEGATIVE_FLAG);
        
        if (laDiferencia == 0)
            turnOnFlags(ZERO_FLAG);
        else
            turnOffFlags(ZERO_FLAG);
        
    }

    
    void opCpx(int elvalor) {
    	
        int laDiferencia = regX - elvalor;
        
        if (regX >= elvalor)
            turnOnFlags(CARRY_FLAG);
        else
            turnOffFlags(CARRY_FLAG);
        
        if ((laDiferencia & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        else
            turnOffFlags(NEGATIVE_FLAG);
        
        if (laDiferencia == 0)
            turnOnFlags(ZERO_FLAG);
        else
            turnOffFlags(ZERO_FLAG);
        
    }

    
    void opCpy(int elvalor) {
    	
        int laDiferencia = regY - elvalor;
        
        if (regY >= elvalor)
            turnOnFlags(CARRY_FLAG);
        else
            turnOffFlags(CARRY_FLAG);
        
        if ((laDiferencia & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        else
            turnOffFlags(NEGATIVE_FLAG);
        
        if (laDiferencia == 0) {
            turnOnFlags(ZERO_FLAG);
        }
        else
            turnOffFlags(ZERO_FLAG);
        
    }

    
    void opAdc(int elOperando) {

        int tempor = elOperando + regA + (flagsRegister & CARRY_FLAG);
        turnOffFlags(NEGATIVE_FLAG | OVERFLOW_FLAG | ZERO_FLAG | CARRY_FLAG);
        
        if ((((regA ^ elOperando) & 0x80) == 0) && (((regA ^ tempor) & 0x80) != 0))
            turnOnFlags(OVERFLOW_FLAG);
        
        if ((tempor & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        
        if ((tempor & 0xFF) == 0)
            turnOnFlags(ZERO_FLAG);
        
        if (tempor > 0xFF)
            turnOnFlags(CARRY_FLAG);
        
        regA = tempor & 0xFF;
        
    }

    
    void opSbc(int elOperandu) {
    	
        elOperandu = (~elOperandu) & 0xFF;
        opAdc(elOperandu);
        
    }

    
    int opIncDec(int elOperando, int elSinal) {
    	
        elOperando = (elOperando + elSinal) & 0xff;
        
        if ((elOperando & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        else
            turnOffFlags(NEGATIVE_FLAG);
        
        if (elOperando == 0)
            turnOnFlags(ZERO_FLAG);
        else
            turnOffFlags(ZERO_FLAG);
        
        return elOperando;
        
    }

    
    int opLsr(int elOperando) {
    	
        if ((elOperando & 1) == 1)
            turnOnFlags(CARRY_FLAG);
        else
            turnOffFlags(CARRY_FLAG);
        
        elOperando >>= 1;
        
        if ((elOperando & 0x80) == 0x80)
            turnOnFlags(NEGATIVE_FLAG);
        else
            turnOffFlags(NEGATIVE_FLAG);
        
        if (elOperando == 0)
            turnOnFlags(ZERO_FLAG);
        else
            turnOffFlags(ZERO_FLAG);
        
        return elOperando;
        
    }

    
    void opBranch(boolean condiccion) {
    	
        if (condiccion) {
        	
            int temp = PC;
            PC = (PC + 2 + (byte)getValImmed()) & 0xffff;
            
            if (((temp ^ PC) & 0x100) == 0x100)
                currentCycles += 2;
            else 
                currentCycles++;
            
        }
        
        else
            PC += 2;
        
    }

    
    void opJmpInd() {
    	
        if (getValImmed() == 0xff)
            PC = mapper.ioRead(getAddr_Abs()) + (mapper.ioRead(getAddr_Abs() - 0xff) << 8);
        else
            PC = mapper.ioRead(getAddr_Abs()) + (mapper.ioRead(getAddr_Abs() + 1) << 8);
        
    }

    
    void opJsr() {

        pushByte((PC + 2) >> 8);
        pushByte((PC + 2) & 0xFF);
        PC = getAddr_Abs();
        
    }

    // manejo de interrupcoes e chamadas de subrotinas
    void opInterrupt(int elPc, int addr, int flagsRegister) {
    	updateInterruptCycles = true;
    	//currentCycles=7;
        pushByte((elPc) >> 8);
        pushByte((elPc) & 0xFF);
        pushByte(flagsRegister);
        turnOnFlags(INTERRUPT_FLAG);
        PC = addr;
        
    }
    
    
    void opRti() {
    	
        flagsRegister = pullByte(0) & 0xef;
        PC = pullByte(0) | (pullByte(0) << 8);
        
    }

    
    void opRts() {
    	
        PC = pullByte(0) | (pullByte(0) << 8);
        PC++;
        
    }

    
    public void handleInterrupts() {
        // 3 tipos de interrupcoes, IRQ gerado pelo jogo, IRQ gerado pela APU, NMI gerado pela PPU ao final de cada quadro

    		if((mapper.getInterruptSignal() & 8) == Globals.INTERRUPT_NMI) {	// NMI
           if (mapper.getNmiDelayFlag()) {
           		mapper.setNmiDelayFlag(false);
           		mapper.getPpu().turnOnFlags(Globals.NMI_FLAG);
           }    	   
           else {
	           mapper.clearInterruptSignal(Globals.INTERRUPT_NMI);
	    	   opInterrupt(PC, mapper.getNmiAddr(), (flagsRegister & 0xef) | BIT_5);
	       }
       }
       
       if(mapper.irqIsRequested()) {	// IRQ

    	   if (mapper.getIrqDelayFlag()) {
    		   mapper.setIrqDelayFlag(false);
    		   return;
    	   }
    	   else if (!flagIsSet(INTERRUPT_FLAG)) {
    		   if((mapper.getInterruptSignal() & 1) == Globals.INTERRUPT_FRAME_IRQ) {

    			   mapper.clearInterruptSignal(Globals.INTERRUPT_FRAME_IRQ);    			   
    			   opInterrupt(PC, mapper.getIrqAddr(), (flagsRegister & 0xef) | BIT_5);
    			   return;
    		   }
    		   else if((mapper.getInterruptSignal() & 2) == Globals.INTERRUPT_DMC_IRQ) {
    			   mapper.clearInterruptSignal(Globals.INTERRUPT_DMC_IRQ);
    			   opInterrupt(PC, mapper.getIrqAddr(), (flagsRegister & 0xef) | BIT_5);
    			   return;
    		   }
    		   else if((mapper.getInterruptSignal() & 4) == Globals.INTERRUPT_MAPPER_IRQ) {
    			   mapper.clearInterruptSignal(Globals.INTERRUPT_MAPPER_IRQ);
    			   opInterrupt(PC, mapper.getIrqAddr(), (flagsRegister & 0xef) | BIT_5);
    			   return;
    		   }
    		   
    	   }
       }
    }
    
    // loop principal - executa um opcode
    int executeInstruction() {
    	
    	// checa se sinalizacao de interrupcao
    	if ((mapper.getInterruptSignal() > 0)) {
    		handleInterrupts();
    	}

    	// carrega o codigo operacional e efetua a respectiva operacao
        currentOpcode = mapper.ioRead(PC);
        deltaCycles = currentCycles;
        currentOpCycles = opcodeCycles[currentOpcode];
        currentCycles += currentOpCycles;
        switch (currentOpcode) {
        	case 0x00: { opInterrupt((PC + 2) & 0xFFFF, mapper.getIrqAddr(), flagsRegister | BREAK_FLAG | BIT_5); break; }
            case 0x01: { opOr(mapper.ioRead(getAddr_Idx_Ind())); PC += 2; break; }
            case 0x04: { PC += 2; break; }

            case 0x05: { opOr(mapper.ioRead(getValImmed())); PC += 2; break; }
            case 0x06: { mapper.ioWrite(getValImmed(), opAsl(mapper.ioRead(getValImmed()))); PC += 2; break; }

            case 0x08: { pushByte(flagsRegister | BREAK_FLAG | 0x20); PC++; break; }
            case 0x09: { opOr(getValImmed()); PC += 2; break; }
            case 0x0A: { setRegA(opAsl(regA)); PC++; break; }

            case 0x0D: { opOr(mapper.ioRead(getAddr_Abs())); PC += 3; break; }
            case 0x0E: { mapper.ioWrite(getAddr_Abs(), opAsl(mapper.ioRead(getAddr_Abs()))); PC += 3; break; }
            case 0x10: { opBranch(!flagIsSet(NEGATIVE_FLAG)); break; }
            case 0x11: { checkBugIndIdx(); opOr(mapper.ioRead(getAddr_Ind_Idx())); PC += 2; break; }

            case 0x15: { opOr(mapper.ioRead((getValImmed() + regX) & 0xFF)); PC += 2; break; }
            case 0x16: { mapper.ioWrite((getValImmed() + regX) & 0xff, opAsl(mapper.ioRead((getValImmed() + regX) & 0xff))); PC += 2; break; }

            case 0x18: { turnOffFlags(CARRY_FLAG); PC++; break; }
            case 0x19: { checkBugAbsInd(regY); opOr(mapper.ioRead((getAddr_Abs() + regY) & 0xffff)); PC += 3; break; }
            case 0x1A: { PC++; break; }

            case 0x1D: { checkBugAbsInd(regX); opOr(mapper.ioRead((getAddr_Abs() + regX) & 0xffff)); PC += 3; break; }
            case 0x1E: { mapper.ioWrite((getAddr_Abs() + regX) & 0xffff, opAsl(mapper.ioRead((getAddr_Abs() + regX) & 0xffff))); PC += 3; break; }

            case 0x20: { opJsr(); break; }
            case 0x21: { opAnd(mapper.ioRead(getAddr_Idx_Ind())); PC += 2; break; }

            case 0x24: { opBit(mapper.ioRead(getValImmed())); PC += 2; break; }
            case 0x25: { opAnd(mapper.ioRead(getValImmed())); PC += 2; break; }
            case 0x26: { mapper.ioWrite(getValImmed(), opRol(mapper.ioRead(getValImmed()))); PC += 2;  break; }
            case 0x28: { flagsRegister = pullByte(1) & 0xef; mapper.setIrqDelayFlag(true); break; }
            case 0x29: { opAnd(getValImmed()); PC += 2; break; }
            case 0x2A: { setRegA(opRol(regA)); PC++; break; }
            case 0x2C: { opBit(mapper.ioRead(getAddr_Abs())); PC += 3; break; }
            case 0x2D: { opAnd(mapper.ioRead(getAddr_Abs())); PC += 3; break; }
            case 0x2E: { mapper.ioWrite(getAddr_Abs(), opRol(mapper.ioRead(getAddr_Abs()))); PC += 3; break; }
 
            case 0x30: { opBranch(flagIsSet(NEGATIVE_FLAG)); break; }
            case 0x31: { checkBugIndIdx(); opAnd(mapper.ioRead(getAddr_Ind_Idx())); PC += 2; break; }
            case 0x35: { opAnd(mapper.ioRead((getValImmed() + regX) & 0xFF)); PC += 2; break; }
            case 0x36: { mapper.ioWrite((getValImmed() + regX) & 0xff, opRol(mapper.ioRead((getValImmed() + regX) & 0xff))); PC += 2; break; }
            case 0x38: { turnOnFlags(CARRY_FLAG); PC++; break; }
            case 0x39: { checkBugAbsInd(regY); opAnd(mapper.ioRead((getAddr_Abs() + regY) & 0xffff)); PC += 3; break; }
            case 0x3D: { checkBugAbsInd(regX); opAnd(mapper.ioRead((getAddr_Abs() + regX) & 0xffff)); PC += 3; break; }
            case 0x3E: { mapper.ioWrite((getAddr_Abs() + regX) & 0xffff, opRol(mapper.ioRead((getAddr_Abs() + regX) & 0xffff))); PC += 3; break; }
            case 0x40: { opRti(); break; }
            case 0x41: { opEor(mapper.ioRead(getAddr_Idx_Ind())); PC += 2; break; }
            case 0x45: { opEor(mapper.ioRead(getValImmed())); PC += 2; break; }
            case 0x46: { mapper.ioWrite(getValImmed(), opLsr(mapper.ioRead(getValImmed()))); PC += 2;  break; }
            case 0x48: { pushByte(regA); PC++; break; }
            case 0x49: { opEor(getValImmed()); PC += 2; break; }
            case 0x4A: { setRegA(opLsr(regA)); PC++; break; }
            case 0x4C: { PC = getAddr_Abs(); break; }
            case 0x4D: { opEor(mapper.ioRead(getAddr_Abs())); PC += 3; break; }
            case 0x4E: { mapper.ioWrite(getAddr_Abs(), opLsr(mapper.ioRead(getAddr_Abs()))); PC += 3; break; }
            case 0x50: { opBranch(!flagIsSet(OVERFLOW_FLAG)); break; }
            case 0x51: { checkBugIndIdx(); opEor(mapper.ioRead(getAddr_Ind_Idx())); PC += 2; break; }
            case 0x55: { opEor(mapper.ioRead((getValImmed() + regX) & 0xFF)); PC += 2; break; }
            case 0x56: { mapper.ioWrite((getValImmed() + regX) & 0xff, opLsr(mapper.ioRead((getValImmed() + regX) & 0xff))); PC += 2; break; }
            case 0x58: { turnOffFlags(INTERRUPT_FLAG); mapper.setIrqDelayFlag(true); PC++; break; }
            case 0x59: { checkBugAbsInd(regY); opEor(mapper.ioRead((getAddr_Abs() + regY) & 0xffff)); PC += 3; break; }
            case 0x5A: { PC++; break; }

            case 0x5D: { checkBugAbsInd(regX); opEor(mapper.ioRead((getAddr_Abs() + regX) & 0xffff)); PC += 3; break; }
            case 0x5E: { mapper.ioWrite((getAddr_Abs() + regX) & 0xffff, opLsr(mapper.ioRead((getAddr_Abs() + regX) & 0xffff))); PC += 3; break; }
            case 0x60: { opRts(); break; }
            case 0x61: { opAdc(mapper.ioRead(getAddr_Idx_Ind())); PC += 2; break; }
            case 0x65: { opAdc(mapper.ioRead(getValImmed())); PC += 2; break; }
            case 0x66: { mapper.ioWrite(getValImmed(), opRor(mapper.ioRead(getValImmed()))); PC += 2;  break; }
            case 0x68: { setRegA(pullByte(1)); break; }
            case 0x69: { opAdc(getValImmed()); PC += 2; break; }
            case 0x6A: { setRegA(opRor(regA)); PC++; break; }
            case 0x6C: { opJmpInd(); break; }
            case 0x6D: { opAdc(mapper.ioRead(getAddr_Abs())); PC += 3; break; }
            case 0x6E: { mapper.ioWrite(getAddr_Abs(), opRor(mapper.ioRead(getAddr_Abs()))); PC += 3; break; }
            case 0x70: { opBranch(flagIsSet(OVERFLOW_FLAG)); break; }
            case 0x71: { checkBugIndIdx(); opAdc(mapper.ioRead(getAddr_Ind_Idx())); PC += 2; break; }
            case 0x75: { opAdc(mapper.ioRead((getValImmed() + regX) & 0xFF)); PC += 2; break; }
            case 0x76: { mapper.ioWrite((getValImmed() + regX) & 0xff, opRor(mapper.ioRead((getValImmed() + regX) & 0xff))); PC += 2; break; }
            case 0x78: { turnOnFlags(INTERRUPT_FLAG); mapper.setIrqDelayFlag(true); PC++; break; }
            case 0x79: { checkBugAbsInd(regY); opAdc(mapper.ioRead((getAddr_Abs() + regY) & 0xffff)); PC += 3; break; }
            case 0x7A: { PC++; break;}

            case 0x7C: { PC += 3; break; }
            case 0x7D: { checkBugAbsInd(regX); opAdc(mapper.ioRead((getAddr_Abs() + regX) & 0xffff)); PC += 3; break; }
            case 0x7E: { mapper.ioWrite((getAddr_Abs() + regX) & 0xffff, opRor(mapper.ioRead((getAddr_Abs() + regX) & 0xffff))); PC += 3; break; }
 
            case 0x80: { PC+=2; break; }
            case 0x81: { mapper.ioWrite(getAddr_Idx_Ind(), regA); PC += 2; break; }
            case 0x84: { mapper.ioWrite(getValImmed(), regY); PC += 2; break; }
            case 0x85: { mapper.ioWrite(getValImmed(), regA); PC += 2; break; }
            case 0x86: { mapper.ioWrite(getValImmed(), regX); PC += 2; break; }
            case 0x88: { setRegY(opIncDec(regY, -1)); PC++; break; }
            case 0x8A: { setRegA(regX); PC++; break; }
            case 0x8C: { mapper.ioWrite(getAddr_Abs(), regY); PC += 3; break; }
            case 0x8D: { mapper.ioWrite(getAddr_Abs(), regA); PC += 3; break; }
            case 0x8E: { mapper.ioWrite(getAddr_Abs(), regX); PC += 3; break; }
            case 0x90: { opBranch(!flagIsSet(CARRY_FLAG)); break; }
            case 0x91: { mapper.ioWrite(getAddr_Ind_Idx(), regA); PC += 2; break; }
            case 0x94: { mapper.ioWrite((getValImmed() + regX) & 0xff, regY); PC += 2; break; }
            case 0x95: { mapper.ioWrite((getValImmed() + regX) & 0xff, regA); PC += 2; break; }
            case 0x96: { mapper.ioWrite((getValImmed() + regY) & 0xff, regX); PC += 2; break; }
            case 0x98: { setRegA(regY); PC++; break; }
            case 0x99: { mapper.ioWrite((getAddr_Abs() + regY) & 0xffff, regA); PC += 3; break; }
            case 0x9A: { regSP = regX; PC++; break; }
            case 0x9C: { opAndXY(regY, regX); PC += 3; break; }
            case 0x9E: { opAndXY(regX, regY); PC += 3; break; }
            case 0x9D: { mapper.ioWrite((getAddr_Abs() + regX) & 0xffff, regA); PC += 3; break; }
            case 0xA0: { setRegY(getValImmed()); PC += 2; break; }
            case 0xA1: { setRegA(mapper.ioRead(getAddr_Idx_Ind())); PC += 2; break; }
            case 0xA2: { setRegX(getValImmed()); PC += 2; break; }
            case 0xA4: { setRegY(mapper.ioRead(getValImmed())); PC += 2; break; }
            case 0xA5: { setRegA(mapper.ioRead(getValImmed())); PC += 2; break; }
            case 0xA6: { setRegX(mapper.ioRead(getValImmed())); PC += 2; break; }
            case 0xA8: { setRegY(regA); PC++; break; }
            case 0xA9: { setRegA(getValImmed()); PC += 2; break; }
            case 0xAA: { setRegX(regA); PC++; break; }
            case 0xAC: { setRegY(mapper.ioRead(getAddr_Abs())); PC += 3;; break; }
            case 0xAD: { setRegA(mapper.ioRead(getAddr_Abs())); PC += 3; break; }
            case 0xAE: { setRegX(mapper.ioRead(getAddr_Abs())); PC += 3; break; }
            case 0xB0: { opBranch(flagIsSet(CARRY_FLAG)); break; }
            case 0xB1: { checkBugIndIdx(); setRegA(mapper.ioRead(getAddr_Ind_Idx())); PC += 2; break; }
            case 0xB4: { setRegY(mapper.ioRead((getValImmed() + regX) & 0xFF)); PC += 2; break; }
            case 0xB5: { setRegA(mapper.ioRead((getValImmed() + regX) & 0xFF)); PC += 2; break; }
            case 0xB6: { setRegX(mapper.ioRead((getValImmed() + regY) & 0xFF)); PC += 2; break; }
            case 0xB8: { turnOffFlags(OVERFLOW_FLAG); PC++; break; }
            case 0xB9: { checkBugAbsInd(regY); setRegA(mapper.ioRead((getAddr_Abs() + regY) & 0xffff)); PC += 3; break; }
            case 0xBA: { setRegX(regSP); PC++; break; }
            case 0xBC: { checkBugAbsInd(regX); setRegY(mapper.ioRead((getAddr_Abs() + regX) & 0xffff)); PC += 3; break; }
            case 0xBD: { checkBugAbsInd(regX); setRegA(mapper.ioRead((getAddr_Abs() + regX) & 0xffff)); PC += 3; break; }
            case 0xBE: { checkBugAbsInd(regY); setRegX(mapper.ioRead((getAddr_Abs() + regY) & 0xffff)); PC += 3; break; }
            case 0xC0: { opCpy(getValImmed()); PC += 2; break; }
            case 0xC1: { opCmp(mapper.ioRead(getAddr_Idx_Ind())); PC += 2; break; }
            case 0xC4: { opCpy(mapper.ioRead(getValImmed())); PC += 2; break; }
            case 0xC5: { opCmp(mapper.ioRead(getValImmed())); PC += 2; break; }
            case 0xC6: { mapper.ioWrite(getValImmed(), opIncDec(mapper.ioRead(getValImmed()), -1)); PC += 2;  break; }
            case 0xC8: { setRegY(opIncDec(regY, 1)); PC++; break;  }
            case 0xC9: { opCmp(getValImmed()); PC += 2; break; }
            case 0xCA: { setRegX(opIncDec(regX, -1)); PC++; break; }
            case 0xCC: { opCpy(mapper.ioRead(getAddr_Abs())); PC += 3; break; }
            case 0xCD: { opCmp(mapper.ioRead(getAddr_Abs())); PC += 3; break;  }
            case 0xCE: { mapper.ioWrite(getAddr_Abs(), opIncDec(mapper.ioRead(getAddr_Abs()), -1)); PC += 3; break; }
            case 0xD0: { opBranch(!flagIsSet(ZERO_FLAG)); break; }
            case 0xD1: { checkBugIndIdx(); opCmp(mapper.ioRead(getAddr_Ind_Idx())); PC += 2;  break; }
         
            case 0xD4: { PC += 2; break; }
            case 0xD5: { opCmp(mapper.ioRead((getValImmed() + regX) & 0xFF)); PC += 2; break; }
            case 0xD6: { mapper.ioWrite((getValImmed() + regX) & 0xff, opIncDec(mapper.ioRead((getValImmed() + regX) & 0xff), -1)); PC += 2; break; }
            case 0xD7: { opCmp((mapper.ioRead(getAddr_Idx_Ind())-1) & 0xFF); PC += 2; break; }
            
            case 0xD8: { turnOffFlags(DECIMAL_FLAG); PC++; break; }
            case 0xD9: { checkBugAbsInd(regY); opCmp(mapper.ioRead((getAddr_Abs() + regY) & 0xffff)); PC += 3; break; }
            case 0xDA: { PC++; break; }
            case 0xDD: { checkBugAbsInd(regX); opCmp(mapper.ioRead((getAddr_Abs() + regX) & 0xffff)); PC += 3; break; }
            case 0xDE: { mapper.ioWrite((getAddr_Abs() + regX) & 0xffff, opIncDec(mapper.ioRead((getAddr_Abs() + regX) & 0xffff), -1)); PC += 3; break; }
            case 0xDF: { checkBugAbsInd(regX); opCmp(((mapper.ioRead((getAddr_Abs() + regX) & 0xffff)) - 1) & 0xFF); PC += 3; break; }
            
            case 0xE0: { opCpx(getValImmed()); PC += 2; break; }
            case 0xE1: { opSbc(mapper.ioRead(getAddr_Idx_Ind())); PC += 2; break; }
            case 0xE2: { PC+=2; break; }

            case 0xE4: { opCpx(mapper.ioRead(getValImmed())); PC += 2; break; }
            case 0xE5: { opSbc(mapper.ioRead(getValImmed())); PC += 2; break; }
            case 0xE6: { mapper.ioWrite(getValImmed(), opIncDec(mapper.ioRead(getValImmed()), 1)); PC += 2;  break; }
            case 0xE8: { setRegX(opIncDec(regX, 1)); PC++; break; }
            case 0xE9: { opSbc(getValImmed()); PC += 2; break; }
            case 0xEA: { PC++; break; }
            case 0xEC: { opCpx(mapper.ioRead(getAddr_Abs())); PC += 3; break; }
            case 0xED: { opSbc(mapper.ioRead(getAddr_Abs())); PC += 3; break; }
            case 0xEE: { mapper.ioWrite(getAddr_Abs(), opIncDec(mapper.ioRead(getAddr_Abs()), 1)); PC += 3; break; }
            case 0xF0: { opBranch(flagIsSet(ZERO_FLAG)); break; }
            case 0xF1: { checkBugIndIdx(); opSbc(mapper.ioRead(getAddr_Ind_Idx())); PC += 2; break; }
            case 0xF5: { opSbc(mapper.ioRead((getValImmed() + regX) & 0xFF)); PC += 2; break; }
            case 0xF6: { mapper.ioWrite((getValImmed() + regX) & 0xff, opIncDec(mapper.ioRead((getValImmed() + regX) & 0xff), 1)); PC += 2; break; }
            case 0xF8: { turnOnFlags(DECIMAL_FLAG); PC++; break; }
            case 0xF9: { checkBugAbsInd(regY); opSbc(mapper.ioRead((getAddr_Abs() + regY) & 0xffff)); PC += 3; break; }
            case 0xFA: { PC++; break; }
            case 0xFD: { checkBugAbsInd(regX); opSbc(mapper.ioRead((getAddr_Abs() + regX) & 0xffff)); PC += 3; break; }
            case 0xFE: { mapper.ioWrite((getAddr_Abs() + regX) & 0xffff, opIncDec(mapper.ioRead((getAddr_Abs() + regX) & 0xffff), 1)); PC += 3; break; }
            case 0xFF: { 
            			 int temp = opIncDec(mapper.ioRead((getAddr_Abs() + regX) & 0xffff), 1);
            			 mapper.ioWrite((getAddr_Abs() + regX) & 0xffff, temp);
            			 opSbc(temp);
            			 PC += 3;
            			 break; 
            }
            default:
            	System.out.printf("Invalid Opcode: %x \n", currentOpcode);
            	break;          
        
        }
        if(updateInterruptCycles) {
        	currentCycles+=7;
        	updateInterruptCycles = false;
        }
        if(mapper.getUpdateDmcCycles()) {
        	currentCycles+=4;
        	mapper.setUpdateDmcCycles(false);
        }
        if(mapper.getUpdateDmaCycles()) {
        	currentCycles+=513;
        	mapper.setUpdateDmaCycles(false);
        }
        return currentCycles - deltaCycles;

    }
    
    int getCurrentCycles() {
    	
    	return currentCycles;
    
    }
    

    void updateCurrentCycles(int cycles) {
    	currentCycles += cycles;
    }
    
    int getLastOpCycles() {
    	return currentCycles - deltaCycles;
    }
    
}

