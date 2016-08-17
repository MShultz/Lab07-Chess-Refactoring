import java.util.ArrayList;
import java.util.Iterator;


public class Board {
	private final static int BOARD_SIZE = 8;
	private Piece[][] board;
	private boolean isCheckmate;
	private boolean isStalemate;
	private boolean winner;
	private boolean invalidCheckMove = false;
	private boolean shouldBeStaleMateDirective = false;
	LogWriter writer;
	DirectiveHandler handler;

	public Board(LogWriter writer) {
		this.writer = writer;
		board = new Piece[BOARD_SIZE][BOARD_SIZE];
		handler = new DirectiveHandler();
		isCheckmate = isStalemate = false;
	}

	public boolean isShouldBeStaleMateDirective() {
		return shouldBeStaleMateDirective;
	}

	public void setShouldBeStaleMateDirective(boolean shouldBeStaleMateDirective) {
		this.shouldBeStaleMateDirective = shouldBeStaleMateDirective;
	}

	public boolean isInvalidCheckMove() {
		return invalidCheckMove;
	}

	public void setInvalidCheckMove(boolean invalidCheckMove) {
		this.invalidCheckMove = invalidCheckMove;
	}

	public Piece[][] getBoard() {
		return board;
	}

	public boolean isCheckmate() {
		return isCheckmate;
	}

	public boolean isStalemate() {
		return isStalemate;
	}

	public boolean isWinner() {
		return winner;
	}

	public void setWinner(boolean winner) {
		this.winner = winner;
	}

	public void setCheckmate(boolean isCheckmate) {
		this.isCheckmate = isCheckmate;
	}

	public void setStalemate(boolean isStalemate) {
		this.isStalemate = isStalemate;
	}

	public void addNewPiece(String placement) {
		Position position = new Position(handler.getInitialRank(placement, false),
				handler.getInitialFile(placement, false));
		boolean isWhite = handler.isWhite(placement);
		char piece = handler.getPieceChar(placement);
		board[position.getRank()][position.getFile()] = handler.getPiece(piece, position, isWhite);
	}

	public boolean movePiece(String placement, boolean isWhite) {
		boolean sucessfulMove = false;
		char piece = handler.getPieceChar(placement);
		Position position1 = new Position(handler.getInitialRank(placement, true),
				handler.getInitialFile(placement, true));
		Position position2 = new Position(handler.getSecondaryRank(placement), handler.getSecondaryFile(placement));

		if (board[position1.getRank()][position1.getFile()] != null) {
			if (isValid(position1, position2, isWhite, placement, piece)) {
				Piece p = getPiece(position1);
				if (isValidPieceMovement(handler.isCapture(placement), p, position2)) {
					Position originalPosition = p.getCurrentPosition();
					Piece[][] checker = moveSinglePiece(position1, position2, copyArray(board), p);
					p.setCurrentPosition(position2);
					King teamKing = (King) getTeamKing(isWhite, board);
					if (isCheck(teamKing)) {
						boolean nowCheck = false;
						for (Piece opposing : getTeam(!isWhite, checker)) {
							if (isCheck(checker, opposing, (King) getTeamKing(isWhite, checker)))
								nowCheck = true;
						}
						if (nowCheck) {
							setInvalidCheckMove(true);
							setWinner(!isWhite);
						}
					}
					if (!isInvalidCheckMove()) {
						King opponentKing = (King) getTeamKing(!isWhite, checker);
						boolean validStalemate = isStalemate(isWhite, checker, opponentKing.isCheck());
						boolean opponentInCheck = isCheck(checker, p, opponentKing);
						if (!opponentInCheck && (!placement.contains("+"))
								|| (opponentInCheck && (placement.contains("+") || opponentInCheck
										&& placement.contains("#") && isCheckmate(!isWhite, checker, true)))) {
							if ((validStalemate && isShouldBeStaleMateDirective())
									|| (validStalemate && !isShouldBeStaleMateDirective())
									|| (!validStalemate && !isShouldBeStaleMateDirective())) {
								p.setHasMoved();
								board = moveSinglePiece(position1, position2, board, p);								
								sucessfulMove = true;
								if (opponentInCheck){
									opponentKing.setCheck(opponentInCheck);
								}else if(validStalemate){
									setStalemate(true);
								}
							}
						}
					} else {
						p.setCurrentPosition(originalPosition);
					}
				}
			}
		}
		return sucessfulMove;
	}
	public Piece[][] moveSinglePiece(Position pos1, Position pos2, Piece[][] updatedBoard, Piece p){
		updatedBoard[pos1.getRank()][pos1.getFile()] = null;
		updatedBoard[pos2.getRank()][pos2.getFile()] = p;
		return updatedBoard;
	}

