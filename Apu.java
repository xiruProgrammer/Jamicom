/**
 * Apu.java
 * Define objeto do tipo APU responsável pela geração de áudio
 * sintetiza 5 canais de som reproduzindo o audio original do videogame:
 * dois canais produzem ondas retangulares simples (simula som de isntrumentos de corda);
 * um canal produz onda triangular (simula som de instrumentos de sopro ex flauta);
 * um canal produz ruidos (explosoes, tiros);
 * um canal reproduz amostras PCM digitalizadas (vozes, percussao);
 * as musicas sao produzidas modulando-se intensidade, timbre, decaimento das ondas
 * os cinco canais sao mixados e a amostra final enviada para o streaming de audio
 * a APU produz amostras na mesma frequencia da CPU (1.76 Mhz); 
 * o audio dos computadores modernos tipicamente utiliza frequencia de 48 Khz;
 * entao a cada 37 amostras produzidas, uma é enviada para reprodução 
 */

package com.jamicom;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;

class Apu { 

	// variaveis internas usadas no calculo das ondas
	private static final byte dutyWaves[] = {
	   	0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0,
    	0, 1, 1, 1, 1, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1 
	};
    
    private static final byte trgWaves[] = {
		0xF, 0xE, 0xD, 0xC, 0xB, 0xA, 9, 8, 7, 6,
		5, 4, 3, 2, 1, 0, 0, 1, 2, 3, 4, 5, 6, 7,
		8, 9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF 
    };
    
    private static final int lengthCounterValues[] = {
		0x0A, 0xFE, 0x14, 0x02, 0x28, 0x04, 0x50, 0x06,
		0xA0, 0x08, 0x3C, 0x0A, 0x0E, 0x0C, 0x1A, 0x0E,
		0x0C, 0x10, 0x18, 0x12, 0x30, 0x14, 0x60, 0x16,
		0xC0, 0x18, 0x48, 0x1A, 0x10, 0x1C, 0x20, 0x1E 
    };
    
    private static final int noiseFreq[] = {
		4, 8, 0x10, 0x20, 0x40, 0x60, 0x80, 0xA0, 0xCA,
		0xFE, 0x17C, 0x1FC, 0x2FA, 0x3F8, 0x7F2, 0xFE4
    };
    
    private static final  int dmcTimerValues[] = { 

    	428, 380, 340, 320, 286, 254, 226, 214, 190, 
    	160, 142, 128, 106,  84,  72,  54
    
    };

    // variaveis internas uma para cada canal
    private int[] channelOutput;            
    private int[] channelSwitch;            
    private int[] channelLengthCounter;       
    private int[] channelTimer;               
    private int[] lengthCounterHaltFlag;    
    private int[] sqrLoopEnvelope;          
    private short[] sqrVolume;              
    private short[] sqrVolEnvelope;         
    private int[] sqrEnvelopeCounter;      
    private int[] sqrDutyActual;            
    private int[] sqrDutyOffset;            
    private boolean[] constVolSqrFlag;          
    private boolean[] RestartSqrEnv;        
    private int[] sqrTimerReload;           
    private boolean[] sqrSweepEnableFlag;       
    private boolean[] sqrSweepReloadFlag;       
    private boolean[] sqrSweepNegFlag;         
    private int[] sqrSweepShift;            
    private int[] sqrSweepCounter;         
    private int[] sqrSweepCounterReload;    
    private int[] sqrRegister0;    		
    
    private int linearCounterHaltFlag;       
    private int linearCounterControlFlag;    
    private int trgLinearCounter;            
    private int trgWaveOffset;             
    private int[] trgRegisters;    		
    
    private int noiseLengthCounterHalt;
    private int noiseConstVolumeFlag;
    private int noiseVolume;
    private int noiseVolumeEnvelope;
    private int noiseTimer;
    private int noiseLengthCounter;
    private boolean noiseRestartEnv;
    private int noiseEnvelopeCounter;
    private int noiseShiftRegister;
    private int noiseFeedbackBit;
    private int noiseModeRegister;          
    private int noiseOutput;
    private int[] noiseRegisters;    			
    
