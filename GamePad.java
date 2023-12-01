/**
 * Gamepad.java
 * Define objeto do tipo Gamepad (controle)
 * estende JPanel e adiciona um Keylistener na janela do jogo
 * entrada pelas portas $4016 e $4017 por bits sinalizando  o status de cada botao (1 = pressionado, 0 = liberado)
 */


package com.jamicom;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.Serializable;
import javax.swing.JPanel;

@SuppressWarnings("serial")
class GamePad extends JPanel implements Serializable {

    private int p1Strobe;   
    private int p2Strobe;
    private int readCounter; 		
    private int buttonFlags;    
    
    private static final int BUTTON_UP = 0x10;
    private static final int BUTTON_DOWN = 0x20;
    private static final int BUTTON_LEFT = 0x40;
    private static final int BUTTON_RIGHT = 0x80;
    private static final int BUTTON_A = 1;
    private static final int BUTTON_B = 2;
    private static final int BUTTON_SELECT = 4;
    private static final int BUTTON_START = 8;

	KeyListener keyListener;
		
	public GamePad() {
		
		keyListener = new KeyListener() {
			
			// chamado quando se pressiona uma tecla e causa ativacao do respectivo flag
			public void keyPressed(KeyEvent keyEvent) {
				int key = keyEvent.getKeyCode(); 
				
				// bloqueia acionamento simultaneo de 2 botoes de direcao (pode paralisar o sistema)
				if(key == Globals.P1_UP) {
					turnOffButton(BUTTON_DOWN);   
					turnOnButton(BUTTON_UP);					
				}
				else if(key == Globals.P1_DOWN) {
					turnOffButton(BUTTON_UP);
					turnOnButton(BUTTON_DOWN);
				}					
				else if(key == Globals.P1_LEFT) {
					turnOffButton(BUTTON_RIGHT);   
					turnOnButton(BUTTON_LEFT);
				}	
				else if(key == Globals.P1_RIGHT) {
					turnOffButton(BUTTON_LEFT);   // Pressing Left & Right simultaneously may cause BurgerTime to crash 
					turnOnButton(BUTTON_RIGHT);
				}					
				else if(key == Globals.P1_B)
					turnOnButton(BUTTON_B);
				else if(key == Globals.P1_A)
					turnOnButton(BUTTON_A);
				else if(key == Globals.P1_SEL)
					turnOnButton(BUTTON_SELECT);
				else if(key == Globals.P1_START)
					turnOnButton(BUTTON_START);						
			}

			// chamado quando se solta uma tecla pressionada, desativa o respectivo flag
			public void keyReleased(KeyEvent keyEvent) {	
				
				int key = keyEvent.getKeyCode(); 
				
				if(key == Globals.P1_UP)
					turnOffButton(BUTTON_UP);   				
				else if(key == Globals.P1_DOWN)
					turnOffButton(BUTTON_DOWN);   
				else if(key == Globals.P1_LEFT)
					turnOffButton(BUTTON_LEFT);   
				else if(key == Globals.P1_RIGHT)
					turnOffButton(BUTTON_RIGHT); 
				else if(key == Globals.P1_B)
					turnOffButton(BUTTON_B);			
				else if(key == Globals.P1_A)
					turnOffButton(BUTTON_A);			
				else if(key == Globals.P1_SEL)
					turnOffButton(BUTTON_SELECT);			
				else if(key == Globals.P1_START)
					turnOffButton(BUTTON_START);						
			}

			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
			}

		};
		
		addKeyListener(keyListener);
		setFocusable(true);
	}
	
	
    void turnOnButton(int button) {
        buttonFlags |= button;
    }

    
    void turnOffButton(int button) {
    	buttonFlags = (buttonFlags & (~(button))) & 0xff;
    }    

    // le o status de cada botao sequencialmente e retorna 1 ou 0
    public int reg4016Read() {	
        int temp =  ((buttonFlags >> readCounter) & 1) | 0x40;
        readCounter++;
        if (readCounter == 24)
        	readCounter = 0;
        return temp;
    }
    
    // ativa porta para leitura quando se escreve 1 seguido de 0
    public void reg4016Write(int value) {
    	if (((value & 1) == 0) && ( p1Strobe == 1))   
    		readCounter = 0;
        p1Strobe = value & 1;
    }

    // ativa porta para leitura quando se escreve 1 seguido de 0
    public void reg4017Write(int value) {
    	if (((value & 1) == 0) && ( p2Strobe == 1))
    		readCounter = 0;
        p2Strobe = value & 1;
    }

}