	public void castle(boolean isWhite, String castle) {
		boolean isKingSide = handler.isKingSide(castle);
		Rook rook = getRook(isWhite, isKingSide);
		King king = getKing(isWhite);
		moveKingForCastle(king, isWhite, isKingSide);
		moveRookForCastle(rook, isWhite, isKingSide);

	}

	public boolean isValidCastle(String castle, boolean isWhite) {
		boolean valid = false;
		boolean isKingSide = handler.isKingSide(castle);
		Position rookPos = getRookPosition(isWhite, isKingSide);
		Position kingPos = getKingPosition(isWhite);
		Piece king = board[kingPos.getRank()][kingPos.getFile()];
		Piece rook = board[rookPos.getRank()][rookPos.getFile()];
		if ((rook != null && king != null) && (rook.getType() == PieceType.ROOK && king.getType() == PieceType.KING)) {
			if (!king.hasMoved() || !rook.hasMoved()) {
				if (!middleGroundOccupied(kingPos, rookPos, isKingSide)) {
					valid = true;
				}
			}
		}
		return valid;
	}

	public void writeBoard() {
		for (int i = BOARD_SIZE - 1; i >= 0; --i) {
			String boardString = "";
			for (int j = 0; j < BOARD_SIZE; ++j) {
				if (j == 0) {
					boardString += (i + 1) + "|" + getPieceStringForBoard(i, j);
				} else {
					boardString += "|" + getPieceStringForBoard(i, j);
				}
				if (j == 7) {
					boardString += "|";
					writer.writeToFile(boardString);
				}
			}
		}
		writer.writeToFile("  A B C D E F G H ");
	}

	public void printBoardToConsole() {
		for (int i = BOARD_SIZE - 1; i >= 0; --i) {
			String boardString = "";
			for (int j = 0; j < BOARD_SIZE; ++j) {
				if (j == 0) {
					boardString += (i + 1) + "|" + getPieceStringForBoard(i, j);
				} else {
					boardString += "|" + getPieceStringForBoard(i, j);
				}
				if (j == 7) {
					boardString += "|";
					System.out.println(boardString);
				}
			}
		}
		System.out.println("  A B C D E F G H ");
	}

	private String getPieceStringForBoard(int y, int x) {
		Piece p = board[y][x];
		return (p == null ? " " : (p.isWhite() ? "" + p.getType().getWhiteType() : "" + p.getType().getBlackType()));
	}

	public boolean isCapture(String directive) {
		return directive.contains("x");
	}

	public boolean isOccupied(Position position) {
		return board[position.getRank()][position.getFile()] != null;
	}

	public boolean isValid(Position position1, Position position2, boolean isWhiteTurn, String placement, char piece) {
		boolean valid = (isCorrectPiece(piece, position1, isWhiteTurn));
		if (isOccupied(position2) && valid) {
			valid = (isCapture(placement) && !isPlayerPiece(isWhiteTurn, position2));
		} else if (!isOccupied(position2) && !isCapture(placement) && valid) {
			valid = true;
		} else {
			valid = false;
		}
		return valid;
	}

	public boolean isPlayerPiece(boolean isWhiteTurn, Position position) {
		return ((board[position.getRank()][position.getFile()].isWhite() && isWhiteTurn)
				|| (!board[position.getRank()][position.getFile()].isWhite() && !isWhiteTurn));
	}