    private int dmcLoopFlag;                 
    private int dmcTimer;                    
    private int dmcSampleBuffer;               
    private byte dmcDac = 0;                      
    private int dmcShiftRemainBits;         
    private int dmcRemainSamples;           
    private int dmcSampleAddr;         
    private int[] dmcRegisters;    			
    private int frameCountersteps;        
    private int frameCounterstepsCounter;
    private int frameCounter;               
    private int frameCounterIrqInhFlag;      
    private int sampleCounter;             
    private  AudioFormat audioFormat;
	private  SourceDataLine dataLine;
	private byte[] frameBuffer;
	private int frameBufferWriteCursor;
	private int frameBufferLecturaCursor;
	private int frameIrqFlag; 
	public int dmcInterruptEnable;         
	public int dmcIrqFlag;
	boolean dmcSilenceFlag;
	private float[] pulse_table;
	private float[] tnd_table;
    boolean updateDmcCycles;
    int reloadDmcSampleAddr;
    int pulseSoma, tndSoma;
    private boolean frameSkip;
	private Mapper mapper;
	
	// numero de amostras por quadro * numero de bytes por amostra ( amostras de 16 bits)
	final int AUDIO_BUFFER_SIZE = Globals.FRAMEBUFFER_SIZE * 2;
	
    public Apu() {
    	channelOutput = new int[3];
    	channelSwitch = new int[5];
    	channelLengthCounter = new int[3];
    	channelTimer = new int[3];
    	lengthCounterHaltFlag = new int[3];
    	sqrLoopEnvelope = new int[2];
    	sqrVolume = new short[2];
    	sqrVolEnvelope = new short[2];
        sqrEnvelopeCounter = new int[2];      
        sqrDutyActual = new int[2];            
        sqrDutyOffset = new int[2];            
        constVolSqrFlag = new boolean[2];          
        RestartSqrEnv = new boolean[2];         
        sqrTimerReload = new int[2];           
        sqrSweepEnableFlag = new boolean[2];       
        sqrSweepReloadFlag = new boolean[2];       
        sqrSweepNegFlag = new boolean[2];         
        sqrSweepShift = new int[2];            
        sqrSweepCounter = new int[2];         
        sqrSweepCounterReload = new int[2];    
        sqrRegister0 = new int[2];
        trgRegisters = new int[3];
        noiseRegisters = new int[2];
        dmcRegisters = new int[3];

        // necessario manter volume de 4 vezes o buffer para o som ser reproduzido sem cortes
        frameBuffer = new byte[AUDIO_BUFFER_SIZE * 4];
        frameCountersteps = 4;
        frameCounterIrqInhFlag = 4;
        noiseShiftRegister = 1;
        noiseModeRegister = 1;
        dmcShiftRemainBits = 7;
        pulse_table = new float[31];
        tnd_table = new float[203];

        // variaveis usadas no calculo da mixagem dos canais
        for(int n = 0; n < 31; n++) {
        	pulse_table [n] = (float) (95.52 / (8128.0 / n + 100) * 0xffff);
        }
        
        for(int n =0; n < 203; n++) {
        	tnd_table [n] = (float) (163.67 / (24329.0 / n + 100) * 0xffff);
        }
		
        // solicita ao sistema interface de audio nas configuracoes pre-definidas
        audioFormat = new AudioFormat(Globals.SAMPLE_RATE, 
        		Globals.BITS_PER_SAMPLE, Globals.AUDIO_CHANNELS, true, true);
		
        try {
        	// abre linha (streaming) para envio das amostras ao audio da maquina hospedeira
			dataLine = AudioSystem.getSourceDataLine(audioFormat);
			dataLine.open(audioFormat);
			dataLine.start();
		    dataLine.flush();
			
		}
        
		catch(LineUnavailableException e) {
			System.out.println("Audio format not supported.");
		}        
    }
    
    public void setup(Mapper mapper) {
    	this.mapper = mapper;
    }

