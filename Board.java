import java.util.Arrays;
import java.util.List;

/**
 * Represents a Tetris board -- essentially a 2-d grid of booleans. Supports
 * tetris pieces and row clearing. Has an "undo" feature that allows clients to
 * add and remove pieces efficiently. Does not do any drawing or have any idea
 * of pixels. Instead, just represents the abstract 2-d board.
 */
public class Board {

	private int width;
	private int height;

	protected boolean[][] grid;
	private boolean committed;
	
	private boolean[][] backupGrid;
	private int backupWidths;
	private int backupHeights;	
	
	/**
	 * Creates an empty board of the given width and height measured in blocks.
	 */
	public Board(int width, int height) {
		this.width = width;
		this.height = height;

		this.grid = new boolean[width][height];
		this.committed = true;
		
		for (boolean[] i : this.grid){
			for (boolean j : i){
				j = false;
			}
		}
	}
	
	//Copy a board
	public Board(Board Cope){
		this.width = Cope.width;
		this.height = Cope.height;
		this.grid = Cope.grid;
		this.committed = Cope.committed;
	}
	
	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	/**
	 * Returns the max column height present in the board. For an empty board
	 * this is 0.
	 */
	public int getMaxHeight() {
		int MaxHeight = 0;
		for (boolean[] i : this.grid){
			int cpt = 0;
			for (boolean j : i){
				if (j){
					MaxHeight = cpt;
				}
				cpt++;
			}
		}
	    return MaxHeight; 
	}

	/**
	 * Given a piece and an x, returns the y value where the piece would come to
	 * rest if it were dropped straight down at that x.
	 * 
	 * <p>
	 * Implementation: use the skirt and the col heights to compute this fast --
	 * O(skirt length).
	 */
	public int dropHeight(Piece piece, int x) {
	    int maxHeight = 0;
	    int cpt = 0;
	    for (int i : piece.getSkirt()){
	    	int ColumnHeight = this.getColumnHeight(cpt+x);
	    	if (ColumnHeight + i > maxHeight){
	    		maxHeight = ColumnHeight + i;
	    	}
	    	cpt++;
	    }
	    return maxHeight;
	}

	/**
	 * Returns the height of the given column -- i.e. the y value of the highest
	 * block + 1. The height is 0 if the column contains no blocks.
	 */
	public int getColumnHeight(int x) {
		int Height = 0; //Quisas no need for just while then when true stop
		int cpt = 0;
	    for (boolean i : this.grid[x]){
	    	if (i){
	    		Height = cpt;
	    	}
	    	cpt++;
	    }
	    return Height;
	}

	/**
	 * Returns the number of filled blocks in the given row.
	 */
	public int getRowWidth(int y) {
	    boolean[][] grille = this.grid;
	    int cpt = 0;
	    int pointeur = 0;
	    while (pointeur < this.width){
	    	if (grille[pointeur][y]){
	    		cpt++;
	    	}
	    	pointeur++;
	    }
	    return cpt;
	}

	/**
	 * Returns true if the given block is filled in the board. Blocks outside of
	 * the valid width/height area always return true.
	 */
	public boolean getGrid(int x, int y) {
		if (x < 0 | x > this.width | y < 0 | y > this.height){
			return true;
		}
		return this.grid[x][y];
	}

	public static final int PLACE_OK = 0;
	public static final int PLACE_ROW_FILLED = 1;
	public static final int PLACE_OUT_BOUNDS = 2;
	public static final int PLACE_BAD = 3;

	/**
	 * Attempts to add the body of a piece to the board. Copies the piece blocks
	 * into the board grid. Returns PLACE_OK for a regular placement, or
	 * PLACE_ROW_FILLED for a regular placement that causes at least one row to
	 * be filled.
	 * 
	 * <p>
	 * Error cases: A placement may fail in two ways. First, if part of the
	 * piece may falls out of bounds of the board, PLACE_OUT_BOUNDS is returned.
	 * Or the placement may collide with existing blocks in the grid in which
	 * case PLACE_BAD is returned. In both error cases, the board may be left in
	 * an invalid state. The client can use undo(), to recover the valid,
	 * pre-place state.
	 */
	public int place(Piece piece, int x, int y) {
	    if (!this.committed) {
		throw new RuntimeException("can only place object if the board has been commited");
	    }
	    if (x < 0 | x > this.width | y < 0 | y > this.height){
	    	return PLACE_OUT_BOUNDS; 
	    } else if (piece.getWidth()+x >= this.width | piece.getHeight()+y >= this.height){
	    	return PLACE_OUT_BOUNDS;
	    } else if (piece.getWidth()+x < 0 | piece.getHeight()+y < 0){
	    	return PLACE_OUT_BOUNDS;
	    } 
	    for (TPoint i : piece.getBody()){
	    	if (!this.getGrid(i.x,i.y)) {
	    		return PLACE_BAD;
	    	}
	    }
	    return PLACE_OK;
	}

	/**
	 * Deletes rows that are filled all the way across, moving things above
	 * down. Returns the number of rows cleared.
	 */
	public int clearRows() {
	    int rowsCleared = 0;
	    for (int y = 0; y < this.height; y++){
	    	if( this.width == this.getRowWidth(y)){
	    		rowsCleared++;
	    		for (int i = 0; i < this.width; i++){ //On enleve la ligne
	    			this.grid[i][y] = false;
	    		}
	    		for (int retour = y; retour < this.getMaxHeight()-1; retour++){ //on down les lignes
	    			for (int i = 0; i < this.width; i++){
	    				this.grid[i][retour] = this.grid[i][retour+1];
	    			}
	    		}
	    		for (int i = 0; i < this.width; i++){ //On enleve la ligne la plus haute
	    			this.grid[i][this.height-1] = false;
	    		}
	    	}
	    }
	    return rowsCleared;
	}

	/**
	 * Reverts the board to its state before up to one place and one
	 * clearRows(); If the conditions for undo() are not met, such as calling
	 * undo() twice in a row, then the second undo() does nothing. See the
	 * overview docs.
	 */
	public void undo() {
	    // YOUR CODE HERE
	}

	/**
	 * Puts the board in the committed state.
	 */
	public void commit() {
	    // YOUR CODE HERE
	    this.committed = true;
	}

	/*
	 * Renders the board state as a big String, suitable for printing. This is
	 * the sort of print-obj-state utility that can help see complex state
	 * change over time. (provided debugging utility)
	 */
	public String toString() {
		StringBuilder buff = new StringBuilder();
		for (int y = this.height - 1; y >= 0; y--) {
			buff.append('|');
			for (int x = 0; x < this.width; x++) {
				if (getGrid(x, y))
					buff.append('+');
				else
					buff.append(' ');
			}
			buff.append("|\n");
		}
		for (int x = 0; x < this.width + 2; x++)
			buff.append('-');
		return buff.toString();
	}
/**
	// Only for unit tests
	protected void updateWidthsHeights() {
		Arrays.fill(this.widths, 0);

		for (int i = 0; i < this.width; i++) {
			for (int j = 0; j < this.height; j++) {
				if (this.grid[i][j]) {
					this.widths[j] += 1;
					this.heights[i] = Math.max(j + 1, this.heights[i]);
				}
			}
		}
	}**/

}