	public boolean isCorrectPiece(char piece, Position position, boolean isWhiteTurn) {
		char p = board[position.getRank()][position.getFile()].getType().getWhiteType();
		boolean isCorrect = ((board[position.getRank()][position.getFile()].isWhite() && isWhiteTurn)
				|| (!board[position.getRank()][position.getFile()].isWhite() && !isWhiteTurn));
		return (piece == p && isCorrect);
	}

	private Position getRookPosition(boolean isWhite, boolean isKingSide) {
		Position rookPos;
		if (isWhite) {
			rookPos = (isKingSide ? new Position(0, 7) : new Position(0, 0));
		} else {
			rookPos = (isKingSide ? new Position(7, 7) : new Position(7, 0));
		}
		return rookPos;
	}

	private Position getKingPosition(boolean isWhite) {
		return (isWhite ? new Position(0, 4) : new Position(7, 4));
	}

	private Rook getRook(boolean isWhite, boolean isKingSide) {
		Position rookPos;
		if (isWhite) {
			rookPos = (isKingSide ? new Position(0, 7) : new Position(0, 0));
		} else {
			rookPos = (isKingSide ? new Position(7, 7) : new Position(7, 0));
		}
		Rook rook = (Rook) board[rookPos.getRank()][rookPos.getFile()];
		board[rookPos.getRank()][rookPos.getFile()] = null;
		return rook;
	}

	private King getKing(boolean isWhite) {
		Position kingPos = (isWhite ? new Position(0, 4) : new Position(7, 4));
		King king = (King) board[kingPos.getRank()][kingPos.getFile()];
		board[kingPos.getRank()][kingPos.getFile()] = null;
		return king;
	}

	private boolean middleGroundOccupied(Position kingPos, Position rookPos, boolean isKingSide) {
		boolean occupied = false;
		if (isKingSide) {
			for (int i = kingPos.getFile() + 1; i < rookPos.getFile(); ++i) {
				if (board[kingPos.getRank()][i] != null) {
					occupied = true;
				}
			}
		} else {
			for (int i = kingPos.getFile() - 1; i > rookPos.getFile(); --i) {
				if (board[kingPos.getRank()][i] != null) {
					occupied = true;
				}
			}
		}
		return occupied;
	}

	private void moveKingForCastle(King king, boolean isWhite, boolean isKingSide) {
		Position newKing = (isKingSide ? king.getKingSide() : king.getQueenSide());
		king.setCurrentPosition(newKing);
		king.setHasMoved();
		board[newKing.getRank()][newKing.getFile()] = king;
	}

	private void moveRookForCastle(Rook rook, boolean isWhite, boolean isKingSide) {
		Position newRook = (isKingSide ? rook.getKingSide() : rook.getQueenSide());
		rook.setCurrentPosition(newRook);
		rook.setHasMoved();
		board[newRook.getRank()][newRook.getFile()] = rook;
	}

	public Piece getPiece(Position p) {
		return board[p.getRank()][p.getFile()];
	}

	private boolean isValidPieceMovement(boolean isCapture, Piece p, Position p2) {
		ArrayList<Position> possiblePositions = p.getMovement(board, isCapture);
		boolean found = false;
		for (Position pos : possiblePositions) {
			if (pos.equals(p2)) {
				found = true;
			}
		}
		return found;
	}

	// *****************************************************************************//

	public ArrayList<Piece> getAllPossiblePieces(boolean isWhite) {
		ArrayList<Piece> possiblePieces = new ArrayList<Piece>();
		for (Piece[] p : board) {
			for (Piece piece : p) {
				if (piece != null) {
					if (piece.isWhite() == isWhite) {
						ArrayList<Position> moves = piece.getMovement(board, true);
						moves = getNonCheckMovements(moves, piece, (King) getTeamKing(isWhite, board));
						if (moves.size() > 0) {
							possiblePieces.add(piece);
						}
					}
				}
			}
		}
		return possiblePieces;
	}