    // atualiza os canais e gera 3 amostras por linha (786 amostras por quadro)
    void audioUpdate(int cicl) {
    	if(frameSkip == true)
    		return;
        for (int n = 0; n < Globals.CYCLES_PER_LINE; n++) {
        	
            if (frameCounter++ > Globals.APU_FRAME_CYCLES) {
                frameCounter = 0;
                executeFrameCounter();
            }
            
            if (--channelTimer[Globals.TRIANGLE] <= 0)
                updateTriangleWave();
            
            if (--dmcTimer < 0)
                updateDmcWave();
            
            if (n % 2 == 1) {      
            	channelTimer[Globals.SQUARE1]--;
                channelTimer[Globals.SQUARE2]--;
                noiseTimer--;
            }
            updateSquareWaves(Globals.SQUARE1);
            updateSquareWaves(Globals.SQUARE2);
            updateNoiseWave();
            updateSquareOutputs(Globals.SQUARE1);
            updateSquareOutputs(Globals.SQUARE2);

            sampleCounter++;
            pulseSoma += pulse_table [channelOutput[Globals.SQUARE1] + channelOutput[Globals.SQUARE2] ];
            tndSoma +=tnd_table [3 * channelOutput[Globals.TRIANGLE] + 2 * noiseOutput + dmcDac];
            
            // preenche o buffer com uma a cada 37 amostras produzidas
            if (sampleCounter == Globals.CYCLES_PER_SAMPLE) {
                sampleCounter = 0;
                int finalSample =  ((pulseSoma + tndSoma) / Globals.CYCLES_PER_SAMPLE) ^ 0x8000;
                pulseSoma = tndSoma = 0;
                frameBuffer[frameBufferWriteCursor++] = (byte)(finalSample >>8);
                frameBuffer[frameBufferWriteCursor++] = (byte) (finalSample &0xff);
 
                if (frameBufferWriteCursor == AUDIO_BUFFER_SIZE * 4)
                    frameBufferWriteCursor = 0;
            
            }
        
        }        	
    }

    // envia o buffer para o streaming ao final do quadro
    void audioPlayFrame() throws InterruptedException {
        byte[] laBuffer = new byte[AUDIO_BUFFER_SIZE];
        for (int n = 0; n < AUDIO_BUFFER_SIZE; n++) {
	        laBuffer[n] = Globals.IS_HALT? 0 : (byte) frameBuffer[frameBufferLecturaCursor++];
	        if(frameBufferWriteCursor - frameBufferLecturaCursor > AUDIO_BUFFER_SIZE)
	        	frameSkip = true;
	        else
	        	frameSkip = false;
	        if (frameBufferLecturaCursor == AUDIO_BUFFER_SIZE * 4)
	        	frameBufferLecturaCursor = 0;        
        }

        dataLine.write(laBuffer, 0, AUDIO_BUFFER_SIZE);

        int available = dataLine.available();
        while(available < (Globals.SAMPLE_RATE - (AUDIO_BUFFER_SIZE)))
        	available = dataLine.available();
    }
    
    // registrador $4015 permite ativar ou desativar os canais
    void toggleChannel(int elBalor) {
        for (int n = 0; n < 5; n++) {
            if (((elBalor >> n) & 1) == 1) { // Bit set, activa el channel
            	if(n == 4) {
            		if(dmcRemainSamples == 0) {
            			dmcSampleAddr= reloadDmcSampleAddr;            			
            			dmcRemainSamples = (dmcRegisters[Globals.DMC_REGISTER_3] * 16) + 1; // ACA DESACTIVADO DABA FLATULENCIAS EN GUARDIC GAIDEN
            		}
            	}
            	channelSwitch[n] = 0x7FFFF;
            }
            else {                           // Bit Reset, desactiva el channel; la desactivacion reseta el lengthCounter
                channelSwitch[n] = 0;
                
                if(n < 3)
                	channelLengthCounter[n] = 0;
                
                else if(n == 3)
                	noiseLengthCounter = 0;
                else if(n == 4)
                	dmcRemainSamples = 0;
            }
        }        
    }

    // atualizacao das ondas retangulares
    void updateLengthCounters() {
        for (int n = 0; n < 3; n++) {
            if ((lengthCounterHaltFlag[n] & channelLengthCounter[n]) != 0)
                channelLengthCounter[n]--;
        }
    }

    // atualizacao da onda triangular
    void updateLinearCounter() {
    	
        if (linearCounterHaltFlag == 1)
            trgLinearCounter = trgRegisters[Globals.TRIANGLE_REGISTER_0] & 0x7F;
        
        if ((trgLinearCounter > 0) && (linearCounterHaltFlag == 0))
            trgLinearCounter--;
        
        if (linearCounterControlFlag == 0)
            linearCounterHaltFlag = 0;
        
    }

    
    void updateTriangleWave() {
    	
        channelTimer[Globals.TRIANGLE] = (trgRegisters[Globals.TRIANGLE_REGISTER_1] | 
        		(trgRegisters[Globals.TRIANGLE_REGISTER_2] & 7) << 8) + 1;
        
        if ((trgLinearCounter > 0) && (channelLengthCounter[Globals.TRIANGLE] > 0)) {
            trgWaveOffset = (trgWaveOffset + 1) & 0x1F;
             if(channelTimer[Globals.TRIANGLE] < 2)
            		channelOutput[Globals.TRIANGLE] =  1;
            else
            	channelOutput[Globals.TRIANGLE] = trgWaves[trgWaveOffset] & channelSwitch[Globals.TRIANGLE];
        }
        
    }

