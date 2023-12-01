/**
 * Jamicom.java
 * Carrega interface gr�fica para o usuario operar o emulador
 * aguarda o usu�rio selecionar um arquivo de ROM para inicializar os subsistemas
 */

package com.jamicom;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JFileChooser;

@SuppressWarnings("serial")
public class Jamicom extends JFrame{
	
	String titleWindow = "Jamicom v 1.00"; 
	JMenuBar menuBar;
	GamePad gamepad;
	Canvas canvas;
	Cartridge cartridge;
	Ppu ppu;
	Cpu cpu;
	Apu apu;
	Mapper mapper;
	Console console;
	
	// criacao da interface grafica
	public Jamicom() throws IOException { 
		
		// cria barra de menu
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		// itens da barra do menu
		JMenu menuFile = new JMenu("Arquivo");
		menuFile.setMnemonic(KeyEvent.VK_F); // tecla de atalho
		menuBar.add(menuFile); // adiciona barra
		
		JMenu menuConfig = new JMenu("Configurar");
		menuConfig.setMnemonic(KeyEvent.VK_C);
		menuBar.add(menuConfig);

		JMenu menuAjuda = new JMenu("Ajuda");
		menuConfig.setMnemonic(KeyEvent.VK_A);
		menuBar.add(menuAjuda);

		// janela de menu File
		JMenuItem fileItem01 = new JMenuItem("Abrir...", KeyEvent.VK_A);
		menuFile.add(fileItem01);
		menuFile.addSeparator(); // insere linha separatoria
		JMenuItem fileItem02 = new JMenuItem("Sair", KeyEvent.VK_S);
		menuFile.add(fileItem02);

		// janela de menu Config
		JMenu configItem01 = new JMenu("Escala");
		menuConfig.add(configItem01);

		/**
		 * opccao Config -> Escala -> 1x, 2x, 3x 
		 * por enquanto somente opcao de redimensionamento, no futuro opcoes para ajustar performance, sons, botoes...
		*/ 
		ButtonGroup escalaGroup = new ButtonGroup();
		JMenuItem escalaItem01 = new JRadioButtonMenuItem("1 X");
		configItem01.add(escalaItem01);
		escalaGroup.add(escalaItem01);
		JMenuItem escalaItem02 = new JRadioButtonMenuItem("2 X");
		configItem01.add(escalaItem02);
		escalaGroup.add(escalaItem02);
		JMenuItem escalaItem03 = new JRadioButtonMenuItem("3 X");
		configItem01.add(escalaItem03);
		escalaGroup.add(escalaItem03);
		escalaItem01.setSelected(true); // botao 1 ativado
		
		// adicao de listeners
		fileItem01.addActionListener(new ActionListener() { // File -> Abrir
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Arquivos de ROM (.nes, .zip)", "nes", "zip"); 
				chooser.addChoosableFileFilter(filter);
				chooser.setFileFilter(filter);
				chooser.setCurrentDirectory(new File(System.getProperty("user.home"))); // configura path inicial
				
				// se user selecionou arquivo (sinalizado pelo valor de retorno de chooser()
				if(chooser.showOpenDialog(Jamicom.this) == JFileChooser.APPROVE_OPTION) { 
					File rom = chooser.getSelectedFile();
					titleWindow = chooser.getName();
					
					try {
						String fullPath = rom.getCanonicalPath().toLowerCase();
						if((fullPath.endsWith(".zip")) || (fullPath.endsWith(".nes"))) {
							
							// cria objeto tipo cartridge a partir do arquivo de ROM
							cartridge = new Cartridge(rom);
							Globals.EMULA = false;
							setupSystem(); // se arquivo valido, prossegue com inicializacao do sistema					
						}
						else
			    			JOptionPane.showMessageDialog(null, "Formato invalido", "Erro carregando ROM", JOptionPane.ERROR_MESSAGE);

					} catch (IOException e1) {
						// TODO Auto-generated catch block
					}
					
				}
			}}
		);