	public ArrayList<Piece> getAllPossiblePieces(boolean isWhite, Piece[][] board) {
		ArrayList<Piece> possiblePieces = new ArrayList<Piece>();
		for (Piece[] p : board) {
			for (Piece piece : p) {
				if (piece != null) {
					if (piece.isWhite() == isWhite) {
						ArrayList<Position> moves = piece.getMovement(board, true);
						moves = getNonCheckMovements(moves, piece, (King) getTeamKing(isWhite, board));
						if (moves.size() > 0) {
							possiblePieces.add(piece);
						}
					}
				}
			}
		}
		return possiblePieces;
	}

	public boolean isPlayable() {
		boolean playable = false;
		for (Piece[] rows : board) {
			for (Piece p : rows) {
				if (p != null)
					playable = true;
			}
		}
		return playable;
	}

	// *****************************************************************************//

	public boolean isCheck(Piece[][] board, Piece pieceMoved, King king) {
		ArrayList<Position> possibleMoves = pieceMoved.getMovement(board, true);
		boolean isCheck = false;

		for (Position pos : possibleMoves) {
			if (pos.equals(king.getCurrentPosition())) {
				isCheck = true;
				king.setCheck(isCheck);
			}
		}

		return isCheck;
	}

	public Piece getTeamKing(boolean isWhite, Piece[][] currentBoard) {
		Piece k = null;
		for (Piece[] pieces : currentBoard) {
			for (Piece p : pieces) {
				if (p != null) {
					if (p.isWhite() == isWhite && p.getType() == PieceType.KING) {
						k = p;
					}
				}
			}
		}
		return k;
	}

	public Piece[][] copyArray(Piece[][] board) {
		Piece[][] newCopy = new Piece[8][8];
		for (int i = 0; i < board.length; ++i) {
			System.arraycopy(board[i], 0, newCopy[i], 0, board[i].length);
		}
		return newCopy;
	}

	public ArrayList<Position> getNonCheckMovements(ArrayList<Position> allMoves, Piece p, King k) {
		Iterator<Position> allowableMoves = allMoves.iterator();
		Position prev = p.getCurrentPosition();
		while (allowableMoves.hasNext()) {
			Position pos = allowableMoves.next();
			Piece[][] checker = copyArray(board);
			checker[p.getCurrentPosition().getRank()][p.getCurrentPosition().getFile()] = null;
			checker[pos.getRank()][pos.getFile()] = p;
			if (p.getType() == PieceType.KING) {
				k.setCurrentPosition(pos);
			}
			ArrayList<Piece> opposingTeam = getTeam(!p.isWhite(), checker);
			boolean moveRemoved = false;
			for (Position teamPos : getAllMovements(opposingTeam, checker)) {
				if ((k.getCurrentPosition().equals(teamPos) && !k.isCheck())
						|| (k.isCheck() && (getNumChecks(k, getTeam(!k.isWhite(), checker), checker) != 0))
								&& !moveRemoved) {
					allowableMoves.remove();
					moveRemoved = true;
				}
			}
		}
		if (prev != p.getCurrentPosition())
			p.setCurrentPosition(prev);
		return allMoves;
	}

	public ArrayList<Position> getNonCheckMovements(ArrayList<Position> allMoves, Piece p, King k, Piece[][] checker) {
		Iterator<Position> allowableMoves = allMoves.iterator();
		Position prev = p.getCurrentPosition();
		while (allowableMoves.hasNext()) {
			Position pos = allowableMoves.next();
			checker[p.getCurrentPosition().getRank()][p.getCurrentPosition().getFile()] = null;
			checker[pos.getRank()][pos.getFile()] = p;
			if (p.getType() == PieceType.KING) {
				k.setCurrentPosition(pos);
			}
			ArrayList<Piece> opposingTeam = getTeam(!p.isWhite(), checker);
			boolean moveRemoved = false;
			for (Position teamPos : getAllMovements(opposingTeam, checker)) {
				if (((k.getCurrentPosition().equals(teamPos) && !k.isCheck())
						|| (k.isCheck() && (getNumChecks(k, getTeam(!k.isWhite(), checker), checker) != 0)))
								&& !moveRemoved) {
					allowableMoves.remove();
					moveRemoved = true;
				}
			}
		}
		if (prev != p.getCurrentPosition())
			p.setCurrentPosition(prev);
		return allMoves;
	}