    // Frame Counter : responsavel por atualizar os canais na forma de manter a cadencia do som do jogo
    void executeFrameCounter() {
        switch (frameCounterstepsCounter) {
        
	        case 0: // step 1
	            UpdataSqrEnvelope(Globals.SQUARE1);
	            UpdataSqrEnvelope(Globals.SQUARE2);
	            updateNoiseEnvelope();
	            updateLinearCounter();
	            frameCounterstepsCounter++;
	            break;
	            
	        case 1: // step 2
	            updateLengthCounters();
	            updateNoiseLengthCounter();
	            updateSquareSweep(Globals.SQUARE1);
	            updateSquareSweep(Globals.SQUARE2);
	            UpdataSqrEnvelope(Globals.SQUARE1);
	            UpdataSqrEnvelope(Globals.SQUARE2);
	            updateNoiseEnvelope();
	            updateLinearCounter();
	            frameCounterstepsCounter++;
	            break;
	            
	        case 2: // step 3
	            UpdataSqrEnvelope(Globals.SQUARE1);
	            UpdataSqrEnvelope(Globals.SQUARE2);
	            updateNoiseEnvelope();
	            updateLinearCounter();
	            frameCounterstepsCounter++;
	            break;
	            
	        case 3: // step 4 - SE 5 stepS, PULA PARA EL PROXIMO
	            
	        	if (frameCountersteps == 4) {
	        		
	                updateLengthCounters();
	                updateNoiseLengthCounter();
	                updateSquareSweep(Globals.SQUARE1);
	                updateSquareSweep(Globals.SQUARE2);
	                UpdataSqrEnvelope(Globals.SQUARE1);
	                UpdataSqrEnvelope(Globals.SQUARE2);
	                updateNoiseEnvelope();
	                updateLinearCounter();
	    	        if (frameCounterIrqInhFlag == 0) {
	    	        	frameIrqFlag = 0x40;
	    	        	mapper.setInterruptSignal(Globals.INTERRUPT_FRAME_IRQ);
	    	        }
	                frameCounterstepsCounter = 0;
	            }
	            else
	                frameCounterstepsCounter++;
	        	
	            break;
	            
	        case 4:
	            updateLengthCounters();
	            updateNoiseLengthCounter();
	            updateSquareSweep(Globals.SQUARE1);
	            updateSquareSweep(Globals.SQUARE2);
	            UpdataSqrEnvelope(Globals.SQUARE1);
	            UpdataSqrEnvelope(Globals.SQUARE2);
	            updateNoiseEnvelope();
	            updateLinearCounter();
	            frameCounterstepsCounter = 0;
	            break;
        }	
    }

