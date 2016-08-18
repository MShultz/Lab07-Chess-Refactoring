import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class InteractiveHandler {
	Board board;
	LogWriter writer;
	DirectiveHandler handler;
	UserInterface ui;
	OutputFormatter format;
	Processor process;

	public InteractiveHandler(Board board, LogWriter writer, DirectiveHandler handler, OutputFormatter format,
			Processor process, UserInterface ui) {
		this.board = board;
		this.writer = writer;
		this.handler = handler;
		this.format = format;
		this.process = process;
		this.ui = ui;
	}

	public void initiateInteractionMode(boolean beganWithNewBoard) {
		if (!board.isCheckmate() && !board.isInvalidCheckMove() && !board.isStalemate()) {
			writer.writeToFile("----------------------------------");
			writer.writeToFile("Process: Interactive Mode enabled.");
			writer.writeToFile("----------------------------------");
			interactionMode(beganWithNewBoard);
		}
	}

	private void interactionMode(boolean beganWithNewBoard) {
		boolean quit = false;
		if (beganWithNewBoard) {
			setUpBoard();
		}
		board.writeBoard();
		board.printBoardToConsole();
		int count = 1;
		boolean isWhite = true;
		while (!quit && board.isPlayable() && !board.isStalemate() && !board.isCheckmate()) {
			int piece;
			boolean pieceChosen;
			isWhite = (count % 2 != 0);
			ArrayList<Piece> pieces = board.getAllPossiblePieces(isWhite);
			King currentPlayerKing = (King) board.getTeamKing(isWhite, board.getBoard());
			if (pieces.size() == 0 && !currentPlayerKing.isCheck()) {
				board.setStalemate(true);
			} else if (pieces.size() == 0 && currentPlayerKing.isCheck()) {
				board.setCheckmate(true);
			}
			if (!board.isStalemate() && !board.isCheckmate()) {
				ui.inform(isWhite);
				do {
					pieceChosen = true;
					piece = ui.determinePiece(pieces);
					quit = isQuit(piece);
					if (!quit) {
						ArrayList<Position> possibleMoves = getAllMovesForPiece(pieces, piece, isWhite);
						board.printBoardToConsole();
						int move = ui.determineMove(possibleMoves);
						quit = isQuit(move);
						pieceChosen = !(move == 1);
						if (pieceChosen && !quit)
							getCompleteMovementAndProcess(pieces, piece, possibleMoves, move, isWhite);
					}
				} while (!pieceChosen);
			}
			++count;
			board.setPostMoveChecks();
		}
		board.printBoardToConsole();
	}

	private void getCompleteMovementAndProcess(ArrayList<Piece> pieces, int piece, ArrayList<Position> possibleMoves,
			int move, boolean isWhite) {
		String movement = getCompleteMovement(pieces.get(piece - 1), possibleMoves.get(move - 2));
		if (movement.contains("O")) {
			board.castle(isWhite, movement);
			writer.writeToFile(format.formatCastle(movement, isWhite));
		} else
			process.processMovement(movement, isWhite);
	}

	private void setUpBoard() {
		BufferedReader initializer;
		try {
			FileInputStream inputStream = new FileInputStream("src/BoardInitialization.chess");
			initializer = new BufferedReader(new InputStreamReader(inputStream));
			while (initializer.ready()) {
				process.processPlacement(initializer.readLine().trim());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private boolean isQuit(int choice) {
		return choice == 0;
	}

	private ArrayList<Position> getAllMovesForPiece(ArrayList<Piece> pieces, int piece, boolean isWhite) {
		Piece current = pieces.get(piece - 1);
		ArrayList<Position> possibleMoves = current.getMovement(board.getBoard(),
				(current.getType() == PieceType.PAWN ? false : true));
		possibleMoves = board.getNonCheckMovements(possibleMoves, current,
				(King) board.getTeamKing(current.isWhite(), board.getBoard()));
		if (current.getType() == PieceType.KING || current.getType() == PieceType.ROOK) {
			if (board.isValidCastle("O-O-O", isWhite)
					&& current.getCurrentPosition().equals(board.getRookPosition(isWhite, false)))
				possibleMoves.add(new Position(-1, -1));
			if (board.isValidCastle("O-O", isWhite)
					&& current.getCurrentPosition().equals(board.getRookPosition(isWhite, true)))
				possibleMoves.add(new Position(8, 8));
		}
		return possibleMoves;
	}

	private String getCompleteMovement(Piece piece, Position position) {
		String movement;
		if (position.file == -1)
			movement = "O-O-O";
		else if (position.file == 8)
			movement = "O-O";
		else {
			Piece[][] currentBoard = board.getBoard();
			Position piecePosition = piece.getCurrentPosition();
			movement = "" + (piece.getType() == PieceType.PAWN ? "" : piece.getType().getWhiteType())
					+ Character.toLowerCase(ui.getFileLetter(piecePosition.getFile())) + (piecePosition.getRank() + 1);
			movement += (currentBoard[position.getRank()][position.getFile()] == null ? "-" : "x");
			movement += Character.toLowerCase(ui.getFileLetter(position.getFile()));
			movement += (position.getRank() + 1);
			piece.setCurrentPosition(position);
			if (board.isCheck(board.moveSinglePiece(piecePosition, position, board.copyArray(board.getBoard()), piece),
					piece, (King) board.getTeamKing(!piece.isWhite(), currentBoard))) {
				Piece[][] checkBoard = board.moveSinglePiece(piece.getCurrentPosition(), position,
						board.copyArray(currentBoard), piece);
				if (board.isCheckmate(!piece.isWhite(), checkBoard, false)) {
					movement += "#";
				} else {
					movement += "+";
				}
			}
			piece.setCurrentPosition(piecePosition);
		}
		return movement;
	}


}
