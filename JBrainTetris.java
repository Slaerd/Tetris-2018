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
	private Brain brain;
	private boolean brainMode;
	private boolean paused;
	
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
	}
	
	public void tick(int verb) {
		System.out.println("pause = " + this.paused);
		if (!this.gameOn || this.paused) {
			//System.out.println("salut");
			return;
		}

		if (currentPiece != null) {
			this.board.undo(); // remove the piece from its old position
		}

		// Sets the newXXX ivars
		if(verb == DOWN && this.brainMode) {
			Brain.Move bestMove = this.brain.bestMove(this.board, this.currentPiece, this.board.getHeight() - 4);
			if(bestMove != null) {
				System.out.println(this.currentPiece.toString());
				System.out.println("my name is jacksepticeye");
				newPiece = bestMove.piece;
				newX = bestMove.x;
				newY = bestMove.y;
			}else
				this.computeNewPosition(verb);
		}else
			this.computeNewPosition(verb);

		
		// try out the new position (rolls back if it doesn't work)
		int result = setCurrent(newPiece, newX, newY);

		// if row clearing is going to happen, draw the
		// whole board so the green row shows up
		if (result == Board.PLACE_ROW_FILLED) {
			this.repaint();
		}

		boolean failed = (result >= Board.PLACE_OUT_BOUNDS);
		System.out.println(failed);

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
		if ((failed && verb == DOWN && !moved) || this.brainMode) { // it's landed

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
				System.out.println("new piece owo");
				this.addNewPiece();
			}
		}

		// Note if the player made a successful non-DOWN move --
		// used to detect if the piece has landed on the next tick()
		moved = (!failed && verb != DOWN);
	}
	
	/**
	 * Active ou desactive le jeu automatique de l'IA selon b
	 */
	public void toggleBrain(boolean b) {
		this.brainMode = b;
	}
	
	public void pauseChange() {
		this.paused = !this.paused;
	}
	
	public void unpause() {
		this.paused = false;
	}
	
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
				unpause();
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
		panel.add(Box.createVerticalStrut(12));
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