    // mais metodos para atualizacao das ondas retangulares
void updateSquareSweep(int channelNum) {
    	
        if (--sqrSweepCounter[channelNum] < 0) {
            sqrSweepCounter[channelNum] = sqrSweepCounterReload[channelNum] + 1;
            
            if (sqrSweepEnableFlag[channelNum] && sqrTimerReload[channelNum] > 7 && sqrSweepShift[channelNum] > 0) {
         
            	int elOperando = sqrTimerReload[channelNum] >> sqrSweepShift[channelNum];
                
                if (sqrSweepNegFlag[channelNum])
                    elOperando = (~elOperando) & 0xFFF;
                
                if (channelNum == 1)
                    elOperando = (elOperando + 1) & 0xFFF;
                
                sqrTimerReload[channelNum] = (sqrTimerReload[channelNum] + elOperando) & 0xFFF;
            
            }
      
        }
        
        if (sqrSweepReloadFlag[channelNum]) {
        	
            sqrSweepCounter[channelNum] = sqrSweepCounterReload[channelNum] + 1;
            sqrSweepReloadFlag[channelNum] = false;
            
        }
        
    }


    
    void updateSquareWaves(int channelNum) {
    	
        if (channelTimer[channelNum] < 0) {// cuando el Timer pasa de 0 a t, ** PLUS ONE *** cicla la Wave y restaura balor de timer
        	
            sqrDutyOffset[channelNum] = (sqrDutyOffset[channelNum] + 1) & 0x7;
            channelTimer[channelNum] = sqrTimerReload[channelNum];// + 1;
            
        }
        
    }
  
    
    void updateSquareOutputs(int channelNum) {
    	
        if ((channelLengthCounter[channelNum] != 0) && (channelTimer[channelNum] > 7) )
            channelOutput[channelNum] = 
            (dutyWaves[sqrDutyActual[channelNum] + sqrDutyOffset[channelNum]] * sqrVolume[channelNum] & channelSwitch[channelNum]);
        else
            channelOutput[channelNum] = 0;
        
    }

    
    void UpdataSqrEnvelope(int channelNum) {
    	
        if (RestartSqrEnv[channelNum]) {
        	
            sqrEnvelopeCounter[channelNum] = (sqrRegister0[channelNum] & 0xF) + 1;
            sqrVolEnvelope[channelNum] = 0xF;
            RestartSqrEnv[channelNum] = false;
            
        }
        else {
        	
            sqrEnvelopeCounter[channelNum]--;
            
            if (sqrEnvelopeCounter[channelNum] <= 0) {
            	
                sqrEnvelopeCounter[channelNum] = (sqrRegister0[channelNum] & 0xF) + 1;
                
                if (sqrVolEnvelope[channelNum] > 0)
                    sqrVolEnvelope[channelNum] -= 1;
                else
                    sqrVolEnvelope[channelNum] = (short) (sqrLoopEnvelope[channelNum] == 0xFF ? 0xF : 0);
                
            }
            
        }
        
        if(constVolSqrFlag[channelNum])
        	sqrVolume[channelNum] = (short) (sqrRegister0[channelNum] & 0xF);
        else
        	sqrVolume[channelNum] = sqrVolEnvelope[channelNum];
        
    }

    // metodos para atualizar o canal de efeitos sonoros (Noise channel)
    void updateNoiseLengthCounter()  {
    	
        if ((noiseLengthCounterHalt & noiseLengthCounter) != 0)
            noiseLengthCounter--;
        
    }

    
    
    void updateNoiseEnvelope() {
    	
        if (noiseRestartEnv) {
        	
            noiseEnvelopeCounter = (noiseRegisters[Globals.NOISE_REGISTER_0] & 0xF) + 1;
            noiseVolumeEnvelope = 0xF;
            noiseRestartEnv = false;
            
        }
        else {
        	
            noiseEnvelopeCounter--;
            if (noiseEnvelopeCounter <= 0) {
                noiseEnvelopeCounter = (noiseRegisters[Globals.NOISE_REGISTER_0] & 0xF) + 1;
                
                if (noiseVolumeEnvelope > 0)
                    noiseVolumeEnvelope -= 1;
                else
                    noiseVolumeEnvelope = 0;
                
            }
            
        }
        
        if(noiseConstVolumeFlag == 1)
        	noiseVolume =  noiseRegisters[Globals.NOISE_REGISTER_0] & 0xF;
        else
        	noiseVolume = noiseVolumeEnvelope;
    }

    
    void updateNoiseWave() {
    	
        if (noiseTimer < 0) {// cuando el Timer pasa de 0 a t, ** PLUS ONE *** cicla la Wave y restaura balor de timer
        	
            noiseFeedbackBit = (noiseShiftRegister & 1) ^ ((noiseShiftRegister >> noiseModeRegister) & 1);
            
            if(noiseFeedbackBit == 1)
            	noiseShiftRegister = ((noiseShiftRegister >> 1) | 0x4000);
            else
            	noiseShiftRegister = ((noiseShiftRegister >> 1) & 0x3fff);
            noiseTimer = noiseFreq[noiseRegisters[Globals.NOISE_REGISTER_1] & 0xF];
        
        }
        
        if ((noiseLengthCounter != 0) && ((noiseShiftRegister & 1) != 0))
            noiseOutput = noiseVolume & channelSwitch[Globals.NOISE];
        else
            noiseOutput = 0;
        
    }