		// Arquivo -> Sair
		fileItem02.addActionListener(new ActionListener() { 
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}}
		);

		// Config -> Escala -> 1x
		escalaItem01.addActionListener(new ActionListener() { 
			@Override
			public void actionPerformed(ActionEvent e) {
				Globals.SCR_SCALE = 1;
				updateWindow();
			}}
		);

		// Config -> Escala -> 2x
		escalaItem02.addActionListener(new ActionListener() { 
			@Override
			public void actionPerformed(ActionEvent e) {
				Globals.SCR_SCALE = 2;
				updateWindow();
			}}
		);
		
		// Config -> Escala -> 3x
		escalaItem03.addActionListener(new ActionListener() { 
			@Override
			public void actionPerformed(ActionEvent e) {
				Globals.SCR_SCALE = 3;
				updateWindow();
			}}
		);
		
		canvas = new Canvas();
		gamepad = new GamePad();
		cpu = new Cpu();
		ppu = new Ppu();
		apu = new Apu();
		console = new Console();
		getContentPane().add(gamepad);
		getContentPane().add(canvas);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle(titleWindow);
		setResizable(false);
		updateWindow();
	}
	
	public void start() throws InterruptedException {					
		console.startEmulation();
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		Jamicom jamicom = new Jamicom();
		while(true) {
			while(Globals.EMULA == false) // aguarda at� usuario selecionar ROM
				Thread.sleep(1000);
			jamicom.start();
		}
	}
	
	// funcao para redimensionar tela do jogo
	public void updateWindow() {
		int menuHeight = menuBar.getHeight();
		if(menuHeight == 0)
			menuHeight = 20;
		int insets = getInsets().top; // dimensoes extras alem do canvas (bordas, titulo)
		int extraY = menuHeight + insets;
		setPreferredSize(new Dimension(Globals.SCR_X * Globals.SCR_SCALE, Globals.SCR_Y * Globals.SCR_SCALE + extraY ));
		pack();
		setLocationRelativeTo(null);
		setVisible(true);		
	}

	// comando para atualizar o canvas (tela), chamado 60 vezes por segundo
	void drawFrame() throws InterruptedException {
		canvas.resetDrawBuffer();
		repaint();
	}
	
	Cartridge getCartridge() {
		return cartridge;
	}

	GamePad getGamePad() {
		return gamepad;
	}

	public Mapper getMapper() {
		return this.mapper;
	}

	public Ppu getPpu() {
		return this.ppu;
	}

	public Apu getApu() {
		return this.apu;
	}

	public Cpu getCpu() {
		return this.cpu;
	}

	public Canvas getCanvas() {
		return this.canvas;
	}

	public Console getConsole() {
		return this.console;
	}

	void setupSystem() throws IOException {
		/**
		 * MAPPERS = sistemas de bank switching da placa dos cartuchos que permitia ao NES executar jogos com mais de 65 kilobytes.
		 * Alguns mappers tamb�m s�o capazes de gerar interrup��es de hardware e outros melhoram o som.
		 * Cada softhouse produzia seu pr�prio mapper. At� o momento, mais de 100 mappers foram documentados.
		 * Aqui implementei apenas dois dos mais populares. Quanto mais mappers emulados, mais jogos s�o suportados pelo emulador.
		 * At� o momento > 600 jogos testados e funcionantes.
		 */
    	switch(cartridge.getMapperNum()) { 
			case   0: mapper = new Mapper(); break; 
		//	case   1: mapper = new Mapper001(this); break;
			case   2: mapper = new Mapper002(); break;/*
    		case   3: mapper = new Mapper003(this); break;
    		case   4: mapper = new Mapper004(this); break;
    		case   5: mapper = new Mapper005(this); break;

    		case   7: mapper = new Mapper007(this); break;

    		case   9: mapper = new Mapper009(this); break;
       		case  10: mapper = new Mapper010(this); break;
    		case   11: mapper = new Mapper011(this); break;

    		case  16: mapper = new Mapper016(this); break;

    		case  18: mapper = new Mapper018(this); break;
    		case  19: mapper = new Mapper019(this); break;

    		case  21: mapper = new Mapper021(this); break;
    		case  22: mapper = new Mapper022(this); break;
       		case  23: mapper = new Mapper023(this); break;

       		case  25: mapper = new Mapper025(this); break;
      		case  26: mapper = new Mapper026(this); break;
      		 
    		case  32: mapper = new Mapper032(this); break;
    		case  33: mapper = new Mapper033(this); break;
    		case  34: mapper = new Mapper034(this); break;

    		case  64: mapper = new Mapper064(this); break;

    		case  66: mapper = new Mapper066(this); break;
    		case  67: mapper = new Mapper067(this); break;
    		case  68: mapper = new Mapper068(this); break;
    		case  69: mapper = new Mapper069(this); break;
    		case  70: mapper = new Mapper070(this); break;
    		case  71: mapper = new Mapper071(this); break;
    		case  72: mapper = new Mapper072(this); break;

    		case  75: mapper = new Mapper075(this); break;

    		case  78: mapper = new Mapper078(this); break;
    		case  79: mapper = new Mapper079(this); break;

    		case  80: mapper = new Mapper080(this); break;


    		case  87: mapper = new Mapper087(this); break; 
    		case  88: mapper = new Mapper088(this); break; 
    		case  89: mapper = new Mapper089(this); break; 

    		case  91: mapper = new Mapper091(this); break; 

    		case  94: mapper = new Mapper094(this); break;
    		case  95: mapper = new Mapper095(this); break;

    		case  97: mapper = new Mapper097(this); break;

    		case 118: mapper = new Mapper118(this); break;
    		case 119: mapper = new Mapper119(this); break;

    		case 140: mapper = new Mapper140(this); break;

    		case 152: mapper = new Mapper152(this); break;

    		case 154: mapper = new Mapper154(this); break;

    		case 184: mapper = new Mapper184(this); break;

    		case 189: mapper = new Mapper189(this); break;

    		case 206: mapper = new Mapper206(this); break;

    		case 210: mapper = new Mapper210(this); break;

    		case  65: mapper = new Mapper065(this); break;	*/
    		/*

    		case  72: mapper = new Mapper072(this); break;
    		case  93: mapper = new Mapper093(this); break; 

    		case 180: mapper = new Mapper180(this); break;
*/
    		default : {
    			String errorMsg = "Erro ! Mapper " + cartridge.getMapperNum() + " nao suportado !";
    			JOptionPane.showMessageDialog(this, errorMsg, "Jogo n�o suportado", JOptionPane.ERROR_MESSAGE);
    			mapper = new Mapper();    
    		}
    	}
		console.setup(this);
		canvas.setup(ppu);
		cpu.setup(mapper);
		ppu.setup(this);
		apu.setup(mapper);
		mapper.setup(this);
		cpu.reset(mapper.getRstAddr());
		Globals.EMULA = true; // se tudo bem sucedido, ativa o loop principal
	}
}
