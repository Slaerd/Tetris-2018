import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class JBrainTetris extends JTetris{
	
	private static final long serialVersionUID = 1L;
	private Brain brain;			//Type de brain qu'utilisera l'IA
	private boolean brainMode;		//Vaut True si le brain est actif, false sinon
	private boolean paused;			//Un attribut ajoute pour mettre le jeu en pause, et faciliter les tests
	private Brain.Move bestMove;	//Ceci contient les coups de l'IA si elle est active (et qu'elle en trouve)
	private int brainMoveNb;		//Compte les mouvements de l'IA pour une meme piece
									//Ceci est utile pour un cas que l'on souhaite eviter lors de l'appel de tick

	protected JSlider luck;
	protected JCheckBox Adversaire;
	protected boolean AdversaireMode = false;
	
	
	
	/**
	 * Cree un tableau de jeu selon les params
	 * @param pixels Taille du tableau de jeu
	 * @param brain Type d'IA qui va eventuellement jouer au Tetris
	 */
	public JBrainTetris(int pixels,Brain brain){
		super(pixels);
		this.brain = brain;
		this.brainMode = false;
		this.paused = false;
		this.bestMove = null;
		this.brainMoveNb = 0;
	}
	
	/**
	 * Methode surchargee de tick qui ici prend en compte l'eventuelle activation de notre brain
	 */
	public void tick(int verb) {
		if (!this.gameOn || this.paused) {
			return;
		}

		if (currentPiece != null) {
			this.board.undo(); // remove the piece from its old position
		}
		
		// Sets the newXXX ivars
		if(verb == DOWN && this.brainMode && bestMove == null) { //On cherche le meilleur coup
			brainMoveNb = 0; //On reinitialise le nombre de mouvements
			bestMove = this.brain.bestMove(this.board, this.currentPiece, this.board.getHeight() - 4);
		}
		
		if(verb == DOWN && bestMove != null && brainMoveNb > 0) {	//La condition brainMoveNb > 0 permet d'eviter la suppression d'une piece
																	//due a une rotation initiale qui sortirait du tableau
			if(!currentPiece.equals(bestMove.piece)) {
				System.out.println(currentPiece.toString());
				System.out.println(bestMove.piece.toString());
				System.out.println("Should be rotated");
				this.computeNewPosition(ROTATE);
			}else if(currentX < bestMove.x) {
				System.out.println("Should be going right");
				this.computeNewPosition(RIGHT);
			}else if(currentX > bestMove.x) {
				System.out.println("Should be going left");
				this.computeNewPosition(LEFT);
			}else 
				this.computeNewPosition(verb);	//On est obligé d'appeler separement computeNewPosition(verb) pour ne pas effectuer l'operation 2 fois
		}else									//et ne pas forcer un tick vers le bas lors du deplacement de notre brain qui pourrait etre fatal
			this.computeNewPosition(verb);
		
		brainMoveNb++;							//Arrive ici, on a fait un mouvement, on incremente donc brainMoveNb
		
		// try out the new position (rolls back if it doesn't work)
		int result = setCurrent(newPiece, newX, newY);

		// if row clearing is going to happen, draw the
		// whole board so the green row shows up
		if (result == Board.PLACE_ROW_FILLED) {
			this.repaint();
		}

		boolean failed = (result >= Board.PLACE_OUT_BOUNDS);
		//System.out.println(failed);

		// if it didn't work, put it back the way it was
		if (failed) {
			if (currentPiece != null) {
				board.place(currentPiece, currentX, currentY);
			}
			repaintPiece(currentPiece, currentX, currentY);
		}

		/*
		 * How to detect when a piece has landed: if this move hits something on
		 * its DOWN verb, and the previous verb was also DOWN (i.e. the player
		 * was not still moving it), then the previous position must be the
		 * correct "landed" position, so we're done with the falling of this
		 * piece.
		 */
		if ((failed && verb == DOWN && !moved)) { // it's landed
			int cleared = board.clearRows();
			if (cleared > 0) {
				// score goes up by 5, 10, 20, 40 for row clearing
				// clearing 4 gets you a beep!
				switch (cleared) {
				case 1:
					score += 5;
					break;
				case 2:
					score += 10;
					break;
				case 3:
					score += 20;
					break;
				case 4:
					score += 40;
					Toolkit.getDefaultToolkit().beep();
					break;
				default:
					score += 50; // could happen with non-standard pieces
				}
				updateCounters();
				repaint(); // repaint to show the result of the row clearing
			}

			// if the board is too tall, we've lost
			if (this.board.getMaxHeight() > this.board.getHeight() - TOP_SPACE) {
				this.stopGame();
			} else {
				// Otherwise add a new piece and keep playing
				System.out.println("###########################################################");
				System.out.print("######### NEW PIECE ");
				
				this.bestMove = null;	//Une fois la piece posee on reinitialise notre bestMove
				this.addNewPiece();
				System.out.println(currentPiece.toString() +  " ###########");
				System.out.println("###########################################################");
				
			}
		}

		// Note if the player made a successful non-DOWN move --
		// used to detect if the piece has landed on the next tick()
		moved = (!failed && verb != DOWN);
	}
	
	@Override
	public Piece pickNextPiece() {
		int pieceNum;
		AdversaireMode = Adversaire.isSelected();
		if (AdversaireMode) {
			int randChoose = random.nextInt(101);
			
			//Adversaire ne choisit pas
			if ( randChoose < luck.getValue()) {
				pieceNum = (int) (pieces.length * random.nextDouble());
			} else {
				//Choit de l adversaire
				int worstPiece = 0;
				double worstScore = 0;
				Brain PieceI = new DefaultBrain();
				//On cherche la pire piece 
				for (int i = 0; i < pieces.length; i++) {
					Brain.Move TP = PieceI.bestMove(this.board, pieces[i], this.board.getHeight() - 4);
					if (TP.score > worstScore) {
						worstPiece = i;
						worstScore = TP.score;
					}
				}
				pieceNum = worstPiece;
			}	
		} else {
			pieceNum = (int) (pieces.length * random.nextDouble());	
		}

		Piece piece = pieces[pieceNum];

		return (piece);
	}
	
	
	/**
	 * Active ou desactive le jeu automatique de l'IA selon un booleen
	 */
	public void toggleBrain(boolean b) {
		this.brainMode = b;
	}
	
	/**
	 * Methode pour mettre le jeu en pause ou reprendre
	 */
	public void pauseChange() {
		this.paused = !this.paused;
	}
											//On a besoin de 2 methodes differentes : 
											//unpause pour cliquer sur start sans avoir a depauser
											//pauseChange pour changer l'etat de pause
	/**
	 * Methode pour reprendre le jeu
	 */
	public void unpause() {
		this.paused = false;
	}
	/**
	 * Methode surchargee de startGame pour depauser automatiquement le jeu
	 * ainsi que reinitialiser le bestMove eventuellement restant de la partie precedente
	 */
	public void startGame() {
		super.startGame();
		unpause();
		this.bestMove = null;
	}
	
	/**
	 * Methode surchargee de createControlPanel ajoutant un bouton Brain pour laisser l'IA jouer
	 * On dispose aussi d'un bouton pause pour controler les tests
	 * On ne fait pas d'appel super car on modifie la methodes startGame
	 */
	public JComponent createControlPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		// COUNT
		countLabel = new JLabel("0");
		panel.add(countLabel);

		// SCORE
		scoreLabel = new JLabel("0");
		panel.add(scoreLabel);

		// TIME
		timeLabel = new JLabel(" ");
		panel.add(timeLabel);

		panel.add(Box.createVerticalStrut(12));

		// START button
		startButton = new JButton("Start");
		panel.add(startButton);
		startButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				startGame();
			}
		});
		
		JButton pauseButton = new JButton("Pause");
		panel.add(pauseButton);
		pauseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pauseChange();
			}
		});
		
		// STOP button
		stopButton = new JButton("Stop");
		panel.add(stopButton);
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stopGame();
			}
		});

		enableButtons();

		JPanel row = new JPanel();

		// SPEED slider
		panel.add(Box.createVerticalStrut(6));
		row.add(new JLabel("Speed:"));
		speed = new JSlider(0, 200, 75); // min, max, current
		speed.setPreferredSize(new Dimension(100, 15));

		updateTimer();
		row.add(speed);

		panel.add(row);
		speed.addChangeListener(new ChangeListener() {
			// when the slider changes, sync the timer to its value
			public void stateChanged(ChangeEvent e) {
				updateTimer();
			}
		});
		

		Adversaire = new JCheckBox("Adversaire active");
		panel.add(Adversaire);

		JPanel rowAdversaire = new JPanel();
		
		//SLider luck
		panel.add(Box.createVerticalStrut(6));
		rowAdversaire.add(new JLabel("Luck:"));
		luck = new JSlider(0, 100, 75); // min, max, current
		luck.setPreferredSize(new Dimension(100, 15));
		rowAdversaire.add(luck);
		
		panel.add(rowAdversaire);
		
		
		
		testButton = new JCheckBox("Test sequence");
		panel.add(testButton);

		panel.add(new JLabel("Brain:"));
		JCheckBox brainMode = new JCheckBox("Brain active");
		panel.add(brainMode);
		
		brainMode.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					toggleBrain(brainMode.isSelected());
			}
			
		}
		);
		
		return panel;
	}
	
	public static void main(String[] a) {
		Brain dBrain = new DefaultBrain();
		JBrainTetris tetris = new JBrainTetris(16,dBrain);
		JFrame frame = JBrainTetris.createFrame(tetris);
		frame.setVisible(true);
	}
}