    // atualizacao do canal de PCM
    void updateDmcWave() {

    	if(channelSwitch[Globals.DMC] == 0)
    		return;
        dmcTimer = dmcTimerValues[dmcRegisters[Globals.DMC_REGISTER_0] & 0xF];
        if(dmcSilenceFlag == false) {
	        if ((dmcSampleBuffer & 1) == 1)
	        	dmcDac = (byte) ((dmcDac + 1) < 0x7e ? dmcDac + 2 : dmcDac);
	        else
	        	dmcDac = (byte) ((dmcDac - 1) > 1 ? dmcDac - 2 : dmcDac);
        }
        	dmcSampleBuffer >>= 1;
        
        if (--dmcShiftRemainBits <=0) {
        	
            dmcShiftRemainBits = 8;
            updateSampleBuffer();
       
        }
        if(dmcIrqFlag !=0) {
        	mapper.setInterruptSignal(Globals.INTERRUPT_DMC_IRQ);
        	mapper.setIrqDelayFlag(false);

        }
    }

    void updateSampleBuffer() {
        if (dmcRemainSamples > 0) {
            dmcSampleBuffer = mapper.ioRead(dmcSampleAddr++);
            updateDmcCycles = true;
            dmcSilenceFlag = false;
            dmcRemainSamples--;
        }
        checkDmcAddrOverflow();
        if (dmcRemainSamples == 0) {
        	dmcSilenceFlag = true;
            if (dmcLoopFlag == 0x40) {
                dmcSampleAddr = 0xC000 + (dmcRegisters[Globals.DMC_REGISTER_2] * 64);
                dmcRemainSamples = (dmcRegisters[Globals.DMC_REGISTER_3] * 16) + 1;
            }
            else {
                if (dmcInterruptEnable == 0x80)
                	dmcIrqFlag = 1;
            }
        }
    }
    
   public void checkDmcAddrOverflow() { 	
	    if (dmcSampleAddr > 0xFFFF)
	        dmcSampleAddr = 0x8000;           
    }   
    
 

   public void setSqrTimerReload(int canalNum, int value) {

	  sqrTimerReload[canalNum] = value;

  }

  
  public int getsqrTimerReload(int canalNum) {
	   	
	   return sqrTimerReload[canalNum];
  	
  }

    
  public void setDmcIrqFlag(int value) {
	  dmcIrqFlag = value;
	  
  }

  // a seguir metodos chamados perante a escrita ou leitura dos registradores que controlam a APU
  public void sqrRegister0Write(int canalNum, int value) {
	  sqrRegister0[canalNum] = value;
	  if((value & 0x20) == 0x20) // en APU register bit 0 = FLAG LIGADO, 1 = DESLIGADO
			 lengthCounterHaltFlag[canalNum] = 0;
		 else
			 lengthCounterHaltFlag[canalNum] = 0xff;

	  sqrLoopEnvelope[canalNum] = (~lengthCounterHaltFlag[canalNum]) & 0xFF;
	  constVolSqrFlag[canalNum] = (value & 0x10) == 0x10;
	  sqrDutyActual[canalNum] = ((value & 0xC0) >> 6) * 8;

	  
      if(constVolSqrFlag[canalNum])
    	  sqrVolume[canalNum] = (short)(sqrRegister0[canalNum] & 0xF);
      else
    	  sqrVolume[canalNum] = (short)(sqrVolEnvelope[canalNum]);
	  
  }

  public void sqrRegister1Write(int canalNum, int value) {
	  sqrSweepReloadFlag[canalNum] = true;
	  sqrSweepEnableFlag[canalNum] = (value & 0x80) >> 7 == 1;
	  sqrSweepNegFlag[canalNum] = (value & 0x8) >> 3 == 1;
	  sqrSweepShift[canalNum] = value & 7;
	  sqrSweepCounterReload[canalNum] = (value & 0x70) >> 4;
      updateSquareSweep(canalNum);
  
  }

  public void sqrRegister2Write(int canalNum, int value) {
	  setSqrTimerReload(canalNum, (getsqrTimerReload(canalNum) & 0x700) | value );	    
  }
  
  public void sqrRegister3Write(int canalNum, int value) {
	  setSqrTimerReload(canalNum, (sqrTimerReload[canalNum] & 0xff) | ((value & 7) << 8));
	  channelLengthCounter[canalNum] = lengthCounterValues[value >> 3];
	  sqrDutyOffset[canalNum] = 0;
	  RestartSqrEnv[canalNum] = true;
  }
  
  
  public void trgRegister0Write(int value) {	
	  trgRegisters[Globals.TRIANGLE_REGISTER_0] = value;
	  if((value & 0x80) == 0x80) {// en APU register bit 0 = FLAG LIGADO, 1 = DESLIGADO
		  lengthCounterHaltFlag[Globals.TRIANGLE] = 0;
		  linearCounterControlFlag = 1;
	  }
	  else {
		  lengthCounterHaltFlag[Globals.TRIANGLE] = 0xff;
		  linearCounterControlFlag = 0;
	  }
  
  }
  