	private ArrayList<Piece> getTeam(boolean isWhite, Piece[][] board) {
		ArrayList<Piece> team = new ArrayList<Piece>();
		for (Piece[] pieces : board) {
			for (Piece p : pieces) {
				if (p != null) {
					if (p.isWhite() == isWhite) {
						team.add(p);
					}
				}
			}
		}
		return team;
	}

	private ArrayList<Position> getAllMovements(ArrayList<Piece> team, Piece[][] board) {
		ArrayList<Position> movements = new ArrayList<>();
		for (Piece p : team) {
			movements.addAll(p.getMovement(board, true));
		}
		return movements;
	}

	public void setPostMoveChecks() {
		King whiteKing = (King) getTeamKing(true, board);
		King blackKing = (King) getTeamKing(false, board);

		whiteKing.setCheck(isCheck(whiteKing));
		int whiteKingNum = whiteKing.getNumChecks();
		whiteKing.setNumChecks(getNumChecks(whiteKing, getTeam(false, board), board));
		blackKing.setCheck(isCheck(blackKing));
		int blackKingNum = blackKing.getNumChecks();
		blackKing.setNumChecks(getNumChecks(blackKing, getTeam(true, board), board));
		
		if(whiteKingNum < whiteKing.getNumChecks())
			System.out.println("White King is in check!");
		if(blackKingNum < blackKing.getNumChecks())
			System.out.println("Black King is in check!");

	}

	public boolean isCheck(King king) {
		Iterator<Position> possibleMoves = getAllMovements(getTeam(!king.isWhite(), board), board).iterator();
		boolean isCheck = false;

		while (possibleMoves.hasNext() && !isCheck) {
			Position pos = possibleMoves.next();
			if (pos.equals(king.getCurrentPosition())) {
				isCheck = true;
			}
		}

		return isCheck;
	}

	public int getNumChecks(King k, ArrayList<Piece> team, Piece[][] board) {
		int count = 0;
		for (Piece p : team) {
			ArrayList<Position> moves = p.getMovement(board, true);
			for (Position pos : moves) {
				if (pos.equals(k.getCurrentPosition())) {
					++count;
				}
			}
		}
		return count;
	}

	public boolean isCheckmate(boolean isWhite, Piece[][] board, boolean setCheckmate) {
		King k = (King) getTeamKing(isWhite, board);
		ArrayList<Position> kingsMoves = k.getMovement(board, true);
		ArrayList<Piece> opposingTeam = getTeam(!isWhite, board);
		ArrayList<Position> opposingMoves = new ArrayList<Position>();
		for (Piece p : opposingTeam) {
			for (Position pos : kingsMoves) {
				Piece[][] newBoard = copyArray(board);
				newBoard[k.getCurrentPosition().getRank()][k.getCurrentPosition().getFile()] = null;
				newBoard[pos.getRank()][pos.getFile()] = k;
				opposingMoves.addAll(getNonCheckMovements(p.getMovement(newBoard, true), p,
						(King) getTeamKing(!isWhite, newBoard), newBoard));
			}
		}
		for (Position pos : opposingMoves) {
			if (kingsMoves.contains(pos)) {
				kingsMoves.remove(pos);
			}
		}
		setCheckmate(kingsMoves.size() == 0);
		if (isCheckmate && setCheckmate)
			setWinner(!k.isWhite());
		return kingsMoves.size() == 0;
	}

	private boolean isStalemate(boolean isWhite, Piece[][] board, boolean isCheck) {
		ArrayList<Piece> possiblePieces = getAllPossiblePieces(!isWhite, board);
		return possiblePieces.size() == 0 && !isCheck;
	}
}