  public void trgRegister1Write(int value) {
	  
	  trgRegisters[Globals.TRIANGLE_REGISTER_1] = value;
	  channelTimer[Globals.TRIANGLE] = (channelTimer[Globals.TRIANGLE] & 0x700) | value;

  }  
  

  public void trgRegister2Write(int value) {

	  trgRegisters[Globals.TRIANGLE_REGISTER_2] = value;
	  channelTimer[Globals.TRIANGLE] = ((channelTimer[Globals.TRIANGLE] & 0xff) | ((value & 7) << 8)) + 1;
	  channelLengthCounter[Globals.TRIANGLE] = lengthCounterValues[value >> 3];
	  linearCounterHaltFlag = 1;
  }

  
  public void noiseRegister0Write(int value) {
	  noiseRegisters[Globals.NOISE_REGISTER_0] = value;
	  if((value & 0x20) == 0x20)
		  noiseLengthCounterHalt = 0;
	  else
		  noiseLengthCounterHalt = 0xff;

	  if((value & 0x10) == 0x10) {
		  noiseConstVolumeFlag = 1;
		  noiseVolume = value & 0xF; 
	  }
	  else {
		  noiseConstVolumeFlag = 0;
		  noiseVolume = noiseVolumeEnvelope;
	  }
  
  }

  
  public void noiseRegister1Write(int value) {

	  noiseRegisters[Globals.NOISE_REGISTER_1] = value;
	  if((value & 0x80) == 0x80)
		  noiseModeRegister = 6;
	  else
		  noiseModeRegister = 1;
		  
	  noiseTimer = noiseFreq[value & 0xF];
	  
  }

  public void noiseRegister2Write(int value) {

	  noiseLengthCounter = lengthCounterValues[value >> 3];
	  noiseRestartEnv = true;

  }
  

  public void dmcRegister0Write(int value) {
	  dmcRegisters[Globals.DMC_REGISTER_0] = value;
	  dmcTimer = dmcTimerValues[value & 0xF];
	  if((value & 0x80) == 0x80) {
		  dmcInterruptEnable = 0x80;
	  }
	  else {
		  dmcInterruptEnable = 0;
		  dmcIrqFlag = 0;
	  }
	  if((value & 0x40) == 0x40)
		  dmcLoopFlag = 0x40;
	  else
		  dmcLoopFlag = 0;
	  
  } 

  
  public void dmcRegister1Write(int value) {

	  dmcDac = (byte) (value & 0x7f);
  }
  

  public void dmcRegister2Write(int value) {

	  dmcRegisters[Globals.DMC_REGISTER_2] = value;      
	  dmcSampleAddr = reloadDmcSampleAddr =0xC000 + (value * 64);

  }
  

  public void dmcRegister3Write(int value) {

	  dmcRegisters[Globals.DMC_REGISTER_3] = value;
	  dmcRemainSamples = (value * 16) + 1;

  }
  

  public void reg4017Write(int value) {
	  
	  if((value & 0x80) == 0x80)
		  frameCountersteps = 5;
	  else
		  frameCountersteps = 4;

	  if (frameCountersteps == 4) {
		  frameCounterstepsCounter = 0;
		  frameCounter = 0;
	  }

	  if((value & 0x40) == 0x40) {
		  frameCounterIrqInhFlag = 4;
		  frameIrqFlag = 0;
		  mapper.clearInterruptSignal(Globals.INTERRUPT_FRAME_IRQ);

	  }
	  else
		  frameCounterIrqInhFlag = 0;
  }
  
	public int reg4015Read() {
		int temp = 0;
		for (int n = 0; n < 3; n++) {
		    temp |= channelLengthCounter[n] > 0 ? 1 << n : 0;
		}
		temp |= noiseLengthCounter > 0 ? 8 : 0;
		temp |= frameIrqFlag;
		frameIrqFlag = 0;
		mapper.clearInterruptSignal(Globals.INTERRUPT_FRAME_IRQ);

		if(dmcRemainSamples > 0)
		temp |= 0x10;
		if(dmcIrqFlag !=0)
		temp |= 0x80;
		return temp;	  
	}
